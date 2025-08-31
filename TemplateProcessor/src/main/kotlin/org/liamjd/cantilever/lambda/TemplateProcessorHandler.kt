package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.liamjd.cantilever.common.EnvironmentProvider
import org.liamjd.cantilever.common.FILES.INDEX_HTML
import org.liamjd.cantilever.common.FILES.INDEX_MD
import org.liamjd.cantilever.common.FILE_TYPE.MD
import org.liamjd.cantilever.common.SystemEnvironmentProvider
import org.liamjd.cantilever.common.stripFrontMatter
import org.liamjd.cantilever.models.CantileverProject
import org.liamjd.cantilever.models.sqs.TemplateSQSMessage
import org.liamjd.cantilever.services.AWSLogger
import org.liamjd.cantilever.services.DynamoDBService
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.impl.DynamoDBServiceImpl
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

/**
 * Set up dependency injection
 */
val templateProcessorModule = module {
    single<S3Service> { S3ServiceImpl(Region.EU_WEST_2) }
    single<DynamoDBService> {
        DynamoDBServiceImpl(
            region = Region.EU_WEST_2, enableLogging = true, dynamoDbClient = DynamoDbAsyncClient.create()
        )
    }
}

/**
 * This object is used to set up Koin dependency injection. It ensures that Koin is only started once, even in unit testing scenarios.
 */
object KoinSetup {
    fun setup(modules: List<Module>) {
        // Only start Koin if it's not already running
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                modules(templateProcessorModule)
            }
        }
    }
}

/**
 * Processes a message to transform the source and template into a complete HTML web page
 */
