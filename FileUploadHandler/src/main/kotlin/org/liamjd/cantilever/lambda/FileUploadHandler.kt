package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.S3Event
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import org.liamjd.cantilever.common.*
import org.liamjd.cantilever.common.SOURCE_TYPE.*
import org.liamjd.cantilever.models.ContentMetaDataBuilder
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.SrcKey
import org.liamjd.cantilever.models.sqs.ImageSQSMessage
import org.liamjd.cantilever.models.sqs.MarkdownSQSMessage
import org.liamjd.cantilever.models.sqs.TemplateSQSMessage
import org.liamjd.cantilever.services.DynamoDBService
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.SQSService
import org.liamjd.cantilever.services.impl.DynamoDBServiceImpl
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import org.liamjd.cantilever.services.impl.SQSServiceImpl
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException

/**
 * Responds to a file upload event (PUT or PUSH).
 * It analyses the file and determines where to send it.
 * `source_type` is determined by from the S3 object key:
 * - <domain>/sources/<source_type>/<filename>
 *
 * "Posts" and "Pages" must be markdown files (.md). The source type is added to the SQS message queue so the receiver knows how to process it.
 */
@Suppress("unused")
class FileUploadHandler : RequestHandler<S3Event, String> {

    private val s3Service: S3Service = S3ServiceImpl(Region.EU_WEST_2)
    private val sqsService: SQSService = SQSServiceImpl(Region.EU_WEST_2)
    private val dbClient = DynamoDbAsyncClient.create()
    private val dynamoDBService: DynamoDBService = DynamoDBServiceImpl(
        region = Region.EU_WEST_2, enableLogging = true, dynamoDbClient =
            dbClient
    )

    private lateinit var logger: LambdaLogger

    override fun handleRequest(event: S3Event, context: Context): String {
        logger = context.logger
        var response = "200 OK"
        val markdownQueueURL = System.getenv(QUEUE.MARKDOWN)
        val handlebarQueueURL = System.getenv(QUEUE.HANDLEBARS)
        val imageQueueURL = System.getenv(QUEUE.IMAGES)

        logger.info("${event.records.size} upload events received")

        try {
            for (eventRecord in event.records) {
                // srcKey will be in the format www.<domain>.com/sources/<source_type>/<filename>
                val srcKey = eventRecord.s3.`object`.urlDecodedKey
                val srcBucket = eventRecord.s3.bucket.name
                val projectDomain = srcKey.substringBefore('/')
                // the folder determines the type, POST, PAGE, STATICS
                val folderName = srcKey.removePrefix("$projectDomain/sources/").substringBefore('/')
                val size = eventRecord.s3.`object`.sizeAsLong
                val uploadFolder = SourceHelper.fromFolderName(folderName)

                logger.info("EventRecord: '${eventRecord.eventName}' SourceKey='$srcKey' from '$srcBucket'")

                try {
                    val fileType = srcKey.substringAfterLast('.').lowercase()
                    val contentType = s3Service.getContentType(srcKey, srcBucket)
                    logger.info("FileUpload handler: source type is '$folderName'; file type is '$fileType'; content type is '$contentType'; uploaded to folder '$uploadFolder'")

                    if (size == 0L) {
                        logger.error("File $srcKey is empty, so not processing")
                        response = "400 Bad Request"
                    } else {
                        when (uploadFolder) {
                            Root -> {
                                logger.info("No action defined for ROOT upload")
                            }

                            Posts -> {
                                // I'd like to check ContentType too, but it is not set correctly for .md files uploaded via IntelliJ
                                if (fileType == FILE_TYPE.MD) {
                                    processPostUpload(
                                        srcKey = srcKey,
                                        srcBucket = srcBucket,
                                        queueUrl = markdownQueueURL,
                                        projectDomain = projectDomain
                                    )
                                } else {
                                    logger.error("Posts must be written in Markdown format with the '.md' file extension")
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
                                    logger.error("Pages must be written in Markdown format with the '.md' file extension")
                                }
                            }

                            Templates -> {
                                if (fileType == FILE_TYPE.HBS) {
                                    processTemplateUpload(
                                        srcKey = srcKey,
                                        srcBucket = srcBucket,
                                        projectDomain = projectDomain
                                    )
                                } else {
                                    logger.error("Templates must be written in Handlebars format with the '.html.hbs' file extension")
                                }
                            }

                            Statics -> {
                                logger.info("Analysing file type for static file upload")
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
                                processImageUpload(
                                    srcKey = srcKey,
                                    projectDomain = projectDomain,
                                    contentType = contentType,
                                    queueUrl = imageQueueURL
                                )
                            }

                        }
                    }

                } catch (nske: NoSuchKeyException) {
                    logger.error("FileUpload EXCEPTION ${nske.message}")
                    response = "500 Internal Server Error"
                }
            }
        } finally {
            logger.info("Request completed")
        }

        return response
    }

