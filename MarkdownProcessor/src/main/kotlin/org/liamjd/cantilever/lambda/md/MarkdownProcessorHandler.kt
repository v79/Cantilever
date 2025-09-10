package org.liamjd.cantilever.lambda.md

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.liamjd.cantilever.common.QUEUE
import org.liamjd.cantilever.common.S3_KEY.fragments
import org.liamjd.cantilever.models.ContentMetaDataBuilder.PageBuilder.extractSectionsFromSource
import org.liamjd.cantilever.models.sqs.ImageSQSMessage
import org.liamjd.cantilever.models.sqs.MarkdownSQSMessage
import org.liamjd.cantilever.models.sqs.TemplateSQSMessage
import org.liamjd.cantilever.services.AWSLogger
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.SQSService
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import org.liamjd.cantilever.services.impl.SQSServiceImpl
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException
import java.io.ByteArrayOutputStream

/**
 * Set up Koin dependency injection for the MarkdownProcessor module
 */
val markdownProcessorModule = module {
    single<S3Service> { S3ServiceImpl(Region.EU_WEST_2) }
    single<SQSService> { SQSServiceImpl(Region.EU_WEST_2) }
    single<MarkdownConverter> { FlexmarkMarkdownConverter() }
}

object MarkdownKoinSetup {
    fun setup(modules: List<Module>) {
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                modules(markdownProcessorModule)
            }
        }
    }
}

/**
 * Respond to the SQSEvent which will contain the S3 key of the Markdown file to parse
 * Read the content of the file and pass it to the `convertMDToHTML` function
 * Then write the resultant file to the generation bucket
 * This class produces fragments, which are then passed to the TemplateProcessorHandler
 */
