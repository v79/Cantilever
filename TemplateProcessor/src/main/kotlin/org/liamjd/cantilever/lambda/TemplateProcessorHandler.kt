package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.liamjd.cantilever.common.FILE_TYPE.HTML_HBS
import org.liamjd.cantilever.common.S3_KEY.fragments
import org.liamjd.cantilever.common.S3_KEY.templates
import org.liamjd.cantilever.common.SOURCE_TYPE
import org.liamjd.cantilever.models.Structure
import org.liamjd.cantilever.models.sqs.SqsMsgBody
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

/**
 * Processes a message to transform the given HTML fragment and a template into a complete HTML web page
 */
class TemplateProcessorHandler : RequestHandler<SQSEvent, String> {

    private val s3Service: S3Service

    init {
        s3Service = S3ServiceImpl(Region.EU_WEST_2)
    }

    override fun handleRequest(event: SQSEvent, context: Context): String {
        val logger = context.logger
        var response = "200 OK"

        val sourceBucket = System.getenv("source_bucket")
        val destinationBucket = System.getenv("destination_bucket")

        try {
            val eventRecord = event.records[0]
            logger.info("RAW message: ${eventRecord.body}")

            when(eventRecord.messageAttributes["sourceType"]?.stringValue ?: "posts") {
                SOURCE_TYPE.POSTS -> {
                    val message = Json.decodeFromString<SqsMsgBody>(eventRecord.body) as SqsMsgBody.HTMLFragmentReadyMsg
                    logger.info("Processing message: $message")

                    val body = s3Service.getObjectAsString(message.fragmentKey, sourceBucket)
                    logger.info("Loaded body fragment from '${fragments + message.fragmentKey}: ${body.take(100)}'")

                    // load template file as specified by metadata
                    val template = templates + message.metadata.template + HTML_HBS
                    logger.info("Attempting to load '$template' from bucket '${sourceBucket}' to a string")
                    val templateString = s3Service.getObjectAsString(template, sourceBucket)
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
                    s3Service.putObject(message.metadata.slug,destinationBucket,html,"text/html")
                    logger.info("Written final HTML file to '${message.metadata.slug}'")
                }
                SOURCE_TYPE.PAGES -> {
                    val structureFile = s3Service.getObjectAsString("generated/structure.json",sourceBucket)
                    val projectStructure = Json.decodeFromString<Structure>(structureFile)
                    val message = Json.decodeFromString<SqsMsgBody>(eventRecord.body) as SqsMsgBody.PageHandlebarsModelMsg
                    val pageTemplateKey = templates + message.template + HTML_HBS
                    logger.info("Extracted page model: $message")

                    // load the page.html.hbs template
                    logger.info("Loading template $pageTemplateKey")
                    val templateString = s3Service.getObjectAsString(pageTemplateKey, sourceBucket)

                    val model = mutableMapOf<String,Any?>()
                    model.putAll(message.attributes)

                    message.sectionKeys.forEach { (name, objectKey) ->
                        val html = s3Service.getObjectAsString(objectKey,sourceBucket)
                        logger.info("Adding $name to model from $objectKey: ${html.take(50)}")
                        model[name] = html
                    }

                    model["posts"] = projectStructure.posts

                    logger.info("Final page model keys: ${model.keys}")
                    val html = with(logger) {
                        val renderer = HandlebarsRenderer()
                        renderer.render(model = model , template = templateString)
                    }
                    logger.info("Rendered HTML: ${html.take(100)}")

                    val outputFilename = message.key.substringBefore('.')
                    s3Service.putObject(outputFilename,destinationBucket,html,"text/html")
                    logger.info("Written final HTML file to '$outputFilename'")
                }
            }


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