package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.S3Event
import com.charleskorn.kaml.Yaml
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.SerializationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.liamjd.cantilever.common.*
import org.liamjd.cantilever.common.SOURCE_TYPE.*
import org.liamjd.cantilever.models.CantileverProject
import org.liamjd.cantilever.models.ContentMetaDataBuilder
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.SrcKey
import org.liamjd.cantilever.models.sqs.ImageSQSMessage
import org.liamjd.cantilever.models.sqs.MarkdownSQSMessage
import org.liamjd.cantilever.models.sqs.TemplateSQSMessage
import org.liamjd.cantilever.services.*
import org.liamjd.cantilever.services.impl.DynamoDBServiceImpl
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import org.liamjd.cantilever.services.impl.SQSServiceImpl
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException

/**
 * Set up koin dependency injection
 */
val fileUploadModule = module {
    single<S3Service> { S3ServiceImpl(Region.EU_WEST_2) }
    single<SQSService> { SQSServiceImpl(Region.EU_WEST_2) }
    single<DynamoDBService> {
        DynamoDBServiceImpl(
            region = Region.EU_WEST_2, enableLogging = true, dynamoDbClient = DynamoDbAsyncClient.create()
        )
    }
}

/**
 * This object is used to set up Koin dependency injection. It ensures that Koin is only started once, even in unit testing scenarios.
 */
object KoinSetup {
    fun setup(modules: List<Module>) {
        // Only start Koin if it's not already running
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                modules(fileUploadModule)
            }
        }
    }
}

/**
 * Responds to a file upload event (PUT or PUSH).
 * It analyses the file and determines where to send it.
 * `source_type` is determined by from the S3 object key:
 * - <domain>/sources/<source_type>/<filename>
 *
 * "Posts" and "Pages" must be markdown files (.md). The source type is added to the SQS message queue so the receiver knows how to process it.
 */
