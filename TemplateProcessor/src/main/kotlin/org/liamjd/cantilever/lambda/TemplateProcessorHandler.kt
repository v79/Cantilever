package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.liamjd.cantilever.common.fileTypes.HTML_HBS
import org.liamjd.cantilever.common.s3Keys.fragmentsKey
import org.liamjd.cantilever.models.sqs.HTMLFragmentReadyMsg
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest

/**
 * Processes a message to transform the given HTML fragment and a template into a complete HTML web page
 */
class TemplateProcessorHandler : RequestHandler<SQSEvent, String> {

    override fun handleRequest(event: SQSEvent, context: Context): String {
        val logger = context.logger
        var response = "200 OK"

        val sourceBucket = System.getenv("source_bucket")
        val destinationBucket = System.getenv("destination_bucket")

        try {
            val eventRecord = event.records[0]
            val s3Client = S3Client.builder().region(Region.EU_WEST_2).build()
            logger.info("RAW message: ${eventRecord.body}")
            val message: HTMLFragmentReadyMsg = Json.decodeFromString(eventRecord.body)
            logger.info("Processing message: $message")

            val fragmentRequest = GetObjectRequest.builder()
                .key(message.fragmentKey)
                .bucket(sourceBucket)
                .build()

            val body = String(s3Client.getObjectAsBytes(fragmentRequest).asByteArray())
            logger.info("Loaded body fragment from '${fragmentsKey + message.fragmentKey}: ${body.take(100)}'")


            // load template file as specified by metadata
            val template = message.metadata.template + HTML_HBS
            logger.info("Attempting to load '$template' from bucket '${sourceBucket}' to a string")
            val s3TemplateRequest = GetObjectRequest.builder()
                .key(template)
                .bucket(sourceBucket)
                .build()
            logger.info("Request is: $s3TemplateRequest: ${s3TemplateRequest.bucket()} ${s3TemplateRequest.key()}")
//            val templObj = s3Client.getObject(s3TemplateRequest).response()
            val templateString = String(s3Client.getObjectAsBytes(s3TemplateRequest).asByteArray())
            logger.info("Got templateString: ${templateString.take(100)}")


            // build model from project and from html fragment
            val model = mutableMapOf<String, Any?>()
            model["title"] = message.metadata.title
            model["body"] = body

            val html = with(logger) {
                val renderer = HandlebarsRenderer()
                renderer.render(model = model, template = templateString)
            }
            logger.info("Rendered HTML: ${html.take(100)}")

            // save to S3
            s3Client.putObject(
                PutObjectRequest.builder().contentLength(html.length.toLong()).contentType("text/html")
                    .bucket(destinationBucket).key(message.metadata.slug).build(),
                RequestBody.fromBytes(html.toByteArray())
            )
            logger.info("Written final HTML file to '${message.metadata.slug}'")
        } catch (se: SerializationException) {
            logger.error("Failed to deserialize eventRecord $event, exception: ${se.message}")
            response = "500 Server Error"
        } catch (nske: NoSuchKeyException) {
            logger.error("Could not load file from S3, exception: ${nske.message}")
            response = "500 Server Error"
        } finally {
            logger.info("Template processing completed")
        }

        return response
    }
}

/**
 * Wrappers for logging to make it slightly less annoying
 */
fun LambdaLogger.info(function: String, message: String) = log("INFO $function:  $message\n")
fun LambdaLogger.info(message: String) = info("TemplateProcessorHandler", message)
fun LambdaLogger.warn(function: String, message: String) = log("WARN $function:  $message\n")
fun LambdaLogger.warn(message: String) = warn("TemplateProcessorHandler", message)
fun LambdaLogger.error(function: String, message: String) = log("ERROR $function:  $message\n")
fun LambdaLogger.error(message: String) = error("TemplateProcessorHandler", message)