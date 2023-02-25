package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.S3Event
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.liamjd.cantilever.common.toLocalDateTime
import org.liamjd.cantilever.models.sqs.MarkdownPostUploadMsg
import org.liamjd.cantilever.services.impl.extractPostMetadata
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

/**
 * Responds to a file upload event (PUT or PUSH)
 * In this test implementation, it merely wraps the source text in an HTML file
 * and writes it to the destination bucket, as specified in an environment variable
 */
class FileUploadHandler : RequestHandler<S3Event, String> {

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

            val s3Client = S3Client.builder()
                .region(Region.EU_WEST_2)
                .build()

            try {
                val request = GetObjectRequest.builder()
                    .key(srcKey)
                    .bucket(srcBucket)
                    .build()

                val fileType = srcKey.substringAfterLast('.').lowercase()
                logger.info("FileUpload handler: source type is '$sourceType'; file type is '$fileType'")

                when(sourceType) {
                    POSTS -> {
                        if(fileType == "md") {
                            logger.info("Sending post $srcKey to markdown processor queue")
                            // send to markdown processing queue
                            val markdownQueue = SqsClient.builder().region(Region.EU_WEST_2).build()
                            try {
                                val mdObject = s3Client.getObject(request).response()
                                val srcLastModified = mdObject.lastModified().toLocalDateTime()
                                val sourceString = String(s3Client.getObjectAsBytes(request).asByteArray())

                                // extract metadata
                                val metadata = extractPostMetadata(filename = srcKey, source = sourceString)
                                logger.info("Extracted metadata: $metadata")
                                // extract body
                                val markdownBody = sourceString.substringAfterLast("---")
                                val message = MarkdownPostUploadMsg(metadata, markdownBody)

                                val sqsMessageRequest = SendMessageRequest.builder()
                                    .queueUrl(queueUrl)
                                    .messageAttributes(mapOf("sourceType" to MessageAttributeValue.builder().dataType("String").stringValue(sourceType).build()))
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
                        logger.info("Received page file $srcKey but not ready to process it")
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