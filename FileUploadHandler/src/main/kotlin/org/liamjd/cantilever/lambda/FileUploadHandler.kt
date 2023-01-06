package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.S3Event
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.liamjd.cantilever.models.sqs.MarkdownUploadMsg
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.sqs.SqsClient
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

            logger.info("RECORD=${eventRecord.eventName} SOURCEKEY=$srcKey")

            val s3Client = S3Client.builder()
                .region(Region.EU_WEST_2)
                .build()

            try {
                val destBucketName = System.getenv("destination_bucket") ?: srcBucket
                val queueUrl = System.getenv("markdown_processing_queue")
                val request = GetObjectRequest.builder()
                    .key(srcKey)
                    .bucket(srcBucket)
                    .build()

                val fileType = srcKey.substringAfterLast('.').lowercase()
                logger.info("FileUpload handler: file type is $fileType")
                when (fileType) {
                    "md" -> {
                        // send to markdown processing queue
                        val markdownQueue = SqsClient.builder().region(Region.EU_WEST_2).build()
                        try {
                            val sourceBytes: ByteArray = s3Client.getObjectAsBytes(request).asByteArray()
                            val sourceString = String(sourceBytes)
                            // extract metadata

                            with(logger) {
                                val metadata = extractPostMetadata(filename = srcKey, sourceString)

                                logger.info("Extracted metadata: $metadata")
                                // extract body
                                val markdownBody = sourceString.substringAfterLast("---")
//                            logger.info("Markdown body = $markdownBody")
                                val message = MarkdownUploadMsg(metadata, markdownBody)


                                val msgResponse = markdownQueue.sendMessage(
                                    SendMessageRequest.builder()
                                        .queueUrl(queueUrl)
                                        .messageBody(Json.encodeToString(message))
                                        .build()
                                )

                                logger.info("Message '$srcKey' sent, message ID is ${msgResponse.messageId()}'")
                            }
                        } catch (qdne: QueueDoesNotExistException) {
                            logger.error("queue '$queueUrl' does not exist; ${qdne.message}")
                        } catch (se: SerializationException) {
                            logger.error("Failed to parse metadata string; ${se.message}")
                        }
                    }

                    "jpg", "jpeg", "png", "gif", "webp" -> {
                        // send to image processing queue
                        logger.warn("FileUpload handler: Processing JPG image: NOT YET IMPLEMENTED")
                    }
                    // etc
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