@Suppress("unused")
class TemplateProcessorHandler(private val environmentProvider: EnvironmentProvider = SystemEnvironmentProvider()) :
    RequestHandler<SQSEvent, String>, KoinComponent,
    AWSLogger(enableLogging = true, msgSource = "TemplateProcessorHandler") {

    init {
        KoinSetup.setup(listOf(templateProcessorModule))
    }

    private val s3Service: S3Service by inject()
    private val dynamoDBService: DynamoDBService by inject()

    override var logger: LambdaLogger? = null

    override fun handleRequest(event: SQSEvent, context: Context): String {
        logger = context.logger
        val responses = mutableListOf<String>()

        val sourceBucket = environmentProvider.getEnv("source_bucket")
        val generationBucket = environmentProvider.getEnv("generation_bucket")
        val destinationBucket = environmentProvider.getEnv("destination_bucket")

        log("${event.records.size} records received for processing...")

        event.records.forEach { eventRecord ->
            try {
                val sqsMsg = Json.decodeFromString<TemplateSQSMessage>(eventRecord.body)
                log(sqsMsg.toString())
                when (sqsMsg) {
                    is TemplateSQSMessage.RenderPostMsg -> {
                        renderPost(sqsMsg, sourceBucket, generationBucket, destinationBucket)
                    }

                    is TemplateSQSMessage.RenderPageMsg -> {
                        renderPage(sqsMsg, sourceBucket, generationBucket, destinationBucket)
                    }

                    is TemplateSQSMessage.StaticRenderMsg -> {
                        renderStatic(sqsMsg, sourceBucket, generationBucket, destinationBucket)
                    }
                }
            } catch (se: SerializationException) {
                log("ERROR", "Failed to deserialize eventRecord $eventRecord, exception: ${se.message}")
                responses.add("500 Server Error")
            } catch (nske: NoSuchKeyException) {
                log("ERROR", "Could not load file from S3, exception: ${nske.message}")
                responses.add("500 Server Error")
            }
        }

        return if (responses.isEmpty()) "200 OK" else "${responses.size} errors, final was ${responses.last()}"
    }


    /**
     * Load the templates and HTML fragments for the specified page and combine them into the final HTML file
     * @param pageMsg the Handlebars model
     * @param sourceBucket the sources for the fragments and metadata json TODO: move this to an environment variable for the processor
     * @param generationBucket the intermediate bucket for the generated files
     * @param destinationBucket destination S3 bucket TODO: move this to an environment variable
     */
    private fun renderPage(
        pageMsg: TemplateSQSMessage.RenderPageMsg,
        sourceBucket: String,
        generationBucket: String,
        destinationBucket: String
    ) {
        try {
            val domain = pageMsg.projectDomain
            val pageTemplateKey = domain + "/" + pageMsg.metadata.templateKey
            val project = getProjectModel(pageMsg.projectDomain) ?: throw Exception("Project model is null")
            val navigationBuilder = NavigationBuilder(dynamoDBService, domain)
            val model = mutableMapOf<String, Any?>()
            model["key"] = pageMsg.metadata.srcKey
            model["url"] = pageMsg.metadata.url
            model["project"] = project
            model["title"] = pageMsg.metadata.title
            model["lastModified"] = pageMsg.metadata.lastUpdated
            model.putAll(pageMsg.metadata.attributes)

            // TODO: Restore the pages and posts model elements; this is necessary for home page and other navigation elements
            // TODO: Though I don't want to store everything, so perhaps we can determine the required set elsewhere
//            model["pages"] = navigationBuilder.filterPages()
            model["posts"] = navigationBuilder.getPosts(25)

            pageMsg.metadata.sections.forEach { (name, objectKey) ->
                val html = s3Service.getObjectAsString(objectKey, generationBucket)
                log("Adding section $name to model from generated $objectKey")
                model[name] = html
            }

            val renderer = HandlebarsRenderer(s3Service = s3Service, srcBucket = sourceBucket, domain = domain)
            println("Rending template '${pageMsg.metadata.templateKey}' for '${pageMsg.metadata.srcKey}' with model keys ${model.keys}")
            val html = renderer.render(model = model, srcKey = pageTemplateKey)
            log("Calculated URL for page: ${pageMsg.metadata.url} from parentFolder: ${pageMsg.metadata.parent} and srcKey: ${pageMsg.metadata.srcKey}")
            s3Service.putObjectAsString(pageMsg.metadata.url, destinationBucket, html, "text/html")
            log("Written final HTML file to '${pageMsg.metadata.url}'")
        } catch (nske: NoSuchKeyException) {
            log("ERROR", "Could not load file from S3, exception: ${nske.message}")
        }
    }

    /**
     * Load the templates and HTML fragments for the specified post and combine them into the final HTML file
     * @param postMsg the Handlebars model
     * @param sourceBucket the sources for the fragments and metadata json TODO: move this to an environment variable for the processor
     * @param generationBucket the intermediate bucket for the generated files
     * @param destinationBucket destination S3 bucket TODO: move this to an environment variable
     */
    private fun renderPost(
        postMsg: TemplateSQSMessage.RenderPostMsg,
        sourceBucket: String,
        generationBucket: String,
        destinationBucket: String
    ) {
        try {
            val domain = postMsg.projectDomain
            val body = s3Service.getObjectAsString(postMsg.fragmentSrcKey, generationBucket)
            log("Loaded body fragment from '${postMsg.fragmentSrcKey}: ${body.take(100)}'")

            val project = getProjectModel(postMsg.projectDomain) ?: throw Exception("Project model is null")
            val postTemplateKey = domain + "/" + postMsg.metadata.templateKey
            // build model from project and from HTML fragment
            val model = mutableMapOf<String, Any?>()
            model["project"] = project
            model["title"] = postMsg.metadata.title
            model["body"] = body.stripFrontMatter()
            model["date"] = postMsg.metadata.date
            // TODO: try-catch this
            val navigationBuilder = NavigationBuilder(dynamoDBService, domain)

            val nav = navigationBuilder.getPostNavigationObjects(postMsg.metadata)
            nav.entries.forEach {
                model[it.key] = it.value
            }

            val renderer = HandlebarsRenderer(s3Service = s3Service, srcBucket = sourceBucket, domain = domain)
            println("Rending template '${postMsg.metadata.templateKey}' for '${postMsg.metadata.srcKey}' with model keys ${model.keys}")
            val html = renderer.render(model = model, srcKey = postTemplateKey)

            // save to S3
            s3Service.putObjectAsString(project.domainKey + postMsg.metadata.url, destinationBucket, html, "text/html")
            log("Written final HTML file to '${project.domainKey}${postMsg.metadata.url}'")
        } catch (nske: NoSuchKeyException) {
            log("ERROR", "Could not load file from S3, exception: ${nske.message}")
        }
    }

    /**
     * Parse and renderInline the given CSS file to the destination bucket. This is most likely to be a straight pass-through with no processing
     * @param staticFileMsg
     * @param sourceBucket the source of the original CSS file TODO: move this to an environment variable for the processor
     * @param generationBucket the intermediate bucket for the generated files
     * @param destinationBucket destination S3 bucket TODO: move this to an environment variable
     *
     */
    private fun renderStatic(
        staticFileMsg: TemplateSQSMessage.StaticRenderMsg,
        sourceBucket: String,
        generationBucket: String,
        destinationBucket: String
    ) {
        val model = mutableMapOf<String, Any?>()
        try {
            val domain = staticFileMsg.projectDomain
            val project = getProjectModel(staticFileMsg.projectDomain)
            if (project != null) {
                model["project"] = project

                val cssTemplateString = s3Service.getObjectAsString(staticFileMsg.srcKey, sourceBucket)
                log("Loaded ${staticFileMsg.srcKey} and rendering via Handlebars to ${staticFileMsg.destinationKey}")

                val renderer = HandlebarsRenderer(s3Service, sourceBucket, domain)
                val css = renderer.renderInline(model = model, templateString = cssTemplateString)

                s3Service.putObjectAsString(
                    project.domainKey + staticFileMsg.destinationKey, destinationBucket, css, "text/css"
                )
                log("Written final CSS file to '${project.domainKey}${staticFileMsg.destinationKey}'")
            }
        } catch (nske: NoSuchKeyException) {
            log("ERROR", "Could not load file from S3, exception: ${nske.message}")
        }
    }


    /**
     * Calculate the final output file name
     * For posts:
     * - this is the `metadata.slug` object if it exists, or the source file name minus extensions if no slug exists
     * For pages:
     * - For the home page (i.e. for page index.md) this needs to be index.html
     * - For all other pages, this should be the source file name minus the extension
     */
    @Deprecated("Now URL is a calculated property of the ContentNode")
    private fun calculateFilename(message: TemplateSQSMessage): String {
        log("Calculating final file name for $message")
        return when (message) {
            is TemplateSQSMessage.RenderPageMsg -> if (message.metadata.srcKey.endsWith(INDEX_MD)) INDEX_HTML else message.metadata.srcKey.substringBefore(
                ".$MD"
            ).substringAfterLast("pages/")

            is TemplateSQSMessage.RenderPostMsg -> {
                message.metadata.slug
            }

            is TemplateSQSMessage.StaticRenderMsg -> {
                message.destinationKey
            }
        }
    }

    /**
     * Return the CantileverProject model
     * @param domain the domain to load the project for
     * @return the CantileverProject model or null if it could not be loaded
     */
    private fun getProjectModel(domain: String): CantileverProject? {
        val project = runBlocking {
            try {
                dynamoDBService.getProject(domain)
            } catch (e: Exception) {
                log("ERROR", "Could not load project model for domain '$domain', exception: ${e.message}")
            }
        }
        return project as? CantileverProject?
    }

}