@Suppress("unused")
class MarkdownProcessorHandler : RequestHandler<SQSEvent, String>, KoinComponent,
    AWSLogger(enableLogging = true, msgSource = "MarkdownProcessorHandler") {

    init {
        MarkdownKoinSetup.setup(listOf(markdownProcessorModule))
    }

    private val s3Service: S3Service by inject()
    private val sqsService: SQSService by inject()
    private val converter: MarkdownConverter by inject()

    override var logger: LambdaLogger? = null

    override fun handleRequest(event: SQSEvent, context: Context): String {
        val sourceBucket = System.getenv("source_bucket")
        val generationBucket = System.getenv("generation_bucket")
        val handlebarQueueUrl = System.getenv(QUEUE.HANDLEBARS)
        logger = context.logger
        val response = "200 OK"

        log("Received ${event.records.size} events received for Markdown processing")

        try {
            event.records.forEach { eventRecord ->
                val sqsMsg = Json.decodeFromString<MarkdownSQSMessage>(eventRecord.body)
                log("Decoded SQS message: $sqsMsg")
                when (sqsMsg) {
                    is MarkdownSQSMessage.PostUploadMsg -> {
                        processPostUpload(sqsMsg, sourceBucket, generationBucket, handlebarQueueUrl)
                    }

                    is MarkdownSQSMessage.PageUploadMsg -> {
                        processPageUpload(sqsMsg, sourceBucket, generationBucket, handlebarQueueUrl)
                    }
                }
            }
        } catch (se: SerializationException) {
            log("ERROR", "Failed to parse project definition string; ${se.message}")
            return "500 Internal Server Error"
        } catch (iae: IllegalArgumentException) {
            log("ERROR", "Failed to parse project definition string; ${iae.message}")
            return "500 Internal Server Error"
        } catch (e: Exception) {
            log("ERROR", "Error processing markdown: ${e.message}")
            return "500 Internal Server Error"
        } catch (e: Exception) {
            log("ERROR", "Error processing markdown: ${e.message}")
            return "500 Internal Server Error"
        }
        return response
    }

    /**
     * A page is more complicated than a post, as it may be split into multiple sections.
     * Transform each of the sections from Markdown to HTML and save these as fragments.
     * Then build a message model which contains references to each of the fragments,
     * which will be passed to the TemplateProcessor
     */
    private fun processPageUpload(
        sqsMsgBody: MarkdownSQSMessage.PageUploadMsg,
        sourceBucket: String,
        generationBucket: String,
        handlebarQueueUrl: String
    ): String {
        val fragmentPrefix = fragments + sqsMsgBody.metadata.slug + "/"
        val sectionMap = mutableMapOf<String, String>()
        val domain = sqsMsgBody.projectDomain
        var bytesWritten = 0
        var responseString = "200 OK"
        try {
            val fullSourceText = s3Service.getObjectAsString(sqsMsgBody.metadata.srcKey, sourceBucket)
            val sections = extractSectionsFromSource(fullSourceText, true)
            sections.forEach { section ->
                try {
                    log("Writing ${section.key} to $domain/${fragmentPrefix}${section.key}")
                    val html = converter.convertMDToHTML(section.value)
                    log("HTML output is ${html.length} characters long.")
                    bytesWritten += s3Service.putObjectAsString(
                        domain + "/" + fragmentPrefix + section.key,
                        generationBucket,
                        html,
                        "text/html"
                    )
                    sectionMap[section.key] = domain + "/" + fragmentPrefix + section.key

                    // copy any images referenced in the Markdown to the destination bucket
                    copyImages("${sqsMsgBody.metadata.srcKey}§${section.key}", section.value, domain)
                } catch (qdne: QueueDoesNotExistException) {
                    log("ERROR", "queue '$handlebarQueueUrl' does not exist; ${qdne.message}")
                    responseString = "500 Internal Server Error"
                } catch (se: SerializationException) {
                    log("ERROR", "Failed to parse metadata string; ${se.message}")
                    responseString = "500 Internal Server Error"
                } catch (e: Exception) {
                    log("ERROR", "${e.message}")
                    responseString = "500 Internal Server Error"
                }

                // [ContentNode.PageNode] isn't quite suitable for sending to the Handlebars queue, so we need to build a new message
                // But the only important difference is how we handle sections.
                // [ContentNode.PageNode.sections] is a Map<String, String>, which is fine, but we need to store a Map<String, SrcKey> (which is a typealias for String anyway)

                val updatedPageNode = sqsMsgBody.metadata.copy(sections = sectionMap.toMap())
                log("Updated page node: $updatedPageNode")

                val message = TemplateSQSMessage.RenderPageMsg(
                    projectDomain = domain,
                    fragmentSrcKey = fragmentPrefix + updatedPageNode.srcKey,
                    metadata = updatedPageNode
                )

                log("${updatedPageNode.sections.size} sections written, totalling $bytesWritten bytes")
                val msgResponse = sqsService.sendTemplateMessage(
                    toQueue = handlebarQueueUrl,
                    body = message
                )
                log("Message '${Json.encodeToString(message)}' sent, message ID is ${msgResponse?.messageId()}")
                if (msgResponse == null) {
                    log("WARN", "No response received for message")
                    responseString = "500 Internal Server Error"
                }
            }
        } catch (e: Exception) {
            log("ERROR", "Error processing page: ${e.message}")
            responseString = "500 Internal Server Error"
        }
        return responseString
    }

    /**
     * Posts are the simpler type of Markdown content, as they are not split into separate sections. There is only a body.
     */
    private fun processPostUpload(
        sqsMsgBody: MarkdownSQSMessage.PostUploadMsg,
        sourceBucket: String,
        generationBucket: String,
        handlebarQueueUrl: String
    ): String {
        val html = converter.convertMDToHTML(mdSource = sqsMsgBody.markdownText)
        val outputStream = ByteArrayOutputStream()
        var responseString = "200 OK"
        outputStream.bufferedWriter().write(html)

        try {
            // get project definition from the source bucket
            val domain = sqsMsgBody.projectDomain
            val htmlKey = domain + "/" + fragments + sqsMsgBody.metadata.slug
            s3Service.putObjectAsString(htmlKey, generationBucket, html, "text/html")
            log("Wrote HTML file '$htmlKey'")
            log("Sending message to handlebars handler")
            val message = TemplateSQSMessage.RenderPostMsg(
                projectDomain = domain,
                fragmentSrcKey = htmlKey,
                metadata = sqsMsgBody.metadata
            )
            val msgResponse = sqsService.sendTemplateMessage(toQueue = handlebarQueueUrl, body = message)
            log("Message '${Json.encodeToString(message)}' sent, message ID is ${msgResponse?.messageId()}")

            // now attempt to copy the images referenced in the Markdown sources
            copyImages(sqsMsgBody.metadata.srcKey, sqsMsgBody.markdownText, domain)

        } catch (qdne: QueueDoesNotExistException) {
            log("ERROR", "queue '$handlebarQueueUrl' does not exist; ${qdne.message}")
            responseString = "500 Internal Server Error"
        } catch (se: SerializationException) {
            log("ERROR", "Failed to parse metadata string; ${se.message}")
            responseString = "500 Internal Server Error"
        } catch (nske: NoSuchKeyException) {
            log("ERROR", "Project definition file not found; ${nske.message}")
            responseString = "500 Internal Server Error"
        } catch (e: Exception) {
            log("ERROR", "${e.message}")
            responseString = "500 Internal Server Error"
        }
        return responseString
    }

    /**
     * Copy any images referenced in the Markdown to the destination bucket
     * By sending a copy message to the image processor queue
     */
    private fun copyImages(descriptor: String, mdSource: String, domain: String) {
        val images = converter.extractImages(mdSource)
        log("Found ${images.size} images in markdown '$descriptor'")

        if (images.isNotEmpty()) {
            try {
                // This is the Markdown relative path for an image, i.e. /assets/my-image.jpg
                // But elsewhere we expect to be /sources/images/assets/my-image.jpg
                val message = ImageSQSMessage.CopyImagesMsg(domain, images.map { "/sources/images${it.url}" })
                log("Prepared message: $message")
                val msgResponse = sqsService.sendImageMessage(toQueue = System.getenv(QUEUE.IMAGES), body = message)
                if (msgResponse == null) {
                    log("WARN", "No response received for message")
                } else {
                    log(
                        "Message '${Json.encodeToString(message)}' sent, message ID is ${msgResponse.messageId()}"
                    )
                }
            } catch (qdne: QueueDoesNotExistException) {
                log("ERROR", "queue '${QUEUE.IMAGES}' does not exist; ${qdne.message}")
            } catch (e: Exception) {
                log("ERROR", "${e.message}")
            }
        }
    }
}
