package org.liamjd.cantilever.lambda.md

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.charleskorn.kaml.Yaml
import org.liamjd.cantilever.common.QUEUE
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.common.S3_KEY.fragments
import org.liamjd.cantilever.models.CantileverProject
import org.liamjd.cantilever.models.ContentMetaDataBuilder.PageBuilder.extractSectionsFromSource
import org.liamjd.cantilever.models.sqs.ImageSQSMessage
import org.liamjd.cantilever.models.sqs.MarkdownSQSMessage
import org.liamjd.cantilever.models.sqs.TemplateSQSMessage
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.SQSService
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import org.liamjd.cantilever.services.impl.SQSServiceImpl
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException
import java.io.ByteArrayOutputStream

/**
 * Respond to the SQSEvent which will contain the S3 key of the markdown file to parse
 * Read the content of the file and pass it to the `convertMDToHTML` function
 * Then write the resultant file to the destination bucket
 */
class MarkdownProcessorHandler : RequestHandler<SQSEvent, String> {

    private val s3Service: S3Service
    private val sqsService: SQSService
    private val converter: MarkdownConverter
    private lateinit var logger: LambdaLogger

    init {
        s3Service = S3ServiceImpl(Region.EU_WEST_2)
        sqsService = SQSServiceImpl(Region.EU_WEST_2)
        converter = FlexmarkMarkdownConverter()
    }

