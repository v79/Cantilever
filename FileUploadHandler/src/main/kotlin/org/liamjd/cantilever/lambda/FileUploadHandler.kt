package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.S3Event
import kotlinx.serialization.SerializationException
import org.liamjd.cantilever.common.*
import org.liamjd.cantilever.common.SOURCE_TYPE.*
import org.liamjd.cantilever.models.sqs.SqsMsgBody
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.SQSService
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import org.liamjd.cantilever.services.impl.SQSServiceImpl
import org.liamjd.cantilever.services.impl.extractPageModel
import org.liamjd.cantilever.services.impl.extractPostMetadata
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

        logger.info("${event.records.size} upload events received")

        try {
            val eventRecord = event.records[0]
            val srcKey = eventRecord.s3.`object`.urlDecodedKey
            val srcBucket = eventRecord.s3.bucket.name
            val folderName =
                srcKey.substringAfter('/').substringBefore('/') // the folder determines the type, POST, PAGE, STATICS
            val uploadFolder = SourceHelper.fromFolderName(folderName)
            val markdownQueueURL = System.getenv(QUEUE.MARKDOWN)
            val handlebarQueueURL = System.getenv(QUEUE.HANDLEBARS)

            logger.info("EventRecord: '${eventRecord.eventName}' SourceKey='$srcKey' from '$srcBucket'")

            try {
                val fileType = srcKey.substringAfterLast('.').lowercase()
                logger.info("FileUpload handler: source type is '$folderName'; file type is '$fileType'")

                when (uploadFolder) {
                    Posts -> {
                        if (fileType == FILE_TYPE.MD) {
                            processPostUpload(srcKey, srcBucket, markdownQueueURL, folderName)
                        } else {
                            logger.error("Posts must be written in Markdown format with the '.md' file extension")
                        }
                    }

                    Pages -> {
                        if (fileType == FILE_TYPE.MD) {
                            processPageUpload(srcKey, srcBucket, markdownQueueURL, folderName)
                        } else {
                            logger.error("Pages must be written in Markdown format with the '.md' file extension")
                        }
                    }

                    Templates -> {
                        logger.info("No action defined for TEMPLATE upload")
                    }

                    Statics -> {
                        logger.info("Analysing file type for static file upload")
                        when (fileType) {
                            FILE_TYPE.CSS -> {
                                processCSSUpload(srcKey, srcBucket, handlebarQueueURL, fileType)
                            }
                        }
                    }

                    else -> {
                        logger.info("No action defined for source type '$folderName'")
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
        queueUrl: String,
        sourceType: String
    ) {
        logger.info("Sending post $srcKey to markdown processor queue")
        try {
            val sourceString = s3Service.getObjectAsString(srcKey, srcBucket)
            // extract metadata
            val metadata = extractPostMetadata(filename = srcKey, source = sourceString)
            logger.info("Extracted metadata: $metadata")
            // extract body
            val markdownBody = sourceString.stripFrontMatter()

            val postModelMsg = SqsMsgBody.MarkdownPostUploadMsg(metadata, markdownBody)
            sendMessage(queueUrl, postModelMsg, sourceType, srcKey)
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
        queueUrl: String,
        sourceType: String
    ) {
        try {
            logger.info("Received page file $srcKey and sending it to Markdown processor queue")
            val sourceString = s3Service.getObjectAsString(srcKey, srcBucket)
            val pageSrcKey = srcKey.removePrefix("sources/$sourceType/") // just want the actual file name
            // extract page model
            val pageModelMsg = extractPageModel(pageSrcKey, sourceString)
            logger.info("Built page model for: ${pageModelMsg.srcKey}")

            sendMessage(queueUrl, pageModelMsg, sourceType, srcKey)
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
        srcBucket: String,
        queueUrl: String,
        sourceType: String
    ) {
        try {
            val destinationKey = "css/" + srcKey.removePrefix(S3_KEY.staticsPrefix)
            val cssMsg = SqsMsgBody.CssMsg(srcKey, destinationKey)
            logger.info("Sending message to Handlebars queue for $cssMsg")
            sendMessage(queueUrl, cssMsg, sourceType, srcKey)
        } catch (qdne: QueueDoesNotExistException) {
            logger.error("Queue '$queueUrl' does not exist; ${qdne.message}")
        }
    }

    /**
     * Send a message to the specified queue
     * @param queueUrl the SQS queue
     */
    private fun sendMessage(
        queueUrl: String,
        pageModelMsg: SqsMsgBody,
        sourceType: String,
        srcKey: String
    ) {
        val msgResponse = sqsService.sendMessage(
            toQueue = queueUrl,
            body = pageModelMsg,
            messageAttributes = createStringAttribute("sourceType", sourceType)
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
fun LambdaLogger.info(function: String, message: String) = log("INFO $function:  $message\n")
fun LambdaLogger.info(message: String) = info("FileUploadHandler", message)
fun LambdaLogger.warn(function: String, message: String) = log("WARN $function:  $message\n")
fun LambdaLogger.warn(message: String) = warn("FileUploadHandler", message)
fun LambdaLogger.error(function: String, message: String) = log("ERROR $function:  $message\n")
fun LambdaLogger.error(message: String) = error("FileUploadHandler", message)