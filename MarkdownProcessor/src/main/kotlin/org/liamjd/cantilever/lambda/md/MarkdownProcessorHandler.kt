package org.liamjd.cantilever.lambda.md

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.liamjd.cantilever.common.QUEUE
import org.liamjd.cantilever.common.S3_KEY.fragments
import org.liamjd.cantilever.models.ContentMetaDataBuilder.PageBuilder.extractSectionsFromSource
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
    private lateinit var logger: LambdaLogger

    init {
        s3Service = S3ServiceImpl(Region.EU_WEST_2)
        sqsService = SQSServiceImpl(Region.EU_WEST_2)
    }

    override fun handleRequest(event: SQSEvent, context: Context): String {
        val sourceBucket = System.getenv("source_bucket")
        val handlebarQueueUrl = System.getenv(QUEUE.HANDLEBARS)
        logger = context.logger
        val response = "200 OK"

        logger.info("Received ${event.records.size} events received for Markdown processing")

        event.records.forEach { eventRecord ->
            logger.info("Event record: ${eventRecord.body}")

            when (val sqsMsg = Json.decodeFromString<MarkdownSQSMessage>(eventRecord.body)) {
                is MarkdownSQSMessage.PostUploadMsg -> {
                    processPostUpload(sqsMsg, sourceBucket, handlebarQueueUrl)
                }

                is MarkdownSQSMessage.PageUploadMsg -> {
                    processPageUpload(sqsMsg, sourceBucket, handlebarQueueUrl)
                }
            }
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
        handlebarQueueUrl: String
    ): String {
        val fragmentPrefix = fragments + sqsMsgBody.metadata.slug + "/"
        val sectionMap = mutableMapOf<String, String>()
        var bytesWritten = 0
        var responseString = "200 OK"
        val fullSourceText = s3Service.getObjectAsString(sqsMsgBody.metadata.srcKey, sourceBucket)
        val sections = extractSectionsFromSource(fullSourceText, true)
        sections.forEach {
            try {
                logger.info("Writing ${it.key} to ${fragmentPrefix}${it.key}")
                val html = convertMDToHTML(it.value)
                logger.info("HTML output is ${html.length} characters long.")
                bytesWritten += s3Service.putObject(
                    fragmentPrefix + it.key,
                    sourceBucket,
                    html,
                    "text/html"
                )
                sectionMap[it.key] = fragmentPrefix + it.key
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
        return responseString
    }

    /**
     * Posts are the simpler type of markdown content, as they are not split into separate sections. There is only a body.
     */
    private fun processPostUpload(
        sqsMsgBody: MarkdownSQSMessage.PostUploadMsg,
        sourceBucket: String,
        handlebarQueueUrl: String
    ): String {
        logger.info("Metadata: ${sqsMsgBody.metadata}")
        val html = convertMDToHTML(mdSource = sqsMsgBody.markdownText)
        val outputStream = ByteArrayOutputStream()
        var responseString = "200 OK"
        outputStream.bufferedWriter().write(html)

        try {
            val htmlKey = fragments + sqsMsgBody.metadata.slug
            s3Service.putObject(htmlKey, sourceBucket, html, "text/html")
            logger.info("Wrote HTML file '$htmlKey'")
            logger.info("Sending message to handlebars handler")
            val message = TemplateSQSMessage.RenderPostMsg(
                fragmentSrcKey = htmlKey,
                metadata = sqsMsgBody.metadata
            )
            logger.info("Prepared message: $message")

            val msgResponse = sqsService.sendTemplateMessage(toQueue = handlebarQueueUrl, body = message)
            logger.info("Message '${Json.encodeToString(message)}' sent, message ID is ${msgResponse?.messageId()}")
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