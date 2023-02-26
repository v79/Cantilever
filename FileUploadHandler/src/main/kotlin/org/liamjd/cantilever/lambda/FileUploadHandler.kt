package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.S3Event
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.liamjd.cantilever.common.createStringAttribute
import org.liamjd.cantilever.models.sqs.MarkdownPostUploadMsg
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import org.liamjd.cantilever.services.impl.extractPageModel
import org.liamjd.cantilever.services.impl.extractPostMetadata
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

/**
 * Responds to a file upload event (PUT or PUSH).
 * It analyses the file and determines where to send it.
 * `source_type` is determined by from the S3 object key:
 * - /sources/<source_type>/<filename>
 * - Supports "posts" and "pages" for now.
 *
 * "Posts" must be markdown files (.md). The source type is added to the SQS message queue so the receiver knows how to process it.
 */
class FileUploadHandler : RequestHandler<S3Event, String> {

    private val s3Service: S3Service

    init {
        s3Service = S3ServiceImpl(Region.EU_WEST_2)
    }

    override fun handleRequest(event: S3Event, context: Context): String {
        val logger = context.logger
        var response = "200 OK"

        try {
            val eventRecord = event.records[0]
            val srcKey = eventRecord.s3.`object`.urlDecodedKey
            val srcBucket = eventRecord.s3.bucket.name
            val sourceType = srcKey.substringAfter('/').substringBefore('/')
            val queueUrl = System.getenv("markdown_processing_queue")

            logger.info("EventRecord: '${eventRecord.eventName}' SourceKey='$srcKey' from '$srcBucket'")
            logger.info("MarkdownQueue: $queueUrl")


            try {

                val fileType = srcKey.substringAfterLast('.').lowercase()
                logger.info("FileUpload handler: source type is '$sourceType'; file type is '$fileType'")

                val markdownQueue = SqsClient.builder().region(Region.EU_WEST_2).build()

                when (sourceType) {
                    POSTS -> {
                        if (fileType == "md") {
                            logger.info("Sending post $srcKey to markdown processor queue")
                            // send to markdown processing queue
                            try {
                                val sourceString = s3Service.getObjectAsString(srcKey, srcBucket)

                                // extract metadata
                                val metadata = extractPostMetadata(filename = srcKey, source = sourceString)
                                logger.info("Extracted metadata: $metadata")
                                // extract body
                                val markdownBody = sourceString.substringAfterLast("---")
                                val message = MarkdownPostUploadMsg(metadata, markdownBody)

                                val sqsMessageRequest = SendMessageRequest.builder()
                                    .queueUrl(queueUrl)
                                    .messageAttributes(
                                        createStringAttribute("sourceType", sourceType)
                                    )
                                    .messageBody(Json.encodeToString(message))
                                    .build()
                                logger.info("SQSMessage to send: $sqsMessageRequest")
                                val msgResponse = markdownQueue.sendMessage(
                                    sqsMessageRequest
                                )

                                logger.info("Message '$srcKey' sent, message ID is ${msgResponse.messageId()}'")
                            } catch (qdne: QueueDoesNotExistException) {
                                logger.error("queue '$queueUrl' does not exist; ${qdne.message}")
                            } catch (se: SerializationException) {
                                logger.error("Failed to parse metadata string; ${se.message}")
                            }
                        }
                    }

                    PAGES -> {
                        logger.info("Received page file $srcKey and sending it to Markdown processor queue")
                        val sourceString = s3Service.getObjectAsString(srcKey, srcBucket)
                        val pageSrcKey = srcKey.removePrefix("sources/$sourceType/") // just want the actual file name
                        // extract page model
                        val pageModel = extractPageModel(pageSrcKey,sourceString)
                        logger.info("Built page model: $pageModel")

                        val sqsMessageRequest = SendMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .messageAttributes(createStringAttribute("sourceType", sourceType))
                            .messageBody(Json.encodeToString(pageModel))
                            .build()
                        logger.info("SQSMessage to send: $sqsMessageRequest")
                        val msgResponse = markdownQueue.sendMessage(
                            sqsMessageRequest
                        )
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

    companion object {
        const val POSTS = "posts"
        const val PAGES = "pages"
        const val STATICS = "statics"
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