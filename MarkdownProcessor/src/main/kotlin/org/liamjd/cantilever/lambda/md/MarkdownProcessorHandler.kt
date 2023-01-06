package org.liamjd.cantilever.lambda.md

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.liamjd.cantilever.models.sqs.MarkdownUploadMsg
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.ByteArrayOutputStream

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
        logger.info("RECORD.BODY=${eventRecord.body}")

        val markdownUploadMsg = Json.decodeFromString<MarkdownUploadMsg>(eventRecord.body)
        logger.info("metadata=${markdownUploadMsg.metadata}")

        val html = convertMDToHTML(log = logger, mdSource = markdownUploadMsg.markdownText)
        logger.info("HTML OUTPUT=$html")

        val s3Client = S3Client.builder()
            .region(Region.EU_WEST_2)
            .build()

        val outputStream = ByteArrayOutputStream()
        outputStream.bufferedWriter().write(html)

        try {
            val htmlKey = markdownUploadMsg.metadata.slug
            s3Client.putObject(
                PutObjectRequest.builder().contentLength(html.length.toLong()).contentType("text/html")
                    .bucket(destinationBucket).key(htmlKey).build(),
                RequestBody.fromBytes(html.toByteArray())
            )
            logger.info("Wrote HTML file '$htmlKey'")
        } catch (e: Exception) {
            logger.error("${e.message}")
        }

        return response
    }
}

/**
 * Wrappers for logging to make it slightly less annoying
 */
fun LambdaLogger.info(function: String, message: String) = log("INFO $function:  $message\n")
fun LambdaLogger.info(message: String) = info("MarkdownProcessorHandler",message)
fun LambdaLogger.warn(function: String, message: String) = log("WARN $function:  $message\n")
fun LambdaLogger.warn(message: String) = warn("MarkdownProcessorHandler",message)
fun LambdaLogger.error(function: String, message: String) = log("ERROR $function:  $message\n")
fun LambdaLogger.error(message: String) = error("MarkdownProcessorHandler",message)