package org.liamjd.cantilever.api.controllers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.getFrontMatter
import org.liamjd.cantilever.models.ContentMetaDataBuilder
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.rest.PostNodeRestDTO
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import org.liamjd.cantilever.services.S3Service
import java.net.URLDecoder
import java.nio.charset.Charset

/**
 * Load, save and delete Posts from the S3 bucket
 */
class PostController(val sourceBucket: String) : KoinComponent, APIController {
    private val s3Service: S3Service by inject()

    /**
     * Load a markdown file with the specified `srcKey` and return it as [ContentNode.PostNode] response
     */
    fun loadMarkdownSource(request: Request<Unit>): ResponseEntity<APIResult<ContentNode.PostNode>> {
        val markdownSource = request.pathParameters["srcKey"]
        return if (markdownSource != null) {
            val decoded = URLDecoder.decode(markdownSource, Charset.defaultCharset())
            info("Loading Markdown file $decoded")
            return if (s3Service.objectExists(decoded, sourceBucket)) {
                val mdPost = buildPostNode(decoded)
                ResponseEntity.ok(body = APIResult.Success(mdPost))
            } else {
                error("File '$decoded' not found")
                ResponseEntity.notFound(body = APIResult.Error("Markdown file $decoded not found in bucket $sourceBucket"))
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
        val srcKey = URLDecoder.decode(postToSave.srcKey, Charset.defaultCharset())

        // this if statement is a bit pointless just now as both routes do the same thing
        return if (s3Service.objectExists(srcKey, sourceBucket)) {
            info("Updating existing file '${postToSave.srcKey}'")
            val length = s3Service.putObject(srcKey, sourceBucket, postToSave.toString(), "text/markdown")
            ResponseEntity.ok(body = APIResult.OK("Updated file $srcKey, $length bytes"))
        } else {
            info("Creating new file...")
            val length = s3Service.putObject(srcKey, sourceBucket, postToSave.toString(), "text/markdown")
            ResponseEntity.ok(body = APIResult.OK("Saved new file $srcKey, $length bytes"))
        }
    }

    /**
     * Delete the markdown post... and it's corresponding html? Um....
     */
    fun deleteMarkdownPost(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val markdownSource = request.pathParameters["srcKey"]

        return if (markdownSource != null) {
            val decoded = URLDecoder.decode(markdownSource, Charset.defaultCharset())
            info("Deleting Markdown file $decoded")
            return if (s3Service.objectExists(decoded, sourceBucket)) {
                info("Deleting file $decoded")
                s3Service.deleteObject(decoded, sourceBucket)
                ResponseEntity.ok(body = APIResult.OK("Source $decoded deleted"))
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
     * Build a [ContentNode.PostNode] object from the source specified, and add the full body text
     */
    private fun buildPostNode(
        srcKey: String
    ): ContentNode.PostNode {
        val markdown = s3Service.getObjectAsString(srcKey, sourceBucket)
        val metadata = ContentMetaDataBuilder.PostBuilder.buildFromYamlString(markdown.getFrontMatter(), srcKey)
        val body = markdown.substringAfter("---").substringAfter("---").trim()
        return metadata.apply { this.body = body }
    }

    override fun info(message: String) = println("INFO: PostController: $message")
    override fun warn(message: String) = println("WARN: PostController: $message")
    override fun error(message: String) = println("ERROR: PostController: $message")
}
