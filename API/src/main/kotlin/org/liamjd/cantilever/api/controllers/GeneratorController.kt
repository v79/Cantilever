package org.liamjd.cantilever.api.controllers

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.*
import org.liamjd.cantilever.common.S3_KEY.pagesKey
import org.liamjd.cantilever.common.S3_KEY.pagesPrefix
import org.liamjd.cantilever.common.S3_KEY.postsKey
import org.liamjd.cantilever.common.S3_KEY.postsPrefix
import org.liamjd.cantilever.common.S3_KEY.templatesPrefix
import org.liamjd.cantilever.models.PageTree
import org.liamjd.cantilever.models.PageTreeNode
import org.liamjd.cantilever.models.PostList
import org.liamjd.cantilever.models.sqs.SqsMsgBody
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.SQSService
import org.liamjd.cantilever.services.impl.extractPageModel
import org.liamjd.cantilever.services.impl.extractPostMetadata
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import java.net.URLDecoder
import java.nio.charset.Charset

/**
 * Handle routes relating to document generation. Mostly this will be done by sending messages to the appropriate queues.
 */
class GeneratorController(val sourceBucket: String) : KoinComponent, APIController {

    companion object {
        const val error_NO_RESPONSE = "No response received for message"
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
            warn("Wow, that's a big request")

            // get every page in the pages folder
            val pageListResponse = s3Service.listObjects(pagesPrefix, sourceBucket)
            info("There are ${pageListResponse.keyCount()} potential pages to process")
            var count = 0
            pageListResponse.contents().filter { it.key().endsWith(FILE_TYPE.MD) }.forEach { obj ->
                val sourceString = s3Service.getObjectAsString(obj.key(), sourceBucket)
                val pageSrcKey =
                    obj.key().removePrefix(pagesPrefix) // just want the actual file name
                // extract page model
                val pageModel = extractPageModel(pageSrcKey, sourceString)
                val msgResponse = sqsService.sendMessage(
                    toQueue = markdownQueue,
                    body = pageModel,
                    messageAttributes = createStringAttribute("sourceType", SOURCE_TYPE.Pages.folder)
                )

                if (msgResponse != null) {
                    count++
                    info("Message '${obj.key()}' sent to '$markdownQueue', message ID is ${msgResponse.messageId()}'")
                } else {
                    error(error_NO_RESPONSE)
                }
            }

            return ResponseEntity.ok(body = APIResult.Success(value = "$count pages have been regenerated"))
        } else {
            val srcKey = pagesPrefix + requestKey
            info("GeneratorController: Received request to regenerate page $srcKey")
            try {
                info("Received page file $srcKey and sending it to Markdown processor queue")
                val sourceString = s3Service.getObjectAsString(srcKey, sourceBucket)
                val pageSrcKey = srcKey.removePrefix(
                    pagesPrefix
                ) // just want the actual file name
                queuePageRegeneration(pageSrcKey, sourceString)
            } catch (nske: NoSuchKeyException) {
                error("${nske.message} for key $srcKey")
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
        val srcKey = postsPrefix + requestKey
        info("GeneratorController: Received request to regenerate post '$srcKey'")
        try {
            val sourceString = s3Service.getObjectAsString(srcKey, sourceBucket)
            val postSrcKey = srcKey.removePrefix(postsPrefix)
            val postMetadata = extractPostMetadata(postSrcKey, sourceString)
            val markdownBody = sourceString.stripFrontMatter()
            val message = SqsMsgBody.MarkdownPostUploadMsg(postMetadata, markdownBody)
            info("Built post metadata: $postMetadata")

            val msgResponse = sqsService.sendMessage(
                toQueue = markdownQueue,
                body = message,
                messageAttributes = createStringAttribute("sourceType", SOURCE_TYPE.Posts.folder)
            )
            if (msgResponse != null) {
                info("Message '$srcKey' sent to '$markdownQueue', message ID is '${msgResponse.messageId()}'")
            } else {
                println(error_NO_RESPONSE)
            }

        } catch (nske: NoSuchKeyException) {
            error("${nske.message} for key $srcKey")
            return ResponseEntity.notFound(body = APIResult.Error(message = "Could not find post with key '$srcKey'"))
        }
        return ResponseEntity.ok(body = APIResult.Success(value = "Regenerated post '$requestKey'"))
    }

    /**
     * Generate the HTML fragments for all the pages or posts which match the given template.
     * Does not currently handle the '*' wildcard.
     */
    fun generateTemplate(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val requestKey = request.pathParameters["templateKey"]
        if (requestKey == "*") {
            return ResponseEntity.notImplemented(body = APIResult.Error("Regeneration of all templates is not supported."))
        }
        println("ENCODED: GeneratorController received request to regenerate pages based on template '$requestKey'")
        // TODO: https://github.com/v79/Cantilever/issues/26 this only works for HTML handlebars templates, i.e. those whose file names end in ".index.html" in the "templates" folder.
        // Also, annoying that I have to double-decode this.
        val templateKey =
            URLDecoder.decode(URLDecoder.decode(requestKey, Charset.defaultCharset()), Charset.defaultCharset())
                .substringBefore(".").substringAfterLast("/")
        info("DOUBLE DECODED: GeneratorController received request to regenerate pages based on template '$templateKey'")
        // first, get the pages and posts structure file
        if (!s3Service.objectExists(pagesKey, sourceBucket)) {
            error("GeneratorController: No pages.json exists.")
            return ResponseEntity.notFound(body = APIResult.Error(message = "No pages.json file exists; there may be no pages defined or it may need regenerating."))
        }
        if (!s3Service.objectExists(postsKey, sourceBucket)) {
            error("GeneratorController: No posts.json exists.")
            return ResponseEntity.notFound(body = APIResult.Error(message = "No posts.json file exists; there may be no posts defined or it may need regenerating."))
        }
        val pagesJson = s3Service.getObjectAsString(pagesKey, sourceBucket)
        val postsJson = s3Service.getObjectAsString(postsKey, sourceBucket)
        var count = 0

        // TODO: we don't know if the template is for a Page or a Post. This is less than ideal as I have to check both.
        try {
            val oageTree = Json.decodeFromString(PageTree.serializer(), pagesJson)
            info("Checking the ${oageTree.container.count} pages for a template match to $templateKey")
            oageTree.container.children?.filterIsInstance<PageTreeNode.PageMeta>()?.filter { it.templateKey == templateKey }
                ?.forEach {
                    info("Regenerating page ${it.srcKey} because it has template ${it.templateKey}")
                    val pageSource = s3Service.getObjectAsString(it.srcKey, sourceBucket)
                    queuePageRegeneration(it.srcKey, pageSource)
                    count++
                }

            // TODO: again with the inconsistent naming of templates!
            val postList = Json.decodeFromString(PostList.serializer(), postsJson)
            info("Checking the ${postList.count} posts for a template match to $templateKey")
            postList.posts.filter { it.templateKey == "${templatesPrefix}${templateKey}.html.hbs" }.forEach {
                info("Regenerating post ${it.srcKey} because it has template ${it.templateKey}")
                val postSource = s3Service.getObjectAsString(it.srcKey, sourceBucket)
                queuePostRegeneration(it.srcKey, postSource)
                count++
            }
        } catch (se: SerializationException) {
            error("Error processing pages.json; error is ${se.message}")
            return ResponseEntity.serverError(body = APIResult.Error("Error processing pages.json; error is ${se.message}"))
        }

        return if (count == 0) {
            ResponseEntity.ok(APIResult.Success(value = "No pages or posts with the template '$templateKey' were suitable for regeneration."))
        } else {
            ResponseEntity.accepted(APIResult.Success(value = "Queued $count files with the '$templateKey' template for regeneration."))
        }
    }

    // TODO: WE'VE DONE THIS TWICE NOW
    /**
     * Send a message to the markdown queue for a Page
     */
    private fun queuePageRegeneration(pageSrcKey: String, sourceString: String) {
        // extract page model
        val pageModel = extractPageModel(pageSrcKey, sourceString)
        val msgResponse = sqsService.sendMessage(
            toQueue = markdownQueue,
            body = pageModel,
            messageAttributes = createStringAttribute("sourceType", SOURCE_TYPE.Pages.folder)
        )
        return if (msgResponse != null) {
            info("Message '$pageSrcKey' sent to '$markdownQueue', message ID is ${msgResponse.messageId()}'")
        } else {
            error(error_NO_RESPONSE)
        }
    }

    /**
     * Send a message to the markdown queue for a Post
     */
    private fun queuePostRegeneration(postSrcKey: String, sourceString: String) {
        // extract post model
        val metadata = extractPostMetadata(filename = postSrcKey, source = sourceString)
        // extract body
        val markdownBody = sourceString.stripFrontMatter()

        val message = SqsMsgBody.MarkdownPostUploadMsg(metadata, markdownBody)
        sqsService.sendMessage(
            toQueue = markdownQueue,
            body = message,
            messageAttributes = createStringAttribute("sourceType", SOURCE_TYPE.Posts.folder)
        )
    }

    override fun info(message: String) = println("INFO: GeneratorController: $message")
    override fun warn(message: String) = println("WARN: GeneratorController: $message")
    override fun error(message: String) = println("ERROR: GeneratorController: $message")

}
