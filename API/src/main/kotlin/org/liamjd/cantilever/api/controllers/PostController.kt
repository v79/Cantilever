package org.liamjd.cantilever.api.controllers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.models.MarkdownPost
import org.liamjd.cantilever.models.Post
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.impl.extractPostMetadata
import java.net.URLDecoder
import java.nio.charset.Charset

class PostController(val sourceBucket: String, val destinationBucket: String) : KoinComponent {
    private val s3Service: S3Service by inject()

    fun loadMarkdownSource(request: Request<Unit>): ResponseEntity<APIResult<MarkdownPost>> {
        val markdownSource = request.pathParameters["srcKey"]
        return if (markdownSource != null) {
            val decoded = URLDecoder.decode(markdownSource, Charset.defaultCharset())
            println("PostsController loading Markdown file $decoded")
            return if (s3Service.objectExists(decoded, sourceBucket)) {
                val mdPost = getMarkdownPost(decoded)
                ResponseEntity.ok(body = APIResult.Success(mdPost))
            } else {
                println("PostController: File '$decoded' not found")
                ResponseEntity.notFound(body = APIResult.Error("Markdown file $decoded not found in bucket $sourceBucket"))
            }
        } else {
            ResponseEntity.badRequest(body = APIResult.Error("Invalid request for null source file"))
        }
    }

    private fun getMarkdownPost(
        srcKey: String
    ): MarkdownPost {
        val markdown = s3Service.getObjectAsString(srcKey, sourceBucket)
        val metadata = extractPostMetadata(filename = srcKey, source = markdown)

        println("Returning MarkdownPost from $metadata")
        val mdPost = MarkdownPost(
            Post(
                title = metadata.title,
                srcKey = srcKey,
                url = metadata.slug,
                templateKey = metadata.template,
                date = metadata.date,
                lastUpdated = metadata.lastModified
            )
        )
        mdPost.body = markdown.substringAfter("---").substringAfter("---").trim()
        return mdPost
    }

    /**
     * Save a [MarkdownPost] to the sources bucket
     */
    fun saveMarkdownPost(request: Request<MarkdownPost>): ResponseEntity<APIResult<String>> {
        val postToSave = request.body
        val srcKey = URLDecoder.decode(postToSave.post.srcKey, Charset.defaultCharset())

        return if(s3Service.objectExists(srcKey,sourceBucket)) {
            println("Updating existing file '${postToSave.post.srcKey}'")
            println(postToSave.toString().take(100))
            val length = s3Service.putObject(srcKey,sourceBucket,postToSave.toString(),"text/markdown")
            ResponseEntity.ok(body = APIResult.OK("Updated file $srcKey, $length bytes"))
        } else {
            println("Creating new file...")
            println(postToSave.post)
            val length = s3Service.putObject(srcKey,sourceBucket,postToSave.toString(),"text/markdown")
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
            println("PostsController deleting Markdown file $decoded")
            return if (s3Service.objectExists(decoded, sourceBucket)) {
                println("Deleting file $decoded")
                s3Service.deleteObject(decoded,sourceBucket)
                ResponseEntity.ok(body = APIResult.OK("Source $decoded deleted"))
            } else {
                ResponseEntity.ok(body = APIResult.Error("Could not delete $decoded; object not found"))
            }
        } else {
            ResponseEntity.ok(body = APIResult.Error("Could not delete null markdownSource"))
        }
    }
}
