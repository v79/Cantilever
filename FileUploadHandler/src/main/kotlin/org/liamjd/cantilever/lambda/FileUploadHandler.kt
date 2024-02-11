package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.S3Event
import kotlinx.serialization.SerializationException
import org.liamjd.cantilever.common.*
import org.liamjd.cantilever.common.SOURCE_TYPE.*
import org.liamjd.cantilever.models.ContentMetaDataBuilder
import org.liamjd.cantilever.models.SrcKey
import org.liamjd.cantilever.models.sqs.ImageSQSMessage
import org.liamjd.cantilever.models.sqs.MarkdownSQSMessage
import org.liamjd.cantilever.models.sqs.TemplateSQSMessage
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.SQSService
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import org.liamjd.cantilever.services.impl.SQSServiceImpl
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException

/**
 * Responds to a file upload event (PUT or PUSH).
 * It analyses the file and determines where to send it.
 * `source_type` is determined by from the S3 object key:
 * - /sources/<source_type>/<filename>
 *
 * "Posts" and "Pages" must be markdown files (.md). The source type is added to the SQS message queue so the receiver knows how to process it.
 */
class FileUploadHandler : RequestHandler<S3Event, String> {

    private val s3Service: S3Service
    private val sqsService: SQSService
    private lateinit var logger: LambdaLogger

    init {
        s3Service = S3ServiceImpl(Region.EU_WEST_2)
        sqsService = SQSServiceImpl(Region.EU_WEST_2)
    }

    override fun handleRequest(event: S3Event, context: Context): String {
        logger = context.logger
        var response = "200 OK"
        val markdownQueueURL = System.getenv(QUEUE.MARKDOWN)
        val handlebarQueueURL = System.getenv(QUEUE.HANDLEBARS)
        val imageQueueURL = System.getenv(QUEUE.IMAGES)

        logger.info("${event.records.size} upload events received")

        try {
            val eventRecord = event.records[0] // TODO: No, this is wrong, we need to process all records
            val srcKey = eventRecord.s3.`object`.urlDecodedKey
            val srcBucket = eventRecord.s3.bucket.name
            val size = eventRecord.s3.`object`.sizeAsLong
            val folderName =
                srcKey.substringAfter('/').substringBefore('/') // the folder determines the type, POST, PAGE, STATICS
            val uploadFolder = SourceHelper.fromFolderName(folderName)

            logger.info("EventRecord: '${eventRecord.eventName}' SourceKey='$srcKey' from '$srcBucket'")

            try {
                val fileType = srcKey.substringAfterLast('.').lowercase()
                val contentType = s3Service.getContentType(srcKey, srcBucket)
                logger.info("FileUpload handler: source type is '$folderName'; file type is '$fileType'; content type is '$contentType'")

                if (size == 0L) {
                    logger.error("File $srcKey is empty, so not processing")
                    response = "400 Bad Request"
                } else {
                    when (uploadFolder) {
                        Root -> {
                            logger.info("No action defined for ROOT upload")
                        }

                        Posts -> {
                            // i'd like to check ContenType too, but it is not set correctly for .md files uploaded via IntelliJ
                            if (fileType == FILE_TYPE.MD) {
                                processPostUpload(srcKey, srcBucket, markdownQueueURL)
                            } else {
                                logger.error("Posts must be written in Markdown format with the '.md' file extension")
                            }
                        }

                        Pages -> {
                            if (fileType == FILE_TYPE.MD) {
                                processPageUpload(srcKey, srcBucket, markdownQueueURL)
                            } else {
                                logger.error("Pages must be written in Markdown format with the '.md' file extension")
                            }
                        }

                        Templates -> {
                            logger.warn("No action defined for TEMPLATE upload")
                        }

                        Statics -> {
                            logger.info("Analysing file type for static file upload")
                            when (fileType) {
                                FILE_TYPE.CSS -> {
                                    processCSSUpload(srcKey, handlebarQueueURL)
                                }
                            }
                        }

                        Images -> {
                            processImageUpload(srcKey, srcBucket, contentType, imageQueueURL)
                        }

                        else -> {
                            logger.info("No action defined for source type '$srcKey'")
                        }
                    }
                }

            } catch (nske: NoSuchKeyException) {
                logger.error("FileUpload EXCEPTION ${nske.message}")
                response = "500 Internal Server Error"
            }

        } finally {
            logger.info("Request completed")
        }

        return response
    }