@Suppress("unused")
class FileUploadHandler(private val environmentProvider: EnvironmentProvider = SystemEnvironmentProvider()) :
    RequestHandler<S3Event, String>, KoinComponent, AWSLogger(enableLogging = true, msgSource = "FileUploadHandler") {

    init {
        KoinSetup.setup(listOf(fileUploadModule))
    }

    private val s3Service: S3Service by inject()
    private val sqsService: SQSService by inject()
    private val dynamoDBService: DynamoDBService by inject()

    override var logger: LambdaLogger? = null

    override fun handleRequest(event: S3Event, context: Context): String {
        logger = context.logger
        dynamoDBService.logger = logger
        var response = "200 OK"
        val markdownQueueURL = environmentProvider.getEnv(QUEUE.MARKDOWN)
        val handlebarQueueURL = environmentProvider.getEnv(QUEUE.HANDLEBARS)
        val imageQueueURL = environmentProvider.getEnv(QUEUE.IMAGES)

        log("${event.records.size} file events received")

        runBlocking {
            try {
                for (eventRecord in event.records) {
                    // srcKey will be in the format <www.domain.com>/sources/<source_type>/<filename>
                    val srcKey = eventRecord.s3.`object`.urlDecodedKey
                    val fileType = srcKey.substringAfterLast('.').lowercase()
                    val srcBucket = eventRecord.s3.bucket.name
                    val projectDomain = srcKey.substringBefore('/')
                    val size = eventRecord.s3.`object`.sizeAsLong
                    // the folder determines the type, POST, PAGE, STATICS, etc
                    val parentFolder = srcKey.removePrefix("$projectDomain/sources/").substringBefore('/')
                    log("EventRecord: '${eventRecord.eventName}' SourceKey='$srcKey' from '$srcBucket' folder '$parentFolder' file type '$fileType' size '$size'")

                    val sourceType = SourceHelper.fromFolderName(parentFolder)

                    // Check if the event is ObjectCreated (PUT or POST) or ObjectRemoved (DELETE)
                    // TODO: Better to switch/when on this
                    if (eventRecord.eventName == "ObjectRemoved:Delete" || eventRecord.eventName == "ObjectRemoved:DeleteMarkerCreated") {
                        // handle deletion events
                        // what are the relationships between nodes?
                        // maybe I just soft delete the node by adding a 'deleted' attribute?
                        // then a separate task can clean up the relationships and purge deleted items
                        // templates should have a count of their usage
                        // images should have a count of their usage
                        // posts know their previous and next post links
                        // pages know their parent folder
                        // folders know their index page, if any
                        // folders know their children

                        // TODO: Do some sanity checks
                        deleteContentNode(srcKey, projectDomain, sourceType)
                    }
                    if (eventRecord.eventName == "ObjectCreated:Put" || eventRecord.eventName == "ObjectCreated:Post") {
                        // Process the upload event
                        try {
                            val contentType = s3Service.getContentType(srcKey, srcBucket)
                            log("FileUpload handler: source type is '$parentFolder'; file type is '$fileType'; content type is '$contentType'; uploaded to folder '$sourceType'")

                            if (size == 0L) {
                                // folders are a special case, being zero length but ending in a slash
                                if (srcKey.endsWith('/')) {
                                    processFolderCreation(
                                        srcKey = srcKey,
                                        projectDomain = projectDomain,
                                    )
                                } else {
                                    log("ERROR", "File $srcKey is empty, so not processing")
                                    response = "400 Bad Request"
                                }
                            } else {
                                when (sourceType) {
                                    Unknown -> {
                                        log("ERROR", "Unknown source type '$parentFolder' for $srcKey")
                                    }

                                    Project -> {
                                        // Confirm that this is a project YAML file
                                        if (fileType != FILE_TYPE.YAML) {
                                            log(
                                                "ERROR",
                                                "Project files must be written in YAML format with the '.yaml' file extension"
                                            )
                                            response = "400 Bad Request"
                                        } else {
                                            if (!processProjectUpload(srcKey, srcBucket)) {
                                                log("ERROR", "Error while processing uploaded project file.")
                                                response = "500 Internal Server Error"
                                            }
                                        }
                                    }

                                    Posts -> {
                                        // I'd like to check ContentType too, but it is not set correctly for .md files uploaded via IntelliJ
                                        if (fileType == FILE_TYPE.MD) {
                                            if (!processPostUpload(
                                                    srcKey = srcKey,
                                                    srcBucket = srcBucket,
                                                    queueUrl = markdownQueueURL,
                                                    projectDomain = projectDomain
                                                )
                                            ) {
                                                log("ERROR", "Error while processing uploaded post file.")
                                                response = "500 Internal Server Error"
                                            }
                                        } else {
                                            log(
                                                "ERROR",
                                                "Posts must be written in Markdown format with the '.md' file extension"
                                            )
                                            response = "400 Bad Request"
                                        }
                                    }

                                    Pages -> {
                                        if (fileType == FILE_TYPE.MD) {
                                            processPageUpload(
                                                srcKey = srcKey,
                                                srcBucket = srcBucket,
                                                queueUrl = markdownQueueURL,
                                                projectDomain = projectDomain
                                            )
                                        } else {
                                            log(
                                                "ERROR",
                                                "Pages must be written in Markdown format with the '.md' file extension"
                                            )
                                            response = "400 Bad Request"
                                        }
                                    }

                                    // Templates and partials are uploaded to the same folder
                                    // The difference is that partials do not contain any YAML front matter
                                    Templates, Partials -> {
                                        if (fileType == FILE_TYPE.HBS) {
                                            processTemplateUpload(
                                                srcKey = srcKey, srcBucket = srcBucket, projectDomain = projectDomain
                                            )
                                        } else {
                                            log(
                                                "ERROR",
                                                "Templates must be written in Handlebars format with the '.hbs' file extension"
                                            )
                                            response = "400 Bad Request"
                                        }
                                    }

                                    Statics -> {
                                        // What other static files do we support?
                                        when (fileType) {
                                            FILE_TYPE.CSS -> {
                                                processCSSUpload(
                                                    srcKey = srcKey,
                                                    queueUrl = handlebarQueueURL,
                                                    projectDomain = projectDomain
                                                )
                                            }
                                        }
                                    }

                                    Images -> {
                                        // Should add some type checking here
                                        processImageUpload(
                                            srcKey = srcKey,
                                            projectDomain = projectDomain,
                                            contentType = contentType,
                                            queueUrl = imageQueueURL
                                        )
                                    }

                                    Folders -> {
                                        // Folders are a special case, and they will already have been captured by the
                                        // processFolderCreation method above, so we can ignore them here.
                                        log(
                                            "INFO", "Folder upload detected for $srcKey; no further processing required"
                                        )
                                    }

                                }
                            }

                        } catch (nske: NoSuchKeyException) {
                            log("ERROR", "FileUpload EXCEPTION: ${nske.message}")
                            response = "500 Internal Server Error"
                        }
                    }
                }
            } finally {
                log("Request completed")
            }
        }

        return response
    }

    /**
     * Process the uploaded Project YAML file. The database entry is identical to the YAML file (unlike all other nodes)
     */
    private suspend fun processProjectUpload(srcKey: String, srcBucket: String): Boolean {
        try {
            val sourceString = s3Service.getObjectAsString(srcKey, srcBucket)
            val project = Yaml.default.decodeFromString(CantileverProject.serializer(), sourceString)
            dynamoDBService.saveProject(project)
            return true
        } catch (se: SerializationException) {
            log("ERROR", "Failed to parse project file; ${se.message}")
        }
        return false
    }

    /**
     * Process the uploaded POST Markdown file and send a message to the Markdown processor queue
     */
    private suspend fun processPostUpload(
        srcKey: String, srcBucket: String, queueUrl: String, projectDomain: String
    ): Boolean {
        log("Sending post $srcKey to markdown processor queue")
        try {
            val sourceString = s3Service.getObjectAsString(srcKey, srcBucket)
            // extract metadata
            val metadata =
                ContentMetaDataBuilder.PostBuilder.buildFromSourceString(sourceString.getFrontMatter(), srcKey)
            log("Extracted post metadata: $metadata")
            // extract body
            val markdownBody = sourceString.stripFrontMatter()
            val postModelMsg = MarkdownSQSMessage.PostUploadMsg(
                projectDomain = projectDomain, metadata = metadata, markdownText = markdownBody
            )
            // upsert the content node in the DynamoDB table
            upsertContentNode(
                srcKey = srcKey, projectDomain = projectDomain, contentType = Posts, node = metadata
            )
            sendMarkdownMessage(queueUrl, postModelMsg, srcKey)
            return true
        } catch (qdne: QueueDoesNotExistException) {
            log("ERROR", "Queue '$queueUrl' does not exist; ${qdne.message}")
        } catch (se: SerializationException) {
            log("ERROR", "Failed to parse metadata string; ${se.message}")
        }
        return false
    }

    /**
     * Process the uploaded PAGE Markdown file and send a message to the Markdown processor queue
     */
    private suspend fun processPageUpload(
        srcKey: String, srcBucket: String, queueUrl: String, projectDomain: String
    ): Boolean {
        try {
            log("Received page file $srcKey and sending it to Markdown processor queue")
            val sourceString = s3Service.getObjectAsString(srcKey, srcBucket)
            // extract the page model
            val pageNode = ContentMetaDataBuilder.PageBuilder.buildFromSourceString(sourceString, srcKey)
            val markdownBody = sourceString.stripFrontMatter()
            val pageModelMsg = MarkdownSQSMessage.PageUploadMsg(
                projectDomain = projectDomain, metadata = pageNode, markdownText = markdownBody
            )
            log("Built page model for: ${pageModelMsg.metadata.srcKey}")
            upsertContentNode(
                srcKey = srcKey, projectDomain = projectDomain, contentType = Pages, node = pageNode
            )
            sendMarkdownMessage(queueUrl, pageModelMsg, srcKey)
            val pageKeyOnly = srcKey.removePrefix("$projectDomain/sources/pages/")

            if (pageKeyOnly.contains("/")) {
                log("Creating folder nodes")
                val parentFolder = pageKeyOnly.substringBeforeLast('/')
                val parentSrcKey = "$projectDomain/sources/pages/$parentFolder"
                val parentFolderNode = ContentNode.FolderNode(
                    srcKey = parentSrcKey, lastUpdated = Clock.System.now()
                )
                // check if the folder node already exists. If it does, get its children
                val existingFolderNode = dynamoDBService.getContentNode(parentSrcKey, projectDomain, Folders)
                if (existingFolderNode != null && existingFolderNode is ContentNode.FolderNode) {
                    log("Folder node already exists for $parentSrcKey; getting children")
                    parentFolderNode.children += existingFolderNode.children
                }
                if (!parentFolderNode.children.contains(pageNode.srcKey)) {
                    parentFolderNode.children += srcKey
                }

                upsertContentNode(
                    srcKey = parentSrcKey, projectDomain = projectDomain, contentType = Folders, parentFolderNode
                )
                return true
            } else {
                // In this case, the page has been uploaded to the root of the pages folder
                // We need to update the children of the root folder node
                val rootFolderNode = dynamoDBService.getContentNode(
                    srcKey = "$projectDomain/sources/pages/", projectDomain = projectDomain, contentType = Folders
                )
                if (rootFolderNode != null && rootFolderNode is ContentNode.FolderNode) {
                    log("Root folder node already exists; updating children")
                    if (!rootFolderNode.children.contains(pageNode.srcKey)) {
                        rootFolderNode.children += srcKey
                    }
                    upsertContentNode(
                        srcKey = "$projectDomain/sources/pages/",
                        projectDomain = projectDomain,
                        contentType = Folders,
                        rootFolderNode
                    )
                    log("Root folder node updated with new child page")
                    return true
                } else {
                    log("WARN: Root folder node does not exist; cannot update children")
                }
            }
        } catch (qdne: QueueDoesNotExistException) {
            log("ERROR", "Queue '$queueUrl' does not exist; ${qdne.message}")
        } catch (se: SerializationException) {
            log("ERROR", "Failed to parse metadata string; ${se.message}")
        }
        return false
    }

    /**
     * Process the uploaded CSS file and send a message to the Handlebars template processor queue
     */
    private suspend fun processCSSUpload(
        srcKey: String, queueUrl: String, projectDomain: String
    ): Boolean {
        try {
            val destinationKey = "css/" + srcKey.removePrefix(S3_KEY.staticsPrefix)
            val cssMsg = TemplateSQSMessage.StaticRenderMsg(
                projectDomain = projectDomain, srcKey = srcKey, destinationKey = destinationKey
            )
            log("Sending message to Handlebars queue for $cssMsg")
            val cssNode = ContentNode.StaticNode(
                srcKey = srcKey, lastUpdated = Clock.System.now()
            )
            upsertContentNode(srcKey = srcKey, projectDomain = projectDomain, contentType = Statics, node = cssNode)
            val msgResponse = sqsService.sendTemplateMessage(
                toQueue = queueUrl, body = cssMsg
            )
            if (msgResponse != null) {
                log("Message '$srcKey' sent, message ID is ${msgResponse.messageId()}'")
                return true
            } else {
                log("WARN", "No response received for message")
            }
        } catch (qdne: QueueDoesNotExistException) {
            log("ERROR", "Queue '$queueUrl' does not exist; ${qdne.message}")
        }
        return false
    }

    /**
     * Process the uploaded image file and send a message to the image processor queue.
     * First, check if it is a supported file type.
     */
    private suspend fun processImageUpload(
        srcKey: String, projectDomain: String, contentType: String?, queueUrl: String
    ): Boolean {
        try {
            // check if the image is a supported file type
            val validImageTypes = listOf(MimeType.jpg, MimeType.png, MimeType.gif, MimeType.webp)
            if (contentType == null) {
                log("ERROR", "No Content-Type metadata for $srcKey")
                throw Exception("No Content-Type metadata for $srcKey")
            }

            if (!validImageTypes.contains(MimeType.parse(contentType))) {
                log("ERROR", "Invalid image type '$contentType' for $srcKey")
                throw Exception("Invalid image type '$contentType' for $srcKey")
            }

            // OK, we know it's a valid image type, so send it to the image processor queue
            val imageNode = ContentMetaDataBuilder.ImageBuilder.buildFromSourceString("", srcKey)
            val imageMsg = ImageSQSMessage.ResizeImageMsg(projectDomain, imageNode)

            upsertContentNode(srcKey = srcKey, projectDomain = projectDomain, Images, imageNode)
            log("Sending message to Image processor queue for $imageMsg")
            val msgResponse = sqsService.sendImageMessage(
                toQueue = queueUrl, body = imageMsg
            )
            if (msgResponse != null) {
                log("Message '$srcKey' sent, message ID is ${msgResponse.messageId()}'")
                return true
            } else {
                log("WARN", "No response received for message")
            }

        } catch (e: Exception) {
            log("ERROR", "Failed to process image upload for $srcKey; ${e.message}")
        } catch (qdne: QueueDoesNotExistException) {
            log("ERROR", "Queue '$queueUrl' does not exist; ${qdne.message}")
        }
        return false
    }

    /**
     * Process the creation of a folder.
     * This is a special case where the source key ends with a slash (e.g. `www.example.com/sources/folders/my-folder/`).
     * There is no queue message sent for folder creation, but it is recorded in the DynamoDB table.
     * @param srcKey the source key of the folder
     * @param projectDomain the domain of the project to which the folder belongs
     */
    private suspend fun processFolderCreation(
        srcKey: String, projectDomain: String
    ) {
        // Create a ContentNode for the folder
        val folderNode = ContentNode.FolderNode(
            srcKey = srcKey,
            lastUpdated = Clock.System.now(),
        )
        // Upsert the content node in the DynamoDB table
        upsertContentNode(
            srcKey = srcKey, projectDomain = projectDomain, contentType = Folders, node = folderNode
        )
    }

    /**
     * Process the uploaded template file. In the future, this will send a message to the template processor queue.
     * Currently, it does nothing as the template processing is handled by the Markdown processor.
     * This function responds to both templates and partials.
     * @param srcKey the source key of the uploaded template file
     * @param srcBucket the S3 bucket where the template file is stored
     * @param projectDomain the domain of the project to which the template belongs
     */
    private suspend fun processTemplateUpload(
        srcKey: String, srcBucket: String, projectDomain: String
    ): Boolean {
        try {
            val sourceString = s3Service.getObjectAsString(srcKey, srcBucket)
            if (sourceString.hasFrontMatter()) {
                // extract metadata
                val templateNode = ContentMetaDataBuilder.TemplateBuilder.buildFromSourceString(sourceString, srcKey)
                val attributes: MutableMap<String, String> = mutableMapOf()
                attributes["title"] = templateNode.title
                // TODO: add the names of the sections as a set
                dynamoDBService.upsertContentNode(srcKey, projectDomain, Templates, templateNode, attributes)
                return true
            } else {
                // This is a partial, which has no front matter
                val partialNode = ContentNode.TemplatePartialNode(
                    srcKey = srcKey,
                    lastUpdated = Clock.System.now()
                )
                dynamoDBService.upsertContentNode(srcKey, projectDomain, Partials, partialNode, emptyMap())
                return true
            }
        } catch (e: Exception) {
            log("ERROR", "Failed to process template upload for $srcKey; ${e.message}")
        }
        return false
    }

    /**
     * Send a message to the specified queue, for conversion to HTML
     * @param queueUrl the SQS queue
     * @param markdownMsg the message to send
     * @param srcKey the source key of the file
     */
    private fun sendMarkdownMessage(
        queueUrl: String, markdownMsg: MarkdownSQSMessage, srcKey: SrcKey
    ) {
        val msgResponse = sqsService.sendMarkdownMessage(
            toQueue = queueUrl, body = markdownMsg
        )
        if (msgResponse != null) {
            log("Message '$srcKey' sent, message ID is ${msgResponse.messageId()}'")
        } else {
            log("WARN", "No response received for message")
        }
    }

    /**
     * Insert or update the content node in the DynamoDB table.
     * @param srcKey the source key of the content node
     * @param projectDomain the domain of the project
     * @param contentType the type of content (e.g. POST, PAGE, IMAGE, etc.)
     * @param node the content node to upsert
     */
    private suspend fun upsertContentNode(
        srcKey: SrcKey, projectDomain: String, contentType: SOURCE_TYPE, node: ContentNode
    ) {
        log("Upserting '$contentType' node for '$srcKey' in project '$projectDomain'")
        dynamoDBService.upsertContentNode(
            srcKey = srcKey,
            projectDomain = projectDomain,
            contentType = contentType,
            node = node,
            attributes = emptyMap()
        )
    }

    /**
     * Delete the content node from the DynamoDB table.
     * @param srcKey the source key of the content node
     * @param projectDomain the domain of the project
     * @param contentType the type of content (e.g. POST, PAGE, IMAGE, etc.)
     * @return the result of the DynamoDB delete operation
     */
    private suspend fun deleteContentNode(
        srcKey: SrcKey, projectDomain: String, contentType: SOURCE_TYPE
    ): DynamoDBResult {
        log("WARN", "Deleting '$contentType' node for '$srcKey' in project '$projectDomain'")
        val result = dynamoDBService.deleteContentNode(
            srcKey = srcKey, projectDomain = projectDomain, contentType = contentType
        )
        return result
    }

}
