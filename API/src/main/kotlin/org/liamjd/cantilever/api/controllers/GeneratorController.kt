package org.liamjd.cantilever.api.controllers

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.apiviaduct.routing.Request
import org.liamjd.apiviaduct.routing.Response
import org.liamjd.apiviaduct.schema.OpenAPIPath
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.*
import org.liamjd.cantilever.common.S3_KEY.postsPrefix
import org.liamjd.cantilever.models.ContentMetaDataBuilder
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.sqs.MarkdownSQSMessage
import org.liamjd.cantilever.services.SQSService
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import java.net.URLDecoder
import java.nio.charset.Charset

/**
 * Handle routes relating to document generation. Mostly this will be done by sending messages to the appropriate queues.
 */
class GeneratorController(sourceBucket: String, generationBucket: String) : KoinComponent,
    APIController(sourceBucket, generationBucket) {

    companion object {
        const val error_NO_RESPONSE = "No response received for message"
    }

    private val sqsService: SQSService by inject()
    private val markdownQueue: String = System.getenv(QUEUE.MARKDOWN) ?: "markdown_processing_queue"

    /**
     * PUT /generate/page/{srcKey}
     * Generate the HTML version of the page specified by the path parameter 'srcKey'.
     * The actual path searched for will be `/sources/pages/<srcKey>'.
     * This method will send a message to the Markdown processing queue in SQS.
     * If <srcKey> is '*', it will trigger regeneration of all source Markdown pages
     */
    @OpenAPIPath
    fun generatePage(request: Request<Unit>): Response<APIResult<String>> {
        val requestKey = request.pathParameters["srcKey"]
            ?: return Response.badRequest(body = APIResult.Error("No srcKey provided"))
        var srcKey = ""
        val domain = request.headers["cantilever-project-domain"]!!
        try {
            if (requestKey == "*") {
                info("GeneratorController: Received request to regenerate all pages")
                val pages = contentTree.items.filterIsInstance<ContentNode.PageNode>()
                var count = 0
                if (pages.isNotEmpty()) {
                    pages.forEach { page ->
                        val sourceString = s3Service.getObjectAsString(page.srcKey, sourceBucket)
                        val msgResponse = queuePageRegeneration(domain, page.srcKey, sourceString)
                        if (msgResponse != null) {
                            count++
                        } else {
                            error("No response when queueing page ${page.srcKey}")
                        }
                    }
                    info("Queued $count pages for regeneration")
                    return Response.ok(body = APIResult.Success(value = "Queued $count pages for regeneration"))
                } else {
                    return Response.notFound(body = APIResult.Error("No pages found to regenerate"))
                }
            } else {
                srcKey = URLDecoder.decode(requestKey, Charset.defaultCharset())
                info("GeneratorController: Received request to regenerate page '$srcKey'")
                val sourceString = s3Service.getObjectAsString(srcKey, sourceBucket)
                queuePageRegeneration(domain, srcKey, sourceString)
                return Response.ok(body = APIResult.Success(value = "Regenerated page '$requestKey'"))
            }
        } catch (nske: NoSuchKeyException) {
            error("${nske.message} for key $srcKey")
            return Response.notFound(body = APIResult.Error(statusText = "Could not find page with key '$srcKey'"))
        } catch (e: Exception) {
            error("Error generating page: ${e.message}")
            return Response.serverError(body = APIResult.Error("Error generating page: ${e.message}"))
        }
    }

    /**
     * PUT /generate/post/{srcKey}
     * Generate the HTML fragments of the post specified by the path parameter 'srcKey'.
     * The actual path searched for will be `/sources/posts/<srcKey>`.
     * This method will send a message to the Markdown processing queue in SQS.
     */
    fun generatePost(request: Request<Unit>): Response<APIResult<String>> {
        val requestKey = request.pathParameters["srcKey"]
        var srcKey = ""
        val domain = request.headers["cantilever-project-domain"]!!
        try {
            runBlocking {
                if (requestKey == "*") {
                    info("GeneratorController: Received request to regenerate all posts")
                    val posts = dynamoDBService.listAllNodesForProject(domain, SOURCE_TYPE.Posts)
                        .filterIsInstance<ContentNode.PostNode>()
                    var count = 0
                    if (posts.isNotEmpty()) {
                        posts.forEach { post ->
                            val sourceString = s3Service.getObjectAsString(post.srcKey, sourceBucket)
                            val postSrcKey = post.srcKey.removePrefix(postsPrefix)
                            val msgResponse = queuePostRegeneration(
                                postSrcKey = postSrcKey,
                                sourceString = sourceString,
                                projectDomain = domain
                            )
                            if (msgResponse != null) {
                                count++
                            } else {
                                error("No response when queueing post ${post.srcKey}")
                            }
                        }
                        info("Queued $count posts for regeneration")
                        return@runBlocking Response.ok(body = APIResult.Success(value = "Queued $count posts for regeneration"))
                    } else {
                        return@runBlocking Response.notFound(body = APIResult.Error("No posts found to regenerate"))
                    }
                } else {
                    srcKey = postsPrefix + requestKey
                    info("GeneratorController: Received request to regenerate post '$srcKey'")
                    val sourceString = s3Service.getObjectAsString(srcKey, sourceBucket)
                    val postSrcKey = srcKey.removePrefix(postsPrefix)
                    queuePostRegeneration(
                        postSrcKey = postSrcKey,
                        sourceString = sourceString,
                        projectDomain = domain
                    )
                    return@runBlocking Response.ok(body = APIResult.Success(value = "Regenerated post '$requestKey'"))
                }
            }
        } catch (nske: NoSuchKeyException) {
            error("${nske.message} for key $srcKey")
            return Response.notFound(body = APIResult.Error(statusText = "Could not find post with key '$srcKey'"))
        } catch (e: Exception) {
            error("Error generating post: ${e.javaClass}: ${e.message}")
            return Response.serverError(body = APIResult.Error("Error generating post: ${e.message}"))
        }
        return Response.serverError(body = APIResult.Error("Unexpected error while generating posts"))
    }

    /**
     * PUT /generate/template/{templateKey}
     * Generate the HTML fragments for all the pages or posts which match the given template.
     * Does not currently handle the '*' wildcard.
     */
    fun generateTemplate(request: Request<Unit>): Response<APIResult<String>> {
        val requestKey = request.pathParameters["templateKey"]
        if (requestKey == "*") {
            return Response.notImplemented(
                body = APIResult.Error("Regeneration of all templates is not supported.")
            )
        }
        val domain = request.headers["cantilever-project-domain"]!!
        // TODO: https://github.com/v79/Cantilever/issues/26 this only works for HTML handlebars templates, i.e. those whose file names end in ".index.html" in the "templates" folder.
        // Also, annoying that I have to double-decode this.
        // And I need to strip off the domain from the requestKey
        val templateKey =
            URLDecoder.decode(URLDecoder.decode(requestKey, Charset.defaultCharset()), Charset.defaultCharset())
                .substringAfter(
                    "$domain/"
                )
        info(
            "Received request to regenerate pages based on template '$templateKey'"
        )
        var count = 0

        // We don't know if the template is for a Page or a Post. This is less than ideal as I have to check both. But I could short-circuit the second check if the first one succeeds?
        runBlocking {
            try {
                val pageSrcKeys = dynamoDBService.getKeyListMatchingTemplate(domain, SOURCE_TYPE.Pages, templateKey)
                // we only have a list of keys, not actual nodes
                pageSrcKeys.forEach { srcKey ->
                    val pageNode = dynamoDBService.getContentNode(srcKey, domain, SOURCE_TYPE.Pages)
                    if (pageNode is ContentNode.PageNode) {
                        info("Regenerating page ${pageNode.srcKey} because it has template ${pageNode.templateKey}")
                        val pageSource = s3Service.getObjectAsString(pageNode.srcKey, sourceBucket)
                        val response = queuePageRegeneration(
                            pageSrcKey = pageNode.srcKey,
                            sourceString = pageSource,
                            projectDomain = domain
                        )
                        if (response != null) {
                            count++
                        } else {
                            error(error_NO_RESPONSE)
                        }
                    }
                }


                contentTree.getPostsForTemplate(templateKey).forEach { post ->
                    info("Regenerating post ${post.srcKey} because it has template ${post.templateKey}")
                    val postSource = s3Service.getObjectAsString(post.srcKey, sourceBucket)
                    val msgResponse = queuePostRegeneration(
                        postSrcKey = post.srcKey,
                        sourceString = postSource,
                        projectDomain = domain
                    )
                    if (msgResponse != null) {
                        count++
                    } else {
                        error(error_NO_RESPONSE)
                    }
                }
            } catch (se: SerializationException) {
                error("Error processing pages.json; error is ${se.message}")
                return@runBlocking Response.serverError(
                    body = APIResult.Error("Error processing pages.json; error is ${se.message}")
                )
            }
        }

        return if (count == 0) {
            Response.ok(
                APIResult.Success(
                    value = "No pages or posts with the template '$templateKey' were suitable for regeneration."
                )
            )
        } else {
            Response.accepted(
                APIResult.Success(value = "Queued $count files with the '$templateKey' template for regeneration.")
            )
        }
    }

    /**
     * The generation bucket will accumulate a lot of files over time. This method will clear out all the generated fragments.
     */
    fun clearGeneratedFragments(request: Request<Unit>): Response<APIResult<String>> {
        val projectKeyHeader = request.headers["cantilever-project-domain"]!!

        info("Received request to clear generated fragments from folder $projectKeyHeader/generated/htmlFragments/")
        val listResponse = s3Service.listObjects("$projectKeyHeader/generated/htmlFragments/", generationBucket)
        var count = 0
        if (listResponse.hasContents()) {
            listResponse.contents().forEach { obj ->
                info("Deleting object ${obj.key()}")
                val delResponse = s3Service.deleteObject(obj.key(), generationBucket)
                if (delResponse != null) {
                    count++
                }
            }
        } else {
            return Response.ok(body = APIResult.Success("No generated fragments to clear"))
        }
        info("Deleted $count generated fragments from folder $projectKeyHeader/generated/htmlFragments/")
        return Response.ok(
            body = APIResult.Success("Deleted $count generated fragments from folder $projectKeyHeader/generated/htmlFragments/")
        )
    }

    /**
     * The generation bucket will accumulate a lot of files over time. This method will clear out generated image resolutions which are no longer referenced in metadata.json
     */
    fun clearGeneratedImages(request: Request<Unit>): Response<APIResult<String>> {
        val projectKeyHeader = request.headers["cantilever-project-domain"]!!

        info("Received request to clear generated images from folder $projectKeyHeader/images/")
        loadContentTree(projectKeyHeader)
        val listResponse = s3Service.listObjects("$projectKeyHeader/generated/images/", generationBucket)
        var count = 0
        val deleteList = mutableListOf<String>()
        if (listResponse.hasContents()) {
            // this will not respond with any particular order. I'll need to iterate twice?
            deleteList.addAll(listResponse.contents().map { it.key().removePrefix("$projectKeyHeader/sources/") })
            listResponse.contents().forEach { obj ->
                //* check if the image is still referenced in metadata.json
                // the problem is we are iterating over the objects in the bucket, not the metadata.json file.
                // this iteration will include both the original image and the thumbnails */
                if (contentTree.images.any {
                        it.srcKey.removePrefix("$projectKeyHeader/sources/") == obj.key()
                            .removePrefix("$projectKeyHeader/generated/")
                    }) {
                    info("Image ${obj.key()} is still referenced in metadata.json")
                    // We've found the image in the metadata, so we don't want to delete it.
                    // BUT we don't want to delete any of its image resolutions either.
                    // Put the image resolution keys into a 'do not delete' list?
                    deleteList.remove(obj.key())
                    s3Service.listObjects(obj.key(), generationBucket).contents()
                        .forEach { imageResolution ->
                            println("Removing ${imageResolution.key()} from delete list")
                            deleteList.remove(imageResolution.key())
                        }
                }
            }
            deleteList.forEach { key ->
                info("Deleting object $key")
                count++
                val delResponse = s3Service.deleteObject(key, generationBucket)
                if (delResponse != null) {
                    count++
                }
            }

        } else {
            return Response.ok(body = APIResult.Success("No generated images to clear"))
        }
        if (count == 0) {
            return Response.noContent(body = APIResult.Success("No generated images to clear"))
        }
        return Response.ok(
            body = APIResult.Success("Deleted $count generated images from folder $projectKeyHeader/images/")
        )
    }

    // TODO: WE'VE DONE THIS TWICE NOW
    /**
     * Send a message to the Markdown queue for a Page
     */
    private fun queuePageRegeneration(
        pageSrcKey: String,
        sourceString: String,
        projectDomain: String
    ): SendMessageResponse? {
        // extract the page model
        val pageMode = ContentMetaDataBuilder.PageBuilder.buildFromSourceString(sourceString, pageSrcKey)
        val msgBody = MarkdownSQSMessage.PageUploadMsg(
            projectDomain = projectDomain,
            metadata = pageMode,
            markdownText = sourceString
        )
        return sqsService.sendMarkdownMessage(
            toQueue = markdownQueue, body = msgBody,
            messageAttributes = createStringAttribute("sourceType", SOURCE_TYPE.Pages.folder)
        )
    }

    /**
     * Send a message to the Markdown queue for a Post
     */
    private fun queuePostRegeneration(
        postSrcKey: String,
        sourceString: String,
        projectDomain: String
    ): SendMessageResponse? {
        // extract the post model
        val metadata =
            ContentMetaDataBuilder.PostBuilder.buildFromSourceString(sourceString.getFrontMatter(), postSrcKey)
        // extract body
        val markdownBody = sourceString.stripFrontMatter()

        val msgBody = MarkdownSQSMessage.PostUploadMsg(
            projectDomain = projectDomain,
            metadata = metadata,
            markdownText = markdownBody
        )
        return sqsService.sendMarkdownMessage(
            toQueue = markdownQueue, body = msgBody,
            messageAttributes = createStringAttribute("sourceType", SOURCE_TYPE.Posts.folder)
        )
    }

    override fun info(message: String) = println("INFO: GeneratorController: $message")
    override fun warn(message: String) = println("WARN: GeneratorController: $message")
    override fun error(message: String) = println("ERROR: GeneratorController: $message")

}
