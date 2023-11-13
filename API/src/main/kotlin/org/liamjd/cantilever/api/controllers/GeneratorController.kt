package org.liamjd.cantilever.api.controllers

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.*
import org.liamjd.cantilever.common.S3_KEY.pagesPrefix
import org.liamjd.cantilever.common.S3_KEY.postsPrefix
import org.liamjd.cantilever.models.ContentMetaDataBuilder
import org.liamjd.cantilever.models.ContentTree
import org.liamjd.cantilever.models.PageTreeNode
import org.liamjd.cantilever.models.sqs.SqsMsgBody
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import org.liamjd.cantilever.services.SQSService
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
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
                val pageModel = ContentMetaDataBuilder.PageBuilder.buildFromSourceString(sourceString, pageSrcKey)
                val msgBody = SqsMsgBody.PageReadyToRenderMsg(pageSrcKey, pageModel)
                val msgResponse = sqsService.sendMessage(
                    toQueue = markdownQueue,
                    body = msgBody,
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
            val postMetadata =
                ContentMetaDataBuilder.PostBuilder.buildFromSourceString(sourceString.getFrontMatter(), srcKey)
            val markdownBody = sourceString.stripFrontMatter()
            val message = SqsMsgBody.MarkdownPostUploadMsg(postMetadata, markdownBody)
            info("Built post metadata: $postMetadata")

            val msgResponse = sqsService.sendMessage(
                toQueue = markdownQueue,
                body = message,
                messageAttributes = createStringAttribute("sourceType", SOURCE_TYPE.Posts.folder)
            )
            if (msgResponse != null) {
                info("Message for post '$srcKey' sent to '$markdownQueue', message ID is '${msgResponse.messageId()}'")
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
        info("DOUBLE DECODED: GeneratorController received request to regenerate pages based on template '$templateKey'")
        val contentTreeJson = s3Service.getObjectAsString(S3_KEY.metadataKey, sourceBucket)
        val contentTree = Json.decodeFromString(ContentTree.serializer(), contentTreeJson)
        var count = 0

        // We don't know if the template is for a Page or a Post. This is less than ideal as I have to check both.
        try {
            contentTree.getPagesForTemplate(templateKey).forEach { page ->
                info("Regenerating page ${page.srcKey} because it has template ${page.templateKey}")
                val pageSource = s3Service.getObjectAsString(page.srcKey, sourceBucket)
                queuePageRegeneration(page.srcKey, pageSource)
                count++
            }

            contentTree.getPostsForTemplate(templateKey).forEach { post ->
                info("Regenerating post ${post.srcKey} because it has template ${post.templateKey}")
                val postSource = s3Service.getObjectAsString(post.srcKey, sourceBucket)
                queuePostRegeneration(post.srcKey, postSource)
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

    /**
     * Recursively loop through the page tree, searching for PageMeta nodes which match the specified templateKey
     * @param folderNode collection of nodes
     * @param templateKey the page template key
     * @param count current count of files sent for rendering
     * @return updated count of files sent for rendering
     */
    @Deprecated("Use PageTree.scanPageTree instead")
    private fun scanPageTree(
        folderNode: PageTreeNode.FolderNode,
        templateKey: String,
        count: Int
    ): Int {
        var localCount = count
        folderNode.children?.forEach { node ->
            when (node) {
                is PageTreeNode.PageMeta -> {
                    if (node.templateKey == templateKey) {
                        info("Regenerating page ${node.srcKey} because it has template '${node.templateKey}'")
                        val pageSource = s3Service.getObjectAsString(node.srcKey, sourceBucket)
                        queuePageRegeneration(node.srcKey, pageSource)
                        localCount++
                    }
                }

                is PageTreeNode.FolderNode -> {
                    localCount += scanPageTree(node, templateKey, localCount)
                }
            }
        }
        // TODO: this count is wrong
        return localCount
    }

    // TODO: WE'VE DONE THIS TWICE NOW
    /**
     * Send a message to the markdown queue for a Page
     */
    private fun queuePageRegeneration(pageSrcKey: String, sourceString: String) {
        // extract page model
        val pageMode = ContentMetaDataBuilder.PageBuilder.buildFromSourceString(sourceString, pageSrcKey)

        // TODO: THIS IS THE WRONG MESSAGE TYPE!
        val msgBody = SqsMsgBody.PageReadyToRenderMsg(pageSrcKey, pageMode)
        val msgResponse = sqsService.sendMessage(
            toQueue = markdownQueue,
            body = msgBody,
            messageAttributes = createStringAttribute("sourceType", SOURCE_TYPE.Pages.folder)
        )
        return if (msgResponse != null) {
            info("Message for page '$pageSrcKey' sent to '$markdownQueue', message ID is ${msgResponse.messageId()}'")
        } else {
            error(error_NO_RESPONSE)
        }
    }

    /**
     * Send a message to the markdown queue for a Post
     */
    private fun queuePostRegeneration(postSrcKey: String, sourceString: String) {
        // extract post model
        val metadata =
            ContentMetaDataBuilder.PostBuilder.buildFromSourceString(sourceString.getFrontMatter(), postSrcKey)
        // extract body
        val markdownBody = sourceString.stripFrontMatter()

        val message = SqsMsgBody.MarkdownPostUploadMsg(metadata, markdownBody)
        val msgResponse = sqsService.sendMessage(
            toQueue = markdownQueue,
            body = message,
            messageAttributes = createStringAttribute("sourceType", SOURCE_TYPE.Posts.folder)
        )
        return if (msgResponse != null) {
            info("Message for post '$postSrcKey' sent to '$markdownQueue', message ID is ${msgResponse.messageId()}'")
        } else {
            error(error_NO_RESPONSE)
        }
    }

    override fun info(message: String) = println("INFO: GeneratorController: $message")
    override fun warn(message: String) = println("WARN: GeneratorController: $message")
    override fun error(message: String) = println("ERROR: GeneratorController: $message")

}