    /**
     * Process the uploaded POST Markdown file and send a message to the Markdown processor queue
     */
    private fun processPostUpload(
        srcKey: String,
        srcBucket: String,
        queueUrl: String,
        projectDomain: String
    ) {
        logger.info("Sending post $srcKey to markdown processor queue")
        try {
            val sourceString = s3Service.getObjectAsString(srcKey, srcBucket)
            // extract metadata
            val metadata =
                ContentMetaDataBuilder.PostBuilder.buildFromSourceString(sourceString.getFrontMatter(), srcKey)
            logger.info("Extracted post metadata: $metadata")
            // extract body
            val markdownBody = sourceString.stripFrontMatter()
            val postModelMsg = MarkdownSQSMessage.PostUploadMsg(
                projectDomain = projectDomain,
                metadata = metadata,
                markdownText = markdownBody
            )
            runBlocking {
                // upsert the content node in the DynamoDB table
                upsertContentNode(
                    srcKey = srcKey,
                    projectDomain = projectDomain,
                    contentType = Posts,
                    node = metadata
                )
                sendMarkdownMessage(queueUrl, postModelMsg, srcKey)
            }
        } catch (qdne: QueueDoesNotExistException) {
            logger.error("Queue '$queueUrl' does not exist; ${qdne.message}")
        } catch (se: SerializationException) {
            logger.error("Failed to parse metadata string; ${se.message}")
        }
    }

    /**
     * Process the uploaded PAGE Markdown file and send a message to the Markdown processor queue
     */
    private fun processPageUpload(
        srcKey: String,
        srcBucket: String,
        queueUrl: String,
        projectDomain: String
    ) {
        try {
            logger.info("Received page file $srcKey and sending it to Markdown processor queue")
            val sourceString = s3Service.getObjectAsString(srcKey, srcBucket)
            // extract the page model
            val metadata =
                ContentMetaDataBuilder.PageBuilder.buildFromSourceString(sourceString, srcKey)
            val markdownBody = sourceString.stripFrontMatter()
            val pageModelMsg = MarkdownSQSMessage.PageUploadMsg(
                projectDomain = projectDomain,
                metadata = metadata,
                markdownText = markdownBody
            )
            logger.info("Built page model for: ${pageModelMsg.metadata.srcKey}")
            sendMarkdownMessage(queueUrl, pageModelMsg, srcKey)
        } catch (qdne: QueueDoesNotExistException) {
            logger.error("Queue '$queueUrl' does not exist; ${qdne.message}")
        } catch (se: SerializationException) {
            logger.error("Failed to parse metadata string; ${se.message}")
        }
    }

    /**
     * Process the uploaded CSS file and send a message to the Handlebars template processor queue
     */
    private fun processCSSUpload(
        srcKey: String,
        queueUrl: String,
        projectDomain: String
    ) {
        try {
            val destinationKey = "css/" + srcKey.removePrefix(S3_KEY.staticsPrefix)
            val cssMsg = TemplateSQSMessage.StaticRenderMsg(
                projectDomain = projectDomain,
                srcKey = srcKey,
                destinationKey = destinationKey
            )
            logger.info("Sending message to Handlebars queue for $cssMsg")
            val msgResponse = sqsService.sendTemplateMessage(
                toQueue = queueUrl,
                body = cssMsg
            )
            if (msgResponse != null) {
                logger.info("Message '$srcKey' sent, message ID is ${msgResponse.messageId()}'")
            } else {
                logger.warn("No response received for message")
            }
        } catch (qdne: QueueDoesNotExistException) {
            logger.error("Queue '$queueUrl' does not exist; ${qdne.message}")
        }
    }

