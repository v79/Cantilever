package org.liamjd.cantilever.api.controllers

import kotlinx.serialization.SerializationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.*
import org.liamjd.cantilever.common.S3_KEY.postsPrefix
import org.liamjd.cantilever.models.ContentMetaDataBuilder
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.sqs.MarkdownSQSMessage
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import org.liamjd.cantilever.services.SQSService
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import java.net.URLDecoder
import java.nio.charset.Charset

/**
 * Handle routes relating to document generation. Mostly this will be done by sending messages to the appropriate queues.
 */
class GeneratorController(sourceBucket: String) : KoinComponent, APIController(sourceBucket) {

    companion object {
        const val error_NO_RESPONSE = "No response received for message"
    }

    private val sqsService: SQSService by inject()
    private val markdownQueue: String = System.getenv(QUEUE.MARKDOWN) ?: "markdown_processing_queue"

    /**
     * PUT /generate/page/{srcKey}
     * Generate the HTML version of the page specified by the path parameter 'srcKey'.
     * The actual path searched for will be `/sources/pages/<srcKey>'.
     * This method will send a message to the markdown processing queue in SQS.
     * If <srcKey> is '*' it will trigger regeneration of all source markdown pages
     */
    fun generatePage(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val requestKey = request.pathParameters["srcKey"]
            ?: return ResponseEntity.badRequest(body = APIResult.Error("No srcKey provided"))
        var srcKey = ""
        try {
            loadContentTree()
            if (requestKey == "*") {
                info("GeneratorController: Received request to regenerate all pages")
                val pages = contentTree.items.filterIsInstance<ContentNode.PageNode>()
                var count = 0
                if (pages.isNotEmpty()) {
                    pages.forEach { page ->
                        val sourceString = s3Service.getObjectAsString(page.srcKey, sourceBucket)
                        val msgResponse = queuePageRegeneration(page.srcKey, sourceString)
                        if (msgResponse != null) {
                            count++
                        } else {
                            error("No response when queueing page ${page.srcKey}")
                        }
                    }
                    info("Queued $count pages for regeneration")
                    return ResponseEntity.ok(body = APIResult.Success(value = "Queued $count pages for regeneration"))
                } else {
                    return ResponseEntity.notFound(body = APIResult.Error("No pages found to regenerate"))
                }
            } else {
                srcKey = URLDecoder.decode(requestKey, Charset.defaultCharset())
                info("GeneratorController: Received request to regenerate page '$srcKey'")
                val sourceString = s3Service.getObjectAsString(srcKey, sourceBucket)
                queuePageRegeneration(srcKey, sourceString)
                return ResponseEntity.ok(body = APIResult.Success(value = "Regenerated page '$requestKey'"))
            }
        } catch (nske: NoSuchKeyException) {
            error("${nske.message} for key $srcKey")
            return ResponseEntity.notFound(body = APIResult.Error(message = "Could not find page with key '$srcKey'"))
        } catch (e: Exception) {
            error("Error generating page: ${e.message}")
            return ResponseEntity.serverError(body = APIResult.Error("Error generating page: ${e.message}"))
        }
    }

    /**
     * PUT /generate/post/{srcKey}
     * Generate the HTML fragments of the post specified by the path parameter 'srcKey'.
     * The actual path searched for will be `/sources/posts/<srcKey>`.
     * This method will send a message to the markdown processing queue in SQS.
     */
    fun generatePost(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val requestKey = request.pathParameters["srcKey"]
        var srcKey = ""
        try {
            loadContentTree()
            if (requestKey == "*") {
                info("GeneratorController: Received request to regenerate all posts")
                val posts = contentTree.items.filterIsInstance<ContentNode.PostNode>()
                var count = 0
                if (posts.isNotEmpty()) {
                    posts.forEach { post ->
                        val sourceString = s3Service.getObjectAsString(post.srcKey, sourceBucket)
                        val postSrcKey = post.srcKey.removePrefix(postsPrefix)
                        val msgResponse = queuePostRegeneration(postSrcKey, sourceString)
                        if (msgResponse != null) {
                            count++
                        } else {
                            error("No response when queueing post ${post.srcKey}")
                        }
                    }
                    info("Queued $count posts for regeneration")
                    return ResponseEntity.ok(body = APIResult.Success(value = "Queued $count posts for regeneration"))
                } else {
                    return ResponseEntity.notFound(body = APIResult.Error("No posts found to regenerate"))
                }
            } else {
                srcKey = postsPrefix + requestKey
                info("GeneratorController: Received request to regenerate post '$srcKey'")
                val sourceString = s3Service.getObjectAsString(srcKey, sourceBucket)
                val postSrcKey = srcKey.removePrefix(postsPrefix)
                queuePostRegeneration(postSrcKey, sourceString)
                return ResponseEntity.ok(body = APIResult.Success(value = "Regenerated post '$requestKey'"))
            }
        } catch (nske: NoSuchKeyException) {
            error("${nske.message} for key $srcKey")
            return ResponseEntity.notFound(body = APIResult.Error(message = "Could not find post with key '$srcKey'"))
        } catch (e: Exception) {
            return ResponseEntity.serverError(body = APIResult.Error("Error generating post: ${e.message}"))
        }
    }

