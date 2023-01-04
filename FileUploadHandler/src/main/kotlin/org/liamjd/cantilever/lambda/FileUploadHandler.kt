package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.S3Event
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
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
                val request = GetObjectRequest.builder()
                    .key(srcKey)
                    .bucket(srcBucket)
                    .build()

                val sourceBytes: ByteArray = s3Client.getObjectAsBytes(request).asByteArray()
                logger.log("FileUpload handler: source bytes: ${sourceBytes.toString(Charset.defaultCharset())}")

                val outputString = """
                <html>
                    <head></head>
                    <body>
                        ${sourceBytes.toString(Charset.defaultCharset())}
                    </body>
                </html>
            """.trimIndent()


                logger.log("FileUpload handler: output string: $outputString}")

                val customMetadata = mutableMapOf<String, String>()
                customMetadata["source-file"] = srcKey

                val outputBytes = outputString.toByteArray(Charset.defaultCharset())

                val putObjectRequest = PutObjectRequest.builder()
                    .bucket(destBucketName)
                    .key(srcKey.removeSuffix(".md"))
                    .metadata(customMetadata)
                    .contentType("text/html")
                    .contentLength(outputBytes.size.toLong())
                    .build()

                logger.log("Writing transformed file '${putObjectRequest.key()}' to $destBucketName")
                s3Client.putObject(putObjectRequest, RequestBody.fromBytes(outputBytes))

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

