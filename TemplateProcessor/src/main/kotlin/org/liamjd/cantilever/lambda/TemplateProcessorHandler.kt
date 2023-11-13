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
import org.liamjd.cantilever.common.FILE_TYPE.MD
import org.liamjd.cantilever.common.S3_KEY.fragments
import org.liamjd.cantilever.common.S3_KEY.projectKey
import org.liamjd.cantilever.common.stripFrontMatter
import org.liamjd.cantilever.models.CantileverProject
import org.liamjd.cantilever.models.ContentTree
import org.liamjd.cantilever.models.PageTree
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
    private lateinit var logger: LambdaLogger
    private lateinit var navigationBuilder: NavigationBuilder

    init {
        s3Service = S3ServiceImpl(Region.EU_WEST_2)
    }

    override fun handleRequest(event: SQSEvent, context: Context): String {
        logger = context.logger
        val responses = mutableListOf<String>()

        val sourceBucket = System.getenv("source_bucket")
        val destinationBucket = System.getenv("destination_bucket")
        val project: CantileverProject = getProjectModel(sourceBucket)

        with(logger) {
            navigationBuilder = NavigationBuilder(s3Service, sourceBucket)
        }
        logger.info("${event.records.size} records received for processing...")

        event.records.forEach { eventRecord ->
            try {
                logger.info("EventRecord: ${eventRecord.body}")
                val sqsMsgBody = Json.decodeFromString<SqsMsgBody>(eventRecord.body)

                logger.info("Decoded message body: $sqsMsgBody")
                when (sqsMsgBody) {
                    is SqsMsgBody.PostReadyToRenderMsg -> {
                        renderPost(sqsMsgBody, sourceBucket, project, destinationBucket)
                    }

                    is SqsMsgBody.PageHandlebarsModelMsg -> {
                        logger.error("PageHandlebarsModelMsg is obsolete")
                        renderPage(sqsMsgBody, sourceBucket, project, destinationBucket)
                    }

                    is SqsMsgBody.PageReadyToRenderMsg -> {
                        renderPage(sqsMsgBody, sourceBucket, project, destinationBucket)
                    }

                    is SqsMsgBody.MarkdownPageUploadMsg -> {
                        // do nothing, these messages are not valid in this context
                    }

                    is SqsMsgBody.CssMsg -> renderCSS(sqsMsgBody, sourceBucket, project, destinationBucket)
                    is SqsMsgBody.PageModelMsg, is SqsMsgBody.MarkdownPostUploadMsg -> {
                        // do nothing, these messages are not valid in this context
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
     * Parse and render the given CSS file to the destination bucket. This is most likely to be a straight pass through with no processing
     * @param cssMsg
     * @param sourceBucket the source of the original CSS file TODO: move this to an environment variable for the processor
     * @param project the overall project model
     * @param destinationBucket destination S3 bucket TODO: move this to an environment variable
     *
     */
    private fun renderCSS(
        cssMsg: SqsMsgBody.CssMsg,
        sourceBucket: String,
        project: CantileverProject,
        destinationBucket: String
    ) {
        val model = mutableMapOf<String, Any?>()
        model["project"] = project

        val cssTemplateString = s3Service.getObjectAsString(cssMsg.srcKey, sourceBucket)
        logger.info("Loaded ${cssMsg.srcKey} and rendering via Handlebars to ${cssMsg.destinationKey}")

        val css = with(logger) {
            val renderer = HandlebarsRenderer()
            renderer.render(model = model, template = cssTemplateString)
        }

        s3Service.putObject(cssMsg.destinationKey, destinationBucket, css, "text/css")
        logger.info("Written final CSS file to '${cssMsg.destinationKey}'")
    }

    /**
     * Load the templates and html fragments for the specified page and combine them into the final HTML file
     * @param pageMsg the handlebars model
     * @param sourceBucket the sources for the fragments and metadata json TODO: move this to an environment variable for the processor
     * @param project the overall project model
     * @param destinationBucket destination S3 bucket TODO: move this to an environment variable
     */
    @Deprecated("Use PageReadyToRenderMsg instead")
    private fun renderPage(
        pageMsg: SqsMsgBody.PageHandlebarsModelMsg,
        sourceBucket: String,
        project: CantileverProject,
        destinationBucket: String
    ) {


        val pageTemplateKey = pageMsg.template

        // load the page.html.hbs template
        logger.info("Loading template $pageTemplateKey")
        val sourceString = s3Service.getObjectAsString(pageTemplateKey, sourceBucket)
        // templates now have frontmatter; strip it out
        val templateString = sourceString.stripFrontMatter()
        val model = mutableMapOf<String, Any?>()
        model["key"] = pageMsg.key
        model["url"] = pageMsg.url
        model["project"] = project
        model["title"] = pageMsg.title
        model["lastModified"] = pageMsg.lastModified
        model.putAll(pageMsg.attributes)

        pageMsg.sectionKeys.forEach { (name, objectKey) ->
            val html = s3Service.getObjectAsString(objectKey, sourceBucket)
            logger.info("Adding section $name to model from $objectKey")
            model[name] = html
        }

        model["pages"] = navigationBuilder.filterPages()
        model["posts"] = navigationBuilder.filterPosts()

        logger.info("Final page model keys: ${model.keys}")
        val html = with(logger) {
            val renderer = HandlebarsRenderer()
            renderer.render(model = model, template = templateString)
        }

        // TODO: this is a hack!
        val outputFilename = calculateFilename(pageMsg)
        s3Service.putObject(outputFilename, destinationBucket, html, "text/html")
        logger.info("Written final HTML file to '$outputFilename'")
    }

    /**
     * Load the templates and html fragments for the specified page and combine them into the final HTML file
     * @param pageMsg the handlebars model
     * @param sourceBucket the sources for the fragments and metadata json TODO: move this to an environment variable for the processor
     * @param project the overall project model
     * @param destinationBucket destination S3 bucket TODO: move this to an environment variable
     */
    private fun renderPage(
        pageMsg: SqsMsgBody.PageReadyToRenderMsg,
        sourceBucket: String,
        project: CantileverProject,
        destinationBucket: String
    ) {
        val contentTreeJson = s3Service.getObjectAsString("generated/contentTree.json", sourceBucket)
        val contentTree = Json.decodeFromString<ContentTree>(contentTreeJson)

        val pageTemplateKey = pageMsg.metadata.templateKey

        // load the page.html.hbs template
        logger.info("Loading template $pageTemplateKey")
        val sourceString = s3Service.getObjectAsString(pageTemplateKey, sourceBucket)
        // templates now have frontmatter; strip it out
        val templateString = sourceString.stripFrontMatter()
        val model = mutableMapOf<String, Any?>()
        model["key"] = pageMsg.metadata.srcKey
        model["url"] = pageMsg.metadata.slug
        model["project"] = project
        model["title"] = pageMsg.metadata.title
        model["lastModified"] = pageMsg.metadata.lastUpdated
        model.putAll(pageMsg.metadata.attributes)

        pageMsg.metadata.sections.forEach { (name, objectKey) ->
            val html = s3Service.getObjectAsString(objectKey, sourceBucket)
            logger.info("Adding section $name to model from $objectKey")
            model[name] = html
        }


        logger.info("Final page model keys: ${model.keys}")
        val html = with(logger) {
            val renderer = HandlebarsRenderer()
            renderer.render(model = model, template = templateString)
        }

        // TODO: this is a hack!
        val outputFilename = calculateFilename(pageMsg)
        s3Service.putObject(outputFilename, destinationBucket, html, "text/html")
        logger.info("Written final HTML file to '$outputFilename'")
    }

    /**
     * Load the templates and html fragments for the specified post and combine them into the final HTML file
     * @param postMsg the handlebars model
     * @param sourceBucket the sources for the fragments and metadata json TODO: move this to an environment variable for the processor
     * @param project the overall project model
     * @param destinationBucket destination S3 bucket TODO: move this to an environment variable
     */
    private fun renderPost(
        postMsg: SqsMsgBody.PostReadyToRenderMsg,
        sourceBucket: String,
        project: CantileverProject,
        destinationBucket: String
    ) {
        val body = s3Service.getObjectAsString(postMsg.fragmentKey, sourceBucket)
        logger.info("Loaded body fragment from '${fragments + postMsg.fragmentKey}: ${body.take(100)}'")



        // load template file as specified by metadata
        val template = postMsg.metadata.templateKey
        logger.info("Attempting to load '$template' from bucket '${sourceBucket}' to a string")
        val sourceString = s3Service.getObjectAsString(template, sourceBucket)
        // templates now have frontmatter; strip it out
        val templateString = sourceString.stripFrontMatter()

        // build model from project and from html fragment
        val model = mutableMapOf<String, Any?>()
        model["project"] = project
        model["title"] = postMsg.metadata.title
        model["body"] = body
        model["date"] = postMsg.metadata.date

        val html = with(logger) {
            val nav = navigationBuilder.getPostNavigationObjects(postMsg.metadata, sourceBucket)
            nav.entries.forEach {
                model[it.key] = it.value
            }

            val renderer = HandlebarsRenderer()
            renderer.render(model = model, template = templateString)
        }

        // save to S3
        val outputFilename = calculateFilename(postMsg)
        s3Service.putObject(outputFilename, destinationBucket, html, "text/html")
        logger.info("Written final HTML file to '${outputFilename}'")
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
        logger.info("Calculating final file name for $message")
        return when (message) {
            is SqsMsgBody.PageHandlebarsModelMsg ->
                if (message.key.endsWith(INDEX_MD)) INDEX_HTML else message.key.substringBefore(".$MD")
                    .substringAfterLast("pages/")

            is SqsMsgBody.PostReadyToRenderMsg -> { // not relevant here
                message.metadata.slug
            }

            is SqsMsgBody.MarkdownPostUploadMsg -> { // not relevant here
                message.metadata.slug
            }

            is SqsMsgBody.PageReadyToRenderMsg -> {
                message.metadata.slug
            }

            is SqsMsgBody.PageModelMsg -> { // not relevant here
                ""
            }

            is SqsMsgBody.MarkdownPageUploadMsg -> { // not relevant here
                message.metadata.slug
            }

            is SqsMsgBody.CssMsg -> {
                return message.destinationKey
            }
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
fun LambdaLogger.info(function: String, message: String) = log("INFO: $function:  $message\n")
fun LambdaLogger.info(message: String) = info("TemplateProcessorHandler", message)
fun LambdaLogger.warn(function: String, message: String) = log("WARN: $function:  $message\n")
fun LambdaLogger.warn(message: String) = warn("TemplateProcessorHandler", message)
fun LambdaLogger.error(function: String, message: String) = log("ERROR: $function:  $message\n")
fun LambdaLogger.error(message: String) = error("TemplateProcessorHandler", message)