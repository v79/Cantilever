package org.liamjd.cantilever.lambda.md

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.liamjd.cantilever.models.sqs.MarkdownUploadMsg
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.Exception

/**
 * Respond to the SQSEvent which will contain the S3 key of the markdown file to parse
 * Read the content of the file and pass it to the `convertMDToHTML` function
 * Then write the resultant file to the destination bucket
 */
class MarkdownProcessorHandler : RequestHandler<SQSEvent, String> {

    override fun handleRequest(event: SQSEvent, context: Context): String {
        val sourceBucket = System.getenv("source_bucket")
        val destinationBucket = System.getenv("destination_bucket")
        val logger = context.logger
        var response = "200 OK"

        val eventRecord = event.records[0]
        logger.log("MarkdownProcessorHandler RECORD.BODY=${eventRecord.body}")

        val markdownUploadMsg = Json.decodeFromString<MarkdownUploadMsg>(eventRecord.body)
        logger.log("MarkdownProcessorHandler metadata=${markdownUploadMsg.metadata}")

        val html = convertMDToHTML(log = logger, mdSource = markdownUploadMsg.markdownText)
        logger.log("MarkdownProcessorHandler HTML OUTPUT=$html")

        val s3Client = S3Client.builder()
            .region(Region.EU_WEST_2)
            .build()


        val outputStream = ByteArrayOutputStream()
        outputStream.bufferedWriter().write(html)

//        val metadata = ObjectMetadata()
//        metadata.contentLength = outputStream.size().toLong()
//        metadata.contentType = "text/html"
//
//        val putObjectRequest = PutObjectRequest(
//            destinationBucket,
//            srcKey.removeSuffix(".md"),
//            ByteArrayInputStream(outputStream.toByteArray()),
//            metadata
//        )
//        logger.log("Writing transformed file '${putObjectRequest.key}' to $destBucketName")
//        s3Client.putObject(putObjectRequest)


        try {
            s3Client.putObject(
                PutObjectRequest.builder().contentLength(html.length.toLong()).contentType("text/html")
                    .bucket(destinationBucket).key(markdownUploadMsg.metadata.title.replace(' ', '-')).build(),
                RequestBody.fromBytes(html.toByteArray())
            )
        } catch (e: Exception) {
            logger.log("MarkdownProcessorHandler EXCEPTION ${e.message}")
        }

        return response
    }
}