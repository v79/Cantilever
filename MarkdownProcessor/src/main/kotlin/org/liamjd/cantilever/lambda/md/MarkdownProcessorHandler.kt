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
import org.liamjd.cantilever.common.SOURCE_TYPE
import org.liamjd.cantilever.common.createStringAttribute
import org.liamjd.cantilever.models.sqs.SqsMsgBody
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

    init {
        s3Service = S3ServiceImpl(Region.EU_WEST_2)
        sqsService = SQSServiceImpl(Region.EU_WEST_2)
    }

    override fun handleRequest(event: SQSEvent, context: Context): String {
        val sourceBucket = System.getenv("source_bucket")
        val handlebarQueueUrl = System.getenv(QUEUE.HANDLEBARS)
        val logger = context.logger
        val response = "200 OK"

        val eventRecord = event.records[0]
        logger.info("EventRecord: $eventRecord")

        val sourceType = eventRecord.messageAttributes["sourceType"]?.stringValue ?: "posts"
        logger.info("SourceType: $sourceType")

        when (sourceType) {
            SOURCE_TYPE.POSTS -> {
                val markdownPostUploadMsg =
                    Json.decodeFromString<SqsMsgBody>(eventRecord.body) as SqsMsgBody.MarkdownPostUploadMsg
                logger.info("Metadata: ${markdownPostUploadMsg.metadata}")
                val html = convertMDToHTML(mdSource = markdownPostUploadMsg.markdownText)
                val outputStream = ByteArrayOutputStream()
                outputStream.bufferedWriter().write(html)

                try {
                    val htmlKey = fragments + markdownPostUploadMsg.metadata.slug
                    s3Service.putObject(htmlKey, sourceBucket, html, "text/html")
                    logger.info("Wrote HTML file '$htmlKey'")
                    logger.info("Sending message to handlebars handler")
                    val message = SqsMsgBody.HTMLFragmentReadyMsg(
                        fragmentKey = htmlKey,
                        metadata = markdownPostUploadMsg.metadata
                    )
                    logger.info("Prepared message: $message")

                    val msgResponse = sqsService.sendMessage(toQueue = handlebarQueueUrl, body = message)
                    logger.info("Message '${Json.encodeToString(message)}' sent, message ID is ${msgResponse?.messageId()}")
                } catch (qdne: QueueDoesNotExistException) {
                    logger.error("queue '$handlebarQueueUrl' does not exist; ${qdne.message}")
                } catch (se: SerializationException) {
                    logger.error("Failed to parse metadata string; ${se.message}")
                } catch (e: Exception) {
                    logger.error("${e.message}")
                }
            }

           SOURCE_TYPE.PAGES -> {
                logger.info(eventRecord.body)
                /**
                 * A page is different from a post. It has a different set of metadata.
                 * It may need access to the structure.json file to populate.
                 * It may contain multiple 'content slots'.
                 */
                val pageModel = Json.decodeFromString<SqsMsgBody>(eventRecord.body) as SqsMsgBody.PageModelMsg
                // transform each of the sections from Markdown to HTML and save them as fragments.
                // then build a message model which contains references to each of the fragments
                // which will be passed to the handlebars template
                val fragmentPrefix = fragments + pageModel.key + "/"
                val sectionMap = mutableMapOf<String, String>()
                pageModel.sections.forEach {
                    logger.info("Writing ${it.key} to ${fragmentPrefix}${it.key}")
                    val html = convertMDToHTML(it.value)
                    logger.info(html)
                    s3Service.putObject(fragmentPrefix + it.key, sourceBucket, html, "text/html")
                    sectionMap[it.key] = fragmentPrefix + it.key
                }

                val message = SqsMsgBody.PageHandlebarsModelMsg(
                    key = pageModel.key,
                    template = pageModel.template,
                    attributes = pageModel.attributes,
                    sectionKeys = sectionMap.toMap(),
                    url = pageModel.url?: pageModel.key.substringBeforeLast(".")
                )
                logger.info("Prepared message: $message")

                val msgResponse = sqsService.sendMessage(
                    toQueue = handlebarQueueUrl,
                    body = message,
                    messageAttributes = createStringAttribute("sourceType", sourceType)
                )
                logger.info("Message '${Json.encodeToString(message)}' sent, message ID is ${msgResponse?.messageId()}")
            }

            else -> {
                logger.info("Cannot process unknown sourceType $sourceType")
            }
        }
        return response
    }
}

/**
 * Wrappers for logging to make it slightly less annoying
 */
fun LambdaLogger.info(function: String, message: String) = log("INFO $function:  $message\n")
fun LambdaLogger.info(message: String) = info("MarkdownProcessorHandler", message)
fun LambdaLogger.warn(function: String, message: String) = log("WARN $function:  $message\n")
fun LambdaLogger.warn(message: String) = warn("MarkdownProcessorHandler", message)
fun LambdaLogger.error(function: String, message: String) = log("ERROR $function:  $message\n")
fun LambdaLogger.error(message: String) = error("MarkdownProcessorHandler", message)