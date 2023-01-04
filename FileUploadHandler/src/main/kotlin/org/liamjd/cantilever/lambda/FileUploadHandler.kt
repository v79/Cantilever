package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.S3Event
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import java.nio.charset.Charset

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
            logger.log("FileUpload handler RECORD=${eventRecord.eventName} SOURCEKEY=$srcKey")

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

                val sourceBytes: ByteArray = s3Client.getObjectAsBytes(request).asByteArray()
                logger.log("FileUpload handler: source bytes: ${sourceBytes.toString(Charset.defaultCharset())}")

                val fileType = srcKey.substringAfterLast('.').lowercase()
                logger.log("FileUpload handler: file type is $fileType")
                when (fileType) {
                    "md" -> {
                        // send to markdown processing queue
                        val markdownQueue = SqsClient.builder().region(Region.EU_WEST_2).build()
                        try {
                            val msgResponse = markdownQueue.sendMessage(
                                SendMessageRequest.builder()
                                    .queueUrl(queueUrl)
                                    .messageBody(srcKey)
                                    .build()
                            )
                            logger.log("FileUpload handler: Message '$srcKey' sent, message ID is ${msgResponse.messageId()}'")
                        } catch (qdne: QueueDoesNotExistException) {
                            logger.log("FileUpload handler EXCEPTION: queue '$queueUrl' does not exist; ${qdne.message}")
                        }
                    }

                    "jpg", "jpeg", "png", "gif", "webp" -> {
                        // send to image processing queue
                        logger.log("FileUpload handler: Processing JPG image: NOT YET IMPLEMENTED")
                    }
                    // etc
                }

            } catch (nske: NoSuchKeyException) {
                logger.log("FileUpload EXCEPTION ${nske.message}")
                response = "500 Internal Server Error"
            }

        } finally {
            logger.log("FileUploadHandler completed")
        }

        return response
    }

}