    /**
     * PUT /generate/template/{templateKey}
     * Generate the HTML fragments for all the pages or posts which match the given template.
     * Does not currently handle the '*' wildcard.
     */
    fun generateTemplate(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val requestKey = request.pathParameters["templateKey"]
        if (requestKey == "*") {
            return ResponseEntity.notImplemented(
                body = APIResult.Error("Regeneration of all templates is not supported.")
            )
        }
        info("ENCODED: GeneratorController received request to regenerate pages based on template '$requestKey'")
        // TODO: https://github.com/v79/Cantilever/issues/26 this only works for HTML handlebars templates, i.e. those whose file names end in ".index.html" in the "templates" folder.
        // Also, annoying that I have to double-decode this.
        val templateKey =
            URLDecoder.decode(URLDecoder.decode(requestKey, Charset.defaultCharset()), Charset.defaultCharset())
        info(
            "DOUBLE DECODED: GeneratorController received request to regenerate pages based on template '$templateKey'"
        )
        var count = 0

        // We don't know if the template is for a Page or a Post. This is less than ideal as I have to check both. But I could short-circuit the second check if the first one succeeds?
        try {
            loadContentTree()
            contentTree.getPagesForTemplate(templateKey).forEach { page ->
                info("Regenerating page ${page.srcKey} because it has template ${page.templateKey}")
                val pageSource = s3Service.getObjectAsString(page.srcKey, sourceBucket)
                val response = queuePageRegeneration(page.srcKey, pageSource)
                if (response != null) {
                    count++
                } else {
                    error(error_NO_RESPONSE)
                }
            }

            contentTree.getPostsForTemplate(templateKey).forEach { post ->
                info("Regenerating post ${post.srcKey} because it has template ${post.templateKey}")
                val postSource = s3Service.getObjectAsString(post.srcKey, sourceBucket)
                val msgResponse = queuePostRegeneration(post.srcKey, postSource)
                if (msgResponse != null) {
                    count++
                } else {
                    error(error_NO_RESPONSE)
                }
            }
        } catch (se: SerializationException) {
            error("Error processing pages.json; error is ${se.message}")
            return ResponseEntity.serverError(
                body = APIResult.Error("Error processing pages.json; error is ${se.message}")
            )
        }

        return if (count == 0) {
            ResponseEntity.ok(
                APIResult.Success(
                    value = "No pages or posts with the template '$templateKey' were suitable for regeneration."
                )
            )
        } else {
            ResponseEntity.accepted(
                APIResult.Success(value = "Queued $count files with the '$templateKey' template for regeneration.")
            )
        }
    }

    // TODO: WE'VE DONE THIS TWICE NOW
    /**
     * Send a message to the markdown queue for a Page
     */
    private fun queuePageRegeneration(pageSrcKey: String, sourceString: String): SendMessageResponse? {
        // extract page model
        val pageMode = ContentMetaDataBuilder.PageBuilder.buildFromSourceString(sourceString, pageSrcKey)
        val msgBody = MarkdownSQSMessage.PageUploadMsg(pageMode, sourceString)
        return sqsService.sendMarkdownMessage(
            toQueue = markdownQueue, body = msgBody,
            messageAttributes = createStringAttribute("sourceType", SOURCE_TYPE.Pages.folder)
        )
    }

    /**
     * Send a message to the markdown queue for a Post
     */
    private fun queuePostRegeneration(postSrcKey: String, sourceString: String): SendMessageResponse? {
        // extract post model
        val metadata =
            ContentMetaDataBuilder.PostBuilder.buildFromSourceString(sourceString.getFrontMatter(), postSrcKey)
        // extract body
        val markdownBody = sourceString.stripFrontMatter()

        val msgBody = MarkdownSQSMessage.PostUploadMsg(metadata, markdownBody)
        return sqsService.sendMarkdownMessage(
            toQueue = markdownQueue, body = msgBody,
            messageAttributes = createStringAttribute("sourceType", SOURCE_TYPE.Posts.folder)
        )
    }

    override fun info(message: String) = println("INFO: GeneratorController: $message")
    override fun warn(message: String) = println("WARN: GeneratorController: $message")
    override fun error(message: String) = println("ERROR: GeneratorController: $message")

}
