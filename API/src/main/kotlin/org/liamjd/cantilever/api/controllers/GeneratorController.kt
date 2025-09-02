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

        return runBlocking {
            try {
                if (requestKey == "*") {
                    info("GeneratorController: Received request to regenerate all pages")
                    val pages = dynamoDBService.listAllNodesForProject(domain, SOURCE_TYPE.Pages)
                        .filterIsInstance<ContentNode.PageNode>()
                    var count = 0
                    if (pages.isNotEmpty()) {
                        pages.forEach { page ->
                            val sourceString = s3Service.getObjectAsString(page.srcKey, sourceBucket)
                            val msgResponse = queuePageRegeneration(
                                pageSrcKey = page.srcKey,
                                sourceString = sourceString,
                                projectDomain = domain
                            )
                            if (msgResponse != null) {
                                count++
                            } else {
                                error("No response when queueing page ${page.srcKey}")
                            }
                        }
                        info("Queued $count pages for regeneration")
                        Response.ok(body = APIResult.Success(value = "Queued $count pages for regeneration"))
                    } else {
                        Response.noContent(APIResult.Error("No pages exist to be regenerated"))
                    }
                } else {
                    srcKey = URLDecoder.decode(requestKey, Charset.defaultCharset())
                    info("GeneratorController: Received request to regenerate page '$srcKey'")
                    val sourceString = s3Service.getObjectAsString(srcKey, sourceBucket)
                    queuePageRegeneration(domain, srcKey, sourceString)
                    Response.ok(body = APIResult.Success(value = "Regenerated page '$requestKey'"))
                }
            } catch (nske: NoSuchKeyException) {
                error("${nske.message} for key $srcKey")
                Response.notFound(body = APIResult.Error(statusText = "Could not find page with key '$srcKey'"))
            } catch (e: Exception) {
                error("Error generating page: ${e.message}")
                Response.serverError(body = APIResult.Error("Error generating page: ${e.message}"))
            }
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
            ?: return Response.badRequest(body = APIResult.Error("No srcKey provided"))
        val domain = request.headers["cantilever-project-domain"]!!

        return runBlocking {
            try {
                if (requestKey == "*") {
                    info("GeneratorController: Received request to regenerate all posts")
                    val posts = dynamoDBService.listAllNodesForProject(domain, SOURCE_TYPE.Posts)
                        .filterIsInstance<ContentNode.PostNode>()
                    var count = 0
                    if (posts.isNotEmpty()) {
                        posts.forEach { post ->
                            val sourceString = s3Service.getObjectAsString(post.srcKey, sourceBucket)
                            val msgResponse = queuePostRegeneration(
                                postSrcKey = post.srcKey,
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
                        Response.ok(body = APIResult.Success(value = "Queued $count posts for regeneration"))

                    } else {
                        Response.noContent(APIResult.Error("No posts exist to be regenerated"))
                    }
                } else {
                    val srcKey = URLDecoder.decode(requestKey, Charset.defaultCharset())
                    info("GeneratorController: Received request to regenerate post '$srcKey'")
                    val sourceString = s3Service.getObjectAsString(srcKey, sourceBucket)
                    queuePostRegeneration(
                        postSrcKey = srcKey,
                        sourceString = sourceString,
                        projectDomain = domain
                    )
                    Response.ok(body = APIResult.Success(value = "Regenerated post '$srcKey'"))

                }
            } catch (nske: NoSuchKeyException) {
                error("${nske.message} for key $requestKey")
                Response.notFound(body = APIResult.Error(statusText = "Could not find post with key '$requestKey'"))
            } catch (e: Exception) {
                error("Error generating post: ${e.javaClass}: ${e.message}")
                Response.serverError(body = APIResult.Error("Error generating post: ${e.message}"))
            }
        }
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
        val decoded =
            URLDecoder.decode(URLDecoder.decode(requestKey, Charset.defaultCharset()), Charset.defaultCharset())
        val templateKey = decoded.removePrefix("${domain}/")
        info(
            "Received request to regenerate pages or posts based on template '$templateKey'"
        )

        return runBlocking {
            try {
                // We don't know if the template is for a Page or a Post. This is less than ideal as I have to check both. But I could short-circuit the second check if the first one succeeds?
                val postSrcKeys = dynamoDBService.getKeyListMatchingTemplate(domain, SOURCE_TYPE.Posts, templateKey)
                var count = 0
                postSrcKeys.forEach { srcKey ->
                    val node = dynamoDBService.getContentNode(srcKey, domain, SOURCE_TYPE.Posts)
                    if (node is ContentNode.PostNode) {
                        info("Regenerating post ${node.srcKey} because it has template ${node.templateKey}")
                        val pageSource = s3Service.getObjectAsString(node.srcKey, sourceBucket)
                        val response = queuePostRegeneration(
                            postSrcKey = node.srcKey,
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
                val pageSourceKeys =
                    dynamoDBService.getKeyListMatchingTemplate(domain, SOURCE_TYPE.Pages, templateKey)

                pageSourceKeys.forEach { srcKey ->
                    val node = dynamoDBService.getContentNode(srcKey, domain, SOURCE_TYPE.Pages)
                    if (node is ContentNode.PageNode) {
                        info("Regenerating page ${node.srcKey} because it has template ${node.templateKey}")
                        val pageSource = s3Service.getObjectAsString(node.srcKey, sourceBucket)
                        val response = queuePageRegeneration(
                            pageSrcKey = node.srcKey,
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
                if (count == 0) {
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

            } catch (se: SerializationException) {
                error("Error processing pages.json; error is ${se.message}")
                Response.serverError(
                    body = APIResult.Error("Error processing pages.json; error is ${se.message}")
                )
            }
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
     * TODO: I haven't linked images with pages or posts in DynamoDB yet.
     * TODO: This function won't behave as there is no contentTree to check with
     * The generation bucket will accumulate a lot of files over time. This method will clear out generated image resolutions which are no longer referenced in the database
     */
    fun clearGeneratedImages(request: Request<Unit>): Response<APIResult<String>> {
        val domain = request.headers["cantilever-project-domain"]!!
        info("Received request to clear generated images from folder $domain/images/")
        val listResponse = s3Service.listObjects("$domain/generated/images/", generationBucket)
        var count = 0
        val deleteList = mutableListOf<String>()
        if (listResponse.hasContents()) {
            // this will not respond with any particular order. I'll need to iterate twice?
            deleteList.addAll(listResponse.contents().map { it.key().removePrefix("$domain/sources/") })
            listResponse.contents().forEach { obj ->
                //* check if the image is still referenced in database
                // the problem is we are iterating over the objects in the bucket, not the database file.
                // this iteration will include both the original image and the thumbnails */

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
            body = APIResult.Success("Deleted $count generated images from folder $domain/images/")
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
