package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.liamjd.cantilever.common.FILES.INDEX_HTML
import org.liamjd.cantilever.common.FILES.INDEX_MD
import org.liamjd.cantilever.common.FILE_TYPE.HTML_HBS
import org.liamjd.cantilever.common.FILE_TYPE.MD
import org.liamjd.cantilever.common.S3_KEY.fragments
import org.liamjd.cantilever.common.S3_KEY.projectKey
import org.liamjd.cantilever.common.S3_KEY.templatesPrefix
import org.liamjd.cantilever.common.SOURCE_TYPE
import org.liamjd.cantilever.models.CantileverProject
import org.liamjd.cantilever.models.PageList
import org.liamjd.cantilever.models.PostList
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
        val responses = mutableListOf<String>()

        val sourceBucket = System.getenv("source_bucket")
        val destinationBucket = System.getenv("destination_bucket")
        val project: CantileverProject = getProjectModel(sourceBucket)

        logger.info("${event.records.size} records received for processing...")

        event.records.forEach { eventRecord ->

            try {
                logger.info("EventRecord: ${eventRecord.body}")

                when (eventRecord.messageAttributes["sourceType"]?.stringValue ?: "posts") {
                    SOURCE_TYPE.Posts.folder -> {
                        val message =
                            Json.decodeFromString<SqsMsgBody>(eventRecord.body) as SqsMsgBody.HTMLFragmentReadyMsg
                        logger.info("Processing message: $message")

                        val body = s3Service.getObjectAsString(message.fragmentKey, sourceBucket)
                        logger.info("Loaded body fragment from '${fragments + message.fragmentKey}: ${body.take(100)}'")

                        // load template file as specified by metadata
                        val template = templatesPrefix + message.metadata.template + HTML_HBS
                        logger.info("Attempting to load '$template' from bucket '${sourceBucket}' to a string")
                        val templateString = s3Service.getObjectAsString(template, sourceBucket)
                        logger.info("Got templateString: ${templateString.take(100)}")

                        // build model from project and from html fragment
                        val model = mutableMapOf<String, Any?>()
                        model["project"] = project
                        model["title"] = message.metadata.title
                        model["body"] = body

                        val html = with(logger) {
                            val renderer = HandlebarsRenderer()
                            renderer.render(model = model, template = templateString)
                        }
                        logger.info("Rendered HTML: ${html.take(100)}")

                        // save to S3
                        val outputFilename = calculateFilename(message)
                        s3Service.putObject(outputFilename, destinationBucket, html, "text/html")
                        logger.info("Written final HTML file to '${outputFilename}'")
                    }

                    SOURCE_TYPE.Pages.folder -> {

                        // TODO: THIS IS ALL A BIT BROKEN

                        val pagesFile = s3Service.getObjectAsString("generated/pages.json", sourceBucket)
                        val postsFile = s3Service.getObjectAsString("generated/posts.json", sourceBucket)
                        val pageList = Json.decodeFromString<PageList>(pagesFile)
                        val postList = Json.decodeFromString<PostList>(postsFile)
                        val message =
                            Json.decodeFromString<SqsMsgBody>(eventRecord.body) as SqsMsgBody.PageHandlebarsModelMsg
                        val pageTemplateKey = templatesPrefix + message.template + HTML_HBS
                        logger.info("Extracted page model: $message")

                        // load the page.html.hbs template
                        logger.info("Loading template $pageTemplateKey")
                        val templateString = s3Service.getObjectAsString(pageTemplateKey, sourceBucket)

                        val model = mutableMapOf<String, Any?>()
                        model["key"] = message.key
                        model["url"] = message.url
                        model["project"] = project
                        model.putAll(message.attributes)

                        message.sectionKeys.forEach { (name, objectKey) ->
                            val html = s3Service.getObjectAsString(objectKey, sourceBucket)
                            logger.info("Adding $name to model from $objectKey: ${html.take(50)}")
                            model[name] = html
                        }

                        model["pages"] = pageList.pages
                        model["posts"] = postList.posts

                        logger.info("Final page model keys: ${model.keys}")
                        val html = with(logger) {
                            val renderer = HandlebarsRenderer()
                            renderer.render(model = model, template = templateString)
                        }
//                        logger.info("Rendered HTML: ${html.take(100)}")

                        // TODO: this is a hack!
                        val outputFilename = calculateFilename(message)
                        s3Service.putObject(outputFilename, destinationBucket, html, "text/html")
                        logger.info("Written final HTML file to '$outputFilename'")
                    }
                }
            } catch (se: SerializationException) {
                logger.error("Failed to deserialize eventRecord $eventRecord, exception: ${se.message}")
                responses.add("500 Server Error")
            } catch (nske: NoSuchKeyException) {
                logger.error("Could not load file from S3, exception: ${nske.message}")
                responses.add("500 Server Error")
            }
        }

        return if (responses.size == 0) "200 OK" else "${responses.size} errors, final was ${responses.last()}"
    }

    /**
     * Calculate the final output file name
     * For posts:
     * - this is the metadata.slug object if it exists, or the source file name minus extensions if no slug exists
     * For pages:
     * - For the home page (i.e. for page index.md) this needs to be index.html
     * - For all other pages, this should be the source file name minus the extension
     */
    private fun calculateFilename(message: SqsMsgBody): String {
        println("TemplateProcessorHandler: Calculating final file name for $message")
        return when (message) {
            is SqsMsgBody.PageHandlebarsModelMsg ->
                if (message.key.endsWith(INDEX_MD)) INDEX_HTML else message.key.substringBefore(MD)
                    .substringAfterLast("pages/")

            is SqsMsgBody.HTMLFragmentReadyMsg -> message.metadata.slug
            is SqsMsgBody.MarkdownPostUploadMsg -> message.metadata.slug
            is SqsMsgBody.PageModelMsg -> if (message.srcKey == INDEX_MD) INDEX_HTML else message.srcKey.substringBefore(
                ".md"
            ).substringAfterLast("pages/")
        }
    }

    /**
     * Return the CantileverProject model
     */
    private fun getProjectModel(sourceBucket: String): CantileverProject {
        val projectYaml = s3Service.getObjectAsString(projectKey, sourceBucket)
        return Yaml.default.decodeFromString(CantileverProject.serializer(), projectYaml)
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