    /**
     * Process the uploaded image file and send a message to the image processor queue.
     * First, check if it is a supported file type.
     */
    private fun processImageUpload(srcKey: String, projectDomain: String, contentType: String?, queueUrl: String) {
        try {
            // check if the image is a supported file type
            val validImageTypes = listOf(MimeType.jpg, MimeType.png, MimeType.gif, MimeType.webp)
            if (contentType == null) {
                logger.error("No Content-Type metadata for $srcKey")
                throw Exception("No Content-Type metadata for $srcKey")
            }

            if (!validImageTypes.contains(MimeType.parse(contentType))) {
                logger.error("Invalid image type '$contentType' for $srcKey")
                throw Exception("Invalid image type '$contentType' for $srcKey")
            }

            // OK, we know it's a valid image type, so send it to the image processor queue
            val metadata = ContentMetaDataBuilder.ImageBuilder.buildFromSourceString("", srcKey)
            val imageMsg = ImageSQSMessage.ResizeImageMsg(projectDomain, metadata)
            logger.info("Sending message to Image processor queue for $imageMsg")
            val msgResponse = sqsService.sendImageMessage(
                toQueue = queueUrl,
                body = imageMsg
            )
            if (msgResponse != null) {
                logger.info("Message '$srcKey' sent, message ID is ${msgResponse.messageId()}'")
            } else {
                logger.warn("No response received for message")
            }

        } catch (e: Exception) {
            logger.error("Failed to process image upload for $srcKey; ${e.message}")
        } catch (qdne: QueueDoesNotExistException) {
            logger.error("Queue '$queueUrl' does not exist; ${qdne.message}")
        }
    }

    /**
     * Process the uploaded template file. In the future, this will send a message to the template processor queue.
     * Currently, it does nothing as the template processing is handled by the Markdown processor.
     * @param srcKey the source key of the uploaded template file
     * @param srcBucket the S3 bucket where the template file is stored
     * @param projectDomain the domain of the project to which the template belongs
     */
    private fun processTemplateUpload(
        srcKey: String,
        srcBucket: String,
        projectDomain: String
    ) {
        try {
            val sourceString = s3Service.getObjectAsString(srcKey, srcBucket)
            // extract metadata
            val metadata = ContentMetaDataBuilder.TemplateBuilder.buildFromSourceString(sourceString, srcKey)
            logger.info("Extracted metadata: $metadata")
            val attributes: MutableMap<String, String> = mutableMapOf()
            attributes.put("title", metadata.title)
            // TODO: add the names of the sections as a set
            runBlocking {
                dynamoDBService.upsertContentNode(srcKey, projectDomain, Templates, metadata)
            }
        } catch (e: Exception) {
            logger.error("Failed to process template upload for $srcKey; ${e.message}")
        }
    }

    /**
     * Send a message to the specified queue, for conversion to HTML
     * @param queueUrl the SQS queue
     * @param markdownMsg the message to send
     * @param srcKey the source key of the file
     */
    private fun sendMarkdownMessage(
        queueUrl: String,
        markdownMsg: MarkdownSQSMessage,
        srcKey: SrcKey
    ) {
        val msgResponse = sqsService.sendMarkdownMessage(
            toQueue = queueUrl,
            body = markdownMsg
        )
        if (msgResponse != null) {
            logger.info("Message '$srcKey' sent, message ID is ${msgResponse.messageId()}'")
        } else {
            logger.warn("No response received for message")
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
        srcKey: SrcKey,
        projectDomain: String,
        contentType: SOURCE_TYPE,
        node: ContentNode
    ) {
        logger.info("Upserting content node for $srcKey in project $projectDomain")
        dynamoDBService.upsertContentNode(
            srcKey = srcKey,
            projectDomain = projectDomain,
            contentType = contentType,
            node = node
        )
    }
}

/**
 * Wrappers for logging to make it slightly less annoying
 */
fun LambdaLogger.info(function: String, message: String) = log("INFO: $function: $message\n")
fun LambdaLogger.info(message: String) = info("FileUploadHandler", message)
fun LambdaLogger.warn(function: String, message: String) = log("WARN: $function: $message\n")
fun LambdaLogger.warn(message: String) = warn("FileUploadHandler", message)
fun LambdaLogger.error(function: String, message: String) = log("ERROR: $function: $message\n")
fun LambdaLogger.error(message: String) = error("FileUploadHandler", message)