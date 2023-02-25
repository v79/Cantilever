package org.liamjd.cantilever.lambda.md

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.liamjd.cantilever.common.s3Keys.fragmentsKey
import org.liamjd.cantilever.models.sqs.HTMLFragmentReadyMsg
import org.liamjd.cantilever.models.sqs.MarkdownPostUploadMsg
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import java.io.ByteArrayOutputStream

/**
 * Respond to the SQSEvent which will contain the S3 key of the markdown file to parse
 * Read the content of the file and pass it to the `convertMDToHTML` function
 * Then write the resultant file to the destination bucket
 */
class MarkdownProcessorHandler : RequestHandler<SQSEvent, String> {

    override fun handleRequest(event: SQSEvent, context: Context): String {
        val sourceBucket = System.getenv("source_bucket")
        val handlebarQueueUrl = System.getenv("handlebar_template_queue")
        val logger = context.logger
        var response = "200 OK"

        val eventRecord = event.records[0]
        logger.info("EventRecord: $eventRecord")

        val sourceType = eventRecord.messageAttributes["sourceType"]?.stringValue ?: "posts"
        logger.info("SourceType: $sourceType")



        when(sourceType) {
            "posts" -> {
                val markdownPostUploadMsg = Json.decodeFromString<MarkdownPostUploadMsg>(eventRecord.body)
                logger.info("Metadata: ${markdownPostUploadMsg.metadata}")
                logger.info("Processing post")
                val html = convertMDToHTML(mdSource = markdownPostUploadMsg.markdownText)
                logger.info("HTML Output: ${html.take(150)}...")

                val s3Client = S3Client.builder()
                    .region(Region.EU_WEST_2)
                    .build()

                val outputStream = ByteArrayOutputStream()
                outputStream.bufferedWriter().write(html)

                val handlebarQueue = SqsClient.builder().region(Region.EU_WEST_2).build()

                try {
                    val htmlKey = fragmentsKey + markdownPostUploadMsg.metadata.slug
                    s3Client.putObject(
                        PutObjectRequest.builder().contentLength(html.length.toLong()).contentType("text/html")
                            .bucket(sourceBucket).key(htmlKey).build(),
                        RequestBody.fromBytes(html.toByteArray())
                    )
                    logger.info("Wrote HTML file '$htmlKey'")
                    logger.info("Sending message to handlebars handler")
                    val message = HTMLFragmentReadyMsg(fragmentKey = htmlKey, metadata = markdownPostUploadMsg.metadata)
                    logger.info("Prepared message: $message")
                    val msgResponse = handlebarQueue.sendMessage(
                        SendMessageRequest.builder()
                            .queueUrl(handlebarQueueUrl)
                            .messageBody(Json.encodeToString(message))
                            .build()
                    )
                    logger.info("Message '${Json.encodeToString(message)}' sent, message ID is ${msgResponse.messageId()}")
                } catch (qdne: QueueDoesNotExistException) {
                    logger.error("queue '$handlebarQueueUrl' does not exist; ${qdne.message}")
                } catch (se: SerializationException) {
                    logger.error("Failed to parse metadata string; ${se.message}")
                } catch (e: Exception) {
                    logger.error("${e.message}")
                }
            }
            "pages" -> {
                logger.info("Processing page (not written yet)")
                /**
                 * A page is different from a post. It has a different set of metadata.
                 * It may need access to the structure.json file to populate.
                 * It may contain multiple 'content slots'.
                 */

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