    /**
     * Process the uploaded POST markdown file and send a message to the markdown processor queue
     */
    private fun processPostUpload(
        srcKey: String,
        srcBucket: String,
        queueUrl: String
    ) {
        logger.info("Sending post $srcKey to markdown processor queue")
        try {
            val sourceString = s3Service.getObjectAsString(srcKey, srcBucket)
            // extract metadata
            val metadata =
                ContentMetaDataBuilder.PostBuilder.buildFromSourceString(sourceString.getFrontMatter(), srcKey)
            logger.info("Extracted metadata: $metadata")
            // extract body
            val markdownBody = sourceString.stripFrontMatter()
            val postModelMsg = MarkdownSQSMessage.PostUploadMsg(metadata, markdownBody)
            sendMarkdownMessage(queueUrl, postModelMsg, srcKey)
        } catch (qdne: QueueDoesNotExistException) {
            logger.error("Queue '$queueUrl' does not exist; ${qdne.message}")
        } catch (se: SerializationException) {
            logger.error("Failed to parse metadata string; ${se.message}")
        }
    }

    /**
     * Process the uploaded PAGE markdown file and send a message to the markdown processor queue
     */
    private fun processPageUpload(
        srcKey: String,
        srcBucket: String,
        queueUrl: String
    ) {
        try {
            logger.info("Received page file $srcKey and sending it to Markdown processor queue")
            val sourceString = s3Service.getObjectAsString(srcKey, srcBucket)
            val pageSrcKey = srcKey.removePrefix(S3_KEY.pagesPrefix) // just want the actual file name
            // extract page model
            val metadata =
                ContentMetaDataBuilder.PageBuilder.buildFromSourceString(sourceString, srcKey)
            val markdownBody = sourceString.stripFrontMatter()
            val pageModelMsg = MarkdownSQSMessage.PageUploadMsg(metadata, markdownBody)
            logger.info("Built page model for: ${pageModelMsg.metadata.srcKey}")
            sendMarkdownMessage(queueUrl, pageModelMsg, srcKey)
        } catch (qdne: QueueDoesNotExistException) {
            logger.error("Queue '$queueUrl' does not exist; ${qdne.message}")
        } catch (se: SerializationException) {
            logger.error("Failed to parse metadata string; ${se.message}")
        }
    }

    /**
     * Process the uploaded CSS file and send a message to the handlebars template processor queue
     */
    private fun processCSSUpload(
        srcKey: String,
        queueUrl: String,
    ) {
        try {
            val destinationKey = "css/" + srcKey.removePrefix(S3_KEY.staticsPrefix)
            val cssMsg = TemplateSQSMessage.StaticRenderMsg(srcKey, destinationKey)
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
     * First check if it is supported file type.
     */
    private fun processImageUpload(srcKey: String, srcBucket: String, contentType: String?, queueUrl: String) {
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
            val imageMsg = ImageSQSMessage.ResizeImageMsg(metadata)
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
}

/**
 * Wrappers for logging to make it slightly less annoying
 */
fun LambdaLogger.info(function: String, message: String) = log("INFO: $function:  $message\n")
fun LambdaLogger.info(message: String) = info("FileUploadHandler", message)
fun LambdaLogger.warn(function: String, message: String) = log("WARN: $function:  $message\n")
fun LambdaLogger.warn(message: String) = warn("FileUploadHandler", message)
fun LambdaLogger.error(function: String, message: String) = log("ERROR: $function:  $message\n")
fun LambdaLogger.error(message: String) = error("FileUploadHandler", message)