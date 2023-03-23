package org.liamjd.cantilever.api.controllers

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.*
import org.liamjd.cantilever.models.PageList
import org.liamjd.cantilever.models.sqs.SqsMsgBody
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.SQSService
import org.liamjd.cantilever.services.impl.extractPageModel
import org.liamjd.cantilever.services.impl.extractPostMetadata
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

/**
 * Handle routes relating to document generation. Mostly this will be done by sending messages to the appropriate queues.
 */
class GeneratorController(val sourceBucket: String) : KoinComponent, APIController {

    companion object {
        const val PAGES_DIR = S3_KEY.sources + SOURCE_TYPE.PAGES + "/"
        const val POSTS_DIR = S3_KEY.sources + SOURCE_TYPE.POSTS + "/"
        const val TEMPLATES_DIR = S3_KEY.templates + "/"
        const val error_NO_RESPONSE = "No response received for message"
        const val pagesKey = "generated/pages.json"
    }

    private val s3Service: S3Service by inject()
    private val sqsService: SQSService by inject()

    private val markdownQueue: String = System.getenv(QUEUE.MARKDOWN) ?: "markdown_processing_queue"

    /**
     * Generate the HTML version of the page specified by the path parameter 'srcKey'.
     * The actual path searched for will be `/sources/pages/<srcKey>'.
     * This method will send a message to the markdown processing queue in SQS.
     * If <srcKey> is '*' it will trigger regeneration of all source markdown pages
     */
    fun generatePage(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val requestKey = request.pathParameters["srcKey"]
        if (requestKey == "*") {
            println("Wow, that's a big request")

            // get every page in the pages folder
            val pageListResponse = s3Service.listObjects(PAGES_DIR, sourceBucket)
            println("There are ${pageListResponse.keyCount()} potential pages to process")
            pageListResponse.contents().filter { it.key().endsWith(FILE_TYPE.MD) }.forEach { obj ->
                println(obj.key())
                val sourceString = s3Service.getObjectAsString(obj.key(), sourceBucket)
                val pageSrcKey =
                    obj.key().removePrefix(PAGES_DIR) // just want the actual file name
                // extract page model
                val pageModel = extractPageModel(pageSrcKey, sourceString)
                val msgResponse = sqsService.sendMessage(
                    toQueue = markdownQueue,
                    body = pageModel,
                    messageAttributes = createStringAttribute("sourceType", SOURCE_TYPE.PAGES)
                )

                if (msgResponse != null) {
                    println("Message '${obj.key()}' sent to '$markdownQueue', message ID is ${msgResponse.messageId()}'")
                } else {
                    println(error_NO_RESPONSE)
                }
            }

            return ResponseEntity.notImplemented(body = APIResult.OK("Mass page generation not yet implemented."))
        } else {
            val srcKey = PAGES_DIR + requestKey
            println("GeneratorController: Received request to regenerate page $srcKey")
            try {
                println("Received page file $srcKey and sending it to Markdown processor queue")
                val sourceString = s3Service.getObjectAsString(srcKey, sourceBucket)
                val pageSrcKey = srcKey.removePrefix(
                    PAGES_DIR
                ) // just want the actual file name
               queuePageRegeneration(pageSrcKey,sourceString)
            } catch (nske: NoSuchKeyException) {
                println("${nske.message} for key $srcKey")
                return ResponseEntity.notFound(body = APIResult.Error(message = "Could not find page with key $srcKey"))
            }
        }
        return ResponseEntity.ok(body = APIResult.Success(value = "Regenerated page $requestKey"))
    }

    /**
     * Generate the HTML fragments of the post specified by the path parameter 'srcKey'.
     * The actual path searched for will be `/sources/posts/<srcKey>`.
     * This method will send a message to the markdown processing queue in SQS.
     * Does not currently handle the '*' wildcard.
     */
    fun generatePost(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val requestKey = request.pathParameters["srcKey"]
        if (requestKey == "*") {
            return ResponseEntity.notImplemented(body = APIResult.Error("Cannot yet regenerate all posts. Please specify I unique key"))
        }
        val srcKey = POSTS_DIR + requestKey
        println("GeneratorController: Received request to regenerate post '$srcKey'")
        try {
            val sourceString = s3Service.getObjectAsString(srcKey, sourceBucket)
            val postSrcKey = srcKey.removePrefix(POSTS_DIR)
            val postMetadata = extractPostMetadata(postSrcKey, sourceString)
            val markdownBody = sourceString.stripFrontMatter()
            val message = SqsMsgBody.MarkdownPostUploadMsg(postMetadata, markdownBody)
            println("Built post metadata: $postMetadata")

            val msgResponse = sqsService.sendMessage(
                toQueue = markdownQueue,
                body = message,
                messageAttributes = createStringAttribute("sourceType", SOURCE_TYPE.POSTS)
            )
            if (msgResponse != null) {
                println("Message '$srcKey' sent to '$markdownQueue', message ID is '${msgResponse.messageId()}'")
            } else {
                println(error_NO_RESPONSE)
            }

        } catch (nske: NoSuchKeyException) {
            println("${nske.message} for key $srcKey")
            return ResponseEntity.notFound(body = APIResult.Error(message = "Could not find post with key '$srcKey'"))
        }
        return ResponseEntity.ok(body = APIResult.Success(value = "Regenerated post '$requestKey'"))
    }

    /**
     * Generate the HTML fragments for all the pages which match the given template.
     * Does not currently handle the '*' wildcard.
     */
    fun generateTemplate(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val requestKey = request.pathParameters["templateKey"]
        if (requestKey == "*") {
            return ResponseEntity.notImplemented(body = APIResult.Error("Regeneration of all templates is not supported."))
        }
        val templateKey = TEMPLATES_DIR + requestKey
        println("GeneratorController received request to regenerate pages based on template '$templateKey'")
        // first, get the pages structure file
        if (!s3Service.objectExists(pagesKey, sourceBucket)) {
            return ResponseEntity.notFound(body = APIResult.Error(message = "No pages.json file exists; there may be no pages defined or it may need regenerating."))
        }
        val pagesJson = s3Service.getObjectAsString(pagesKey, sourceBucket)
        var count = 0
        try {
            val pageList = Json.decodeFromString(PageList.serializer(), pagesJson)
            pageList.pages.filter { it.templateKey == requestKey }.forEach {
                println("Regenerating page ${it.srcKey} because it has template ${it.templateKey}")
                val pageSource = s3Service.getObjectAsString(it.srcKey,sourceBucket)
                queuePageRegeneration(it.srcKey,pageSource)
                count++
            }
            // then, get the pageSrcKeys for each page which uses this template
            // then send the markdown message to each of these pages
        } catch (se: SerializationException) {
            return ResponseEntity.serverError(body = APIResult.Error("Error processing pages.json; error is ${se.message}"))
        }
        return ResponseEntity.ok(APIResult.Success(value = "Regenerated $count files with the '$requestKey' template."))
    }

    /**
     * Send a message to the markdown queue for a Page
     */
    private fun queuePageRegeneration(pageSrcKey: String, sourceString: String) {
        // extract page model
        val pageModel = extractPageModel(pageSrcKey, sourceString)
        val msgResponse = sqsService.sendMessage(
            toQueue = markdownQueue,
            body = pageModel,
            messageAttributes = createStringAttribute("sourceType", SOURCE_TYPE.PAGES)
        )
        return if (msgResponse != null) {
            println("Message '$pageSrcKey' sent to '$markdownQueue', message ID is ${msgResponse.messageId()}'")
        } else {
            println(error_NO_RESPONSE)
        }
    }
}