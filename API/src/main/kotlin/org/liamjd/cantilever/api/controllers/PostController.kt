package org.liamjd.cantilever.api.controllers

import org.koin.core.component.KoinComponent
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.common.getFrontMatter
import org.liamjd.cantilever.models.ContentMetaDataBuilder
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.rest.PostListDTO
import org.liamjd.cantilever.models.rest.PostNodeRestDTO
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import java.net.URLDecoder
import java.nio.charset.Charset

/**
 * Load, save and delete Posts from the S3 bucket. Operations will update the content tree.
 */
class PostController( sourceBucket: String) : KoinComponent, APIController(sourceBucket) {

    /**
     * Load a markdown file with the specified `srcKey` and return it as [ContentNode.PostNode] response
     */
    fun loadMarkdownSource(request: Request<Unit>): ResponseEntity<APIResult<ContentNode.PostNode>> {
        val markdownSource = request.pathParameters["srcKey"]
        if(request.headers["cantilever-project-domain"] === null) {
            error("Missing required header 'cantilever-project-domain'")
            return ResponseEntity.badRequest(body = APIResult.Error("Missing required header 'cantilever-project-domain'"))
        }
        val projectKeyHeader = request.headers["cantilever-project-domain"]!!
        return if (markdownSource != null) {
            val srcKey = projectKeyHeader + "/" + URLDecoder.decode(markdownSource, Charset.defaultCharset())
            info("Loading Markdown file $srcKey")
            return if (s3Service.objectExists(srcKey, sourceBucket)) {
                val mdPost = buildPostNode(srcKey)
                ResponseEntity.ok(body = APIResult.Success(mdPost))
            } else {
                error("File '$srcKey' not found")
                ResponseEntity.notFound(body = APIResult.Error("Markdown file $srcKey not found in bucket $sourceBucket"))
            }
        } else {
            ResponseEntity.badRequest(body = APIResult.Error("Invalid request for null source file"))
        }
    }

    /**
     * Receive a [PostNodeRestDTO] and convert it to a [ContentNode.PostNode] and save it to the S3 bucket
     */
    fun saveMarkdownPost(request: Request<PostNodeRestDTO>): ResponseEntity<APIResult<String>> {
        info("saveMarkdownPost")
        val postToSave = request.body
        if(request.headers["cantilever-project-domain"] === null) {
            error("Missing required header 'cantilever-project-domain'")
            return ResponseEntity.badRequest(body = APIResult.Error("Missing required header 'cantilever-project-domain'"))
        }
        val projectKeyHeader = request.headers["cantilever-project-domain"]!!
        val srcKey = projectKeyHeader + "/" + URLDecoder.decode(postToSave.srcKey, Charset.defaultCharset())

        return if (s3Service.objectExists(srcKey, sourceBucket)) {
            loadContentTree(projectKeyHeader)
            info("Updating existing file '${postToSave.srcKey}'")
            val length = s3Service.putObjectAsString(srcKey, sourceBucket, postToSave.toString(), "text/markdown")
            contentTree.updatePost(postToSave.toPostNode())
            saveContentTree(projectKeyHeader)
            ResponseEntity.ok(body = APIResult.OK("Updated file $srcKey, $length bytes"))
        } else {
            info("Creating new file...")
            val length = s3Service.putObjectAsString(srcKey, sourceBucket, postToSave.toString(), "text/markdown")
            contentTree.insertPost(postToSave.toPostNode())
            saveContentTree(projectKeyHeader)
            ResponseEntity.ok(body = APIResult.OK("Saved new file $srcKey, $length bytes"))
        }
    }

    /**
     * Delete the markdown post... and it's corresponding html? Um....
     */
    fun deleteMarkdownPost(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val markdownSource = request.pathParameters["srcKey"]
        if(request.headers["cantilever-project-domain"] === null) {
            error("Missing required header 'cantilever-project-domain'")
            return ResponseEntity.badRequest(body = APIResult.Error("Missing required header 'cantilever-project-domain'"))
        }
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
                    ResponseEntity.ok(body = APIResult.OK("Source $decoded deleted"))
                } else {
                    error("Could not delete $decoded; object not found or was not a PostNode")
                    ResponseEntity.ok(body = APIResult.Error("Could not delete $decoded; object not found or was not a Post"))
                }
            } else {
                error("Could not delete $decoded; object not found")
                ResponseEntity.ok(body = APIResult.Error("Could not delete $decoded; object not found"))
            }
        } else {
            error("Could not delete null markdownSource")
            ResponseEntity.ok(body = APIResult.Error("Could not delete null markdownSource"))
        }
    }

    /**
     * Return a list of all the posts in the content tree
     * [Request.headers] must contain a "cantilever-project-domain" header
     * @return [PostListDTO] object containing the list of posts, a count and the last updated date/time
     */
    fun getPosts(request: Request<Unit>): ResponseEntity<APIResult<PostListDTO>> {

        if(request.headers["cantilever-project-domain"] === null) {
            error("Missing required header 'cantilever-project-domain'")
            return ResponseEntity.badRequest(body = APIResult.Error("Missing required header 'cantilever-project-domain'"))
        }
        val projectKeyHeader = request.headers["cantilever-project-domain"]!!
        val projectMetadataKey = request.headers["cantilever-project-domain"] + "/" + S3_KEY.metadataKey
        return if (s3Service.objectExists(projectMetadataKey, sourceBucket)) {
            loadContentTree(request.headers["cantilever-project-domain"]!!)
            info("Fetching all posts from $projectMetadataKey")
            val lastUpdated = s3Service.getUpdatedTime(projectMetadataKey, sourceBucket)
            val posts = contentTree.items.filterIsInstance<ContentNode.PostNode>()
            val sorted = posts.sortedByDescending { it.date }
            val postList = PostListDTO(
                count = sorted.size,
                lastUpdated = lastUpdated,
                posts = sorted
            )
            if(postList.posts.isEmpty()) {
                error("No posts found in content tree")
                ResponseEntity.serverError(body = APIResult.Error("No posts found in content tree for project $projectMetadataKey"))
            } else {
                ResponseEntity.ok(body = APIResult.Success(value = postList))
            }
        } else {
            error("Cannot find file '$S3_KEY.metadataKey' in bucket $sourceBucket")
            ResponseEntity.notFound(body = APIResult.Error(statusText = "Cannot find file '${projectMetadataKey}' in bucket $sourceBucket"))
        }
    }

    /**
     * Build a [ContentNode.PostNode] object from the source specified, and add the full body text
     */
    private fun buildPostNode(
        srcKey: String
    ): ContentNode.PostNode {
        val markdown = s3Service.getObjectAsString(srcKey, sourceBucket)
        val metadata = ContentMetaDataBuilder.PostBuilder.buildFromSourceString(markdown.getFrontMatter(), srcKey)
        val body = markdown.substringAfter("---").substringAfter("---").trim()
        return metadata.apply { this.body = body }
    }

    override fun info(message: String) = println("INFO: PostController: $message")
    override fun warn(message: String) = println("WARN: PostController: $message")
    override fun error(message: String) = println("ERROR: PostController: $message")
}
