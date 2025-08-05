package org.liamjd.cantilever.api.controllers

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.koin.core.component.KoinComponent
import org.liamjd.apiviaduct.routing.Request
import org.liamjd.apiviaduct.routing.Response
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.SOURCE_TYPE
import org.liamjd.cantilever.common.getFrontMatter
import org.liamjd.cantilever.models.ContentMetaDataBuilder
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.rest.PostListDTO
import org.liamjd.cantilever.models.rest.PostNodeRestDTO
import java.net.URLDecoder
import java.nio.charset.Charset

/**
 * Load, save and delete Posts from the S3 bucket. Operations will update the content tree.
 */
class PostController(sourceBucket: String, generationBucket: String) : KoinComponent,
    APIController(sourceBucket, generationBucket) {

    /**
     * Load a Markdown file with the specified `srcKey` from the project folder `cantilever-project-domain` and return it as [ContentNode.PostNode] response
     */
    fun loadMarkdownSource(request: Request<Unit>): Response<APIResult<ContentNode.PostNode>> {
        val markdownSource = request.pathParameters["srcKey"]
        val projectKeyHeader = request.headers["cantilever-project-domain"]!!
        return if (markdownSource != null) {
            val srcKey = URLDecoder.decode(markdownSource, Charset.defaultCharset())
            info("Loading Markdown file $srcKey")
            return if (s3Service.objectExists(srcKey, sourceBucket)) {
                val mdPost = buildPostNode(srcKey, srcKey)
                Response.ok(body = APIResult.Success(mdPost))
            } else {
                error("File '$srcKey' not found")
                Response.notFound(body = APIResult.Error("Markdown file $srcKey not found in bucket $sourceBucket"))
            }
        } else {
            Response.badRequest(body = APIResult.Error("Invalid request for null source file"))
        }
    }

    /**
     * Receive a [PostNodeRestDTO] and convert it to a [ContentNode.PostNode] and save it to the S3 bucket
     */
    fun saveMarkdownPost(request: Request<PostNodeRestDTO>): Response<APIResult<String>> {
        info("saveMarkdownPost")
        val postToSave = request.body
        val srcKey = postToSave.srcKey
        val projectKeyHeader = request.headers["cantilever-project-domain"]!!
        return if (s3Service.objectExists(srcKey, sourceBucket)) {
            loadContentTree(projectKeyHeader)
            info("Updating existing file '${srcKey}'")
            val length = s3Service.putObjectAsString(srcKey, sourceBucket, postToSave.toString(), "text/markdown")
            contentTree.updatePost(postToSave.toPostNode())
            Response.ok(body = APIResult.OK("Updated file $srcKey, $length bytes"))
        } else {
            info("Creating new file...")
            val length = s3Service.putObjectAsString(srcKey, sourceBucket, postToSave.toString(), "text/markdown")
            contentTree.insertPost(postToSave.toPostNode())
            Response.ok(body = APIResult.OK("Saved new file $srcKey, $length bytes"))
        }
    }

    /**
     * Delete the Markdown post... and it's corresponding HTML?
     */
    fun deleteMarkdownPost(request: Request<Unit>): Response<APIResult<String>> {
        val markdownSource = request.pathParameters["srcKey"]
        val projectKeyHeader = request.headers["cantilever-project-domain"]!!
        return if (markdownSource != null) {
            loadContentTree(projectKeyHeader)
            val decoded = URLDecoder.decode(markdownSource, Charset.defaultCharset())
            return if (s3Service.objectExists(decoded, sourceBucket)) {
                val postNode = contentTree.getNode(decoded)
                if (postNode != null && postNode is ContentNode.PostNode) {
                    info("Deleting markdown file $decoded")
                    s3Service.deleteObject(decoded, sourceBucket)
                    contentTree.deletePost(postNode)
                    saveContentTree(projectKeyHeader)
                    Response.ok(body = APIResult.OK("Source $decoded deleted"))
                } else {
                    error("Could not delete $decoded; object not found or was not a PostNode")
                    Response.ok(body = APIResult.Error("Could not delete $decoded; object not found or was not a Post"))
                }
            } else {
                error("Could not delete $decoded; object not found")
                Response.ok(body = APIResult.Error("Could not delete $decoded; object not found"))
            }
        } else {
            error("Could not delete null markdownSource")
            Response.ok(body = APIResult.Error("Could not delete null markdownSource"))
        }
    }

    /**
     * Return a list of all the posts in the content tree
     * [Request.headers] must contain a "cantilever-project-domain" header
     * @return [PostListDTO] object containing the list of posts, a count and the last updated date/time
     */
    fun getPosts(request: Request<Unit>): Response<APIResult<PostListDTO>> {
        val projectKeyHeader = request.headers["cantilever-project-domain"]!!
        info("Retrieving posts for project $projectKeyHeader")
        return runBlocking {
            val list = dynamoDBService.listAllNodesForProject(projectKeyHeader, SOURCE_TYPE.Posts)
            val postList = list.filterIsInstance<ContentNode.PostNode>()
            if (postList.isEmpty()) {
                error("No posts found in DynamoDB for project $projectKeyHeader")
                return@runBlocking Response.notFound(
                    body = APIResult.Error("No posts found in DynamoDB for project $projectKeyHeader")
                )
            }
            val sorted = postList.sortedByDescending { it.date }
            val lastUpdated = sorted.last().date
            val dateTime = lastUpdated.atStartOfDayIn(TimeZone.currentSystemDefault())
            val postListDTO = PostListDTO(
                lastUpdated = dateTime,
                posts = sorted,
                count = sorted.size,
            )
            return@runBlocking Response.ok(body = APIResult.Success(value = postListDTO))
        }
    }

    /**
     * Build a [ContentNode.PostNode] object from the source specified, and add the full body text
     */
    private fun buildPostNode(
        fullPathKey: String,
        srcKey: String
    ): ContentNode.PostNode {
        val markdown = s3Service.getObjectAsString(fullPathKey, sourceBucket)
        val metadata = ContentMetaDataBuilder.PostBuilder.buildFromSourceString(markdown.getFrontMatter(), srcKey)
        val body = markdown.substringAfter("---").substringAfter("---").trim()
        return metadata.apply { this.body = body }
    }

    override fun info(message: String) = println("INFO: PostController: $message")
    override fun warn(message: String) = println("WARN: PostController: $message")
    override fun error(message: String) = println("ERROR: PostController: $message")
}