    override fun handleRequest(event: SQSEvent, context: Context): String {
        val sourceBucket = System.getenv("source_bucket")
        val handlebarQueueUrl = System.getenv(QUEUE.HANDLEBARS)
        logger = context.logger
        val response = "200 OK"

        logger.info("Received ${event.records.size} events received for Markdown processing")

        try {
            // load project defintion from S3
            val projectString = s3Service.getObjectAsString(S3_KEY.projectKey, sourceBucket)
            val project = Yaml.default.decodeFromString<CantileverProject>(projectString)
            event.records.forEach { eventRecord ->
                logger.info("Event record: ${eventRecord.body}")

                when (val sqsMsg = Json.decodeFromString<MarkdownSQSMessage>(eventRecord.body)) {
                    is MarkdownSQSMessage.PostUploadMsg -> {
                        processPostUpload(sqsMsg, project, sourceBucket, handlebarQueueUrl)
                    }

                    is MarkdownSQSMessage.PageUploadMsg -> {
                        processPageUpload(sqsMsg, project, sourceBucket, handlebarQueueUrl)
                    }
                }
            }
        } catch (se: SerializationException) {
            logger.error("Failed to parse project definition string; ${se.message}")
            return "500 Internal Server Error"
        } catch (iae: IllegalArgumentException) {
            logger.error("Failed to parse project definition string; ${iae.message}")
            return "500 Internal Server Error"
        } catch (e: Exception) {
            logger.error("Error processing markdown: ${e.message}")
            return "500 Internal Server Error"
        } catch (e: Exception) {
            logger.error("Error processing markdown: ${e.message}")
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
        project: CantileverProject,
        sourceBucket: String,
        handlebarQueueUrl: String
    ): String {
        val fragmentPrefix = fragments + sqsMsgBody.metadata.slug + "/"
        val sectionMap = mutableMapOf<String, String>()
        var bytesWritten = 0
        var responseString = "200 OK"
        try {
            val fullSourceText = s3Service.getObjectAsString(sqsMsgBody.metadata.srcKey, sourceBucket)
            val sections = extractSectionsFromSource(fullSourceText, true)
            sections.forEach { section ->
                try {
                    logger.info("Writing ${section.key} to ${project.domainKey}${fragmentPrefix}${section.key}")
                    val html = converter.convertMDToHTML(section.value)
                    logger.info("HTML output is ${html.length} characters long.")
                    bytesWritten += s3Service.putObjectAsString(
                        project.domainKey + fragmentPrefix + section.key,
                        sourceBucket,
                        html,
                        "text/html"
                    )
                    sectionMap[section.key] = project.domainKey + fragmentPrefix + section.key

                    // copy any images referenced in the markdown to the destination bucket
                    copyImages("${sqsMsgBody.metadata.srcKey}§${section.key}",  section.value)
                } catch (qdne: QueueDoesNotExistException) {
                    logger.error("queue '$handlebarQueueUrl' does not exist; ${qdne.message}")
                    responseString = "500 Internal Server Error"
                } catch (se: SerializationException) {
                    logger.error("Failed to parse metadata string; ${se.message}")
                    responseString = "500 Internal Server Error"
                } catch (e: Exception) {
                    logger.error("${e.message}")
                    responseString = "500 Internal Server Error"

                }

                // [ContentNode.PageNode] isn't quite suitable for sending to the handlebars queue, so we need to build a new message
                // But the only important difference is how we handle sections.
                // [ContentNode.PageNode.sections] is a Map<String, String>, which is fine, but we need to store a Map<String,SrcKey> (which is a typealias for String anyway)

                val updatedPageNode = sqsMsgBody.metadata.copy(sections = sectionMap.toMap())
                // I don't like the number of times I have to force this value.
                updatedPageNode.parent = sqsMsgBody.metadata.srcKey.substringBeforeLast("/") + "/"

                val message = TemplateSQSMessage.RenderPageMsg(
                    fragmentSrcKey = fragmentPrefix + updatedPageNode.srcKey,
                    metadata = updatedPageNode
                )

                logger.info("${updatedPageNode.sections.size} sections written, totalling $bytesWritten bytes")
                logger.info("Prepared message: $message")

                val msgResponse = sqsService.sendTemplateMessage(
                    toQueue = handlebarQueueUrl,
                    body = message
                )
                logger.info("Message '${Json.encodeToString(message)}' sent, message ID is ${msgResponse?.messageId()}")
                if (msgResponse == null) {
                    logger.warn("No response received for message")
                    responseString = "500 Internal Server Error"
                }
            }
        } catch (e: Exception) {
            logger.error("Error processing page: ${e.message}")
            responseString = "500 Internal Server Error"
        }
        return responseString
    }

    /**
     * Posts are the simpler type of markdown content, as they are not split into separate sections. There is only a body.
     */
    private fun processPostUpload(
        sqsMsgBody: MarkdownSQSMessage.PostUploadMsg,
        project: CantileverProject,
        sourceBucket: String,
        handlebarQueueUrl: String
    ): String {
        logger.info("Metadata: ${sqsMsgBody.metadata}")
        val html = converter.convertMDToHTML(mdSource = sqsMsgBody.markdownText)
        val outputStream = ByteArrayOutputStream()
        var responseString = "200 OK"
        outputStream.bufferedWriter().write(html)

        try {
            val htmlKey = project.domainKey + fragments + sqsMsgBody.metadata.slug
            s3Service.putObjectAsString(htmlKey, sourceBucket, html, "text/html")
            logger.info("Wrote HTML file '$htmlKey'")
            logger.info("Sending message to handlebars handler")
            val message = TemplateSQSMessage.RenderPostMsg(
                fragmentSrcKey = htmlKey,
                metadata = sqsMsgBody.metadata
            )
            logger.info("Prepared message: $message")

            val msgResponse = sqsService.sendTemplateMessage(toQueue = handlebarQueueUrl, body = message)
            logger.info("Message '${Json.encodeToString(message)}' sent, message ID is ${msgResponse?.messageId()}")

            // now attempt to copy the images referenced in the markdown sources
            copyImages(sqsMsgBody.metadata.srcKey, sqsMsgBody.markdownText)

        } catch (qdne: QueueDoesNotExistException) {
            logger.error("queue '$handlebarQueueUrl' does not exist; ${qdne.message}")
            responseString = "500 Internal Server Error"
        } catch (se: SerializationException) {
            logger.error("Failed to parse metadata string; ${se.message}")
            responseString = "500 Internal Server Error"
        } catch (e: Exception) {
            logger.error("${e.message}")
            responseString = "500 Internal Server Error"
        }
        return responseString
    }

    /**
     * Copy any images referenced in the markdown to the destination bucket
     * By sending a copy message to the image processor queue
     */
    private fun copyImages(descriptor: String, mdSource: String) {
        val images = converter.extractImages(mdSource)
        logger.info("Found ${images.size} images in markdown '$descriptor'")

        if (images.isNotEmpty()) {
            try {
                val message = ImageSQSMessage.CopyImagesMsg(images.map { it.url.toString() })
                logger.info("Prepared message: $message")
                val msgResponse = sqsService.sendImageMessage(toQueue = System.getenv(QUEUE.IMAGES), body = message)
                if (msgResponse == null) {
                    logger.warn("No response received for message")
                } else {
                    logger.info(
                        "Message '${Json.encodeToString(message)}' sent, message ID is ${msgResponse.messageId()}"
                    )
                }
            } catch (qdne: QueueDoesNotExistException) {
                logger.error("queue '${QUEUE.IMAGES}' does not exist; ${qdne.message}")
            } catch (e: Exception) {
                logger.error("${e.message}")
            }
        }
    }
}

/**
 * Wrappers for logging to make it slightly less annoying
 */
fun LambdaLogger.info(function: String, message: String) = log("INFO: $function:  $message\n")
fun LambdaLogger.info(message: String) = info("MarkdownProcessorHandler", message)
fun LambdaLogger.warn(function: String, message: String) = log("WARN: $function:  $message\n")
fun LambdaLogger.warn(message: String) = warn("MarkdownProcessorHandler", message)
fun LambdaLogger.error(function: String, message: String) = log("ERROR: $function:  $message\n")
fun LambdaLogger.error(message: String) = error("MarkdownProcessorHandler", message)