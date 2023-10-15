package org.liamjd.cantilever.api.controllers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.common.toS3Key
import org.liamjd.cantilever.common.toSlug
import org.liamjd.cantilever.models.PageTreeNode
import org.liamjd.cantilever.models.rest.MarkdownPage
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.impl.extractPageModel
import java.net.URLDecoder
import java.nio.charset.Charset

/**
 * Load, save and delete Pages from the S3 bucket
 */
class PageController(val sourceBucket: String) : KoinComponent, APIController {

    private val s3Service: S3Service by inject()

    /**
     * Load a markdown file with the specified `srcKey` and return it as [MarkdownPage] response
     */
    fun loadMarkdownSource(request: Request<Unit>): ResponseEntity<APIResult<MarkdownPage>> {
        val markdownSource = request.pathParameters["srcKey"]
        return if (markdownSource != null) {
            val decoded = URLDecoder.decode(markdownSource, Charset.defaultCharset())
            info("Loading Markdown file $decoded")
            return if (s3Service.objectExists(decoded, sourceBucket)) {
                val mdPage = buildMarkdownPage(decoded)
                ResponseEntity.ok(body = APIResult.Success(mdPage))
            } else {
                error("File '$decoded' not found")
                ResponseEntity.notFound(body = APIResult.Error("Markdown file $decoded not found in bucket $sourceBucket"))
            }
        } else {
            ResponseEntity.badRequest(body = APIResult.Error("Invalid request for null source file"))
        }
    }

    /**
     * Create a folder in S3 to store pages, i.e. under /sources/pages/
     * This should be the full path.
     */
    fun createFolder(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val folderName = request.pathParameters["folderName"]
        return if (folderName != null) {
            // folder name must be web safe
            val slugged = URLDecoder.decode(folderName, Charset.defaultCharset()).toS3Key()
            info("Creating folder '$slugged'")
            if (!s3Service.objectExists(slugged, sourceBucket)) {
                val result = s3Service.createFolder(slugged, sourceBucket)
                if(result != 0) {
                    ResponseEntity.serverError(body = APIResult.Error("Folder '$slugged' was not created"))
                }
                ResponseEntity.ok(body = APIResult.OK("Folder '$slugged' created"))
            } else {
                warn("Folder '$slugged' already exists")
                ResponseEntity.accepted(body = APIResult.OK(""))
            }
        } else {
            ResponseEntity.badRequest(body = APIResult.Error("Cannot create a folder with no name"))
        }
    }

    /**
     * Build a [MarkdownPage] object from the source specified
     */
    private fun buildMarkdownPage(srcKey: String): MarkdownPage {
        val markdown = s3Service.getObjectAsString(srcKey, sourceBucket)
        val metadata = extractPageModel(key = srcKey, source = markdown)
        val pageMeta = PageTreeNode.PageMeta(
            nodeType = "page",
            title = metadata.title,
            templateKey = metadata.templateKey,
            srcKey = srcKey,
            url = metadata.url,
            attributes = metadata.attributes,
            sections = metadata.sections
        )

        return MarkdownPage(pageMeta)
    }

    /**
     * Save a [MarkdownPage] to the sources bucket
     */
    fun saveMarkdownPageSource(request: Request<MarkdownPage>): ResponseEntity<APIResult<String>> {
        info("saveMarkdownPageSource")
        val pageToSave = request.body
        pageToSave.also {
            info("PageToSave: ${it.metadata.title} has ${it.metadata.attributes.keys.size} attributes and ${it.metadata.sections.keys.size} sections")
        }
        val srcKey = URLDecoder.decode(pageToSave.metadata.srcKey, Charset.defaultCharset())
        return if (s3Service.objectExists(srcKey, sourceBucket)) {
            info("Updating existing file '${pageToSave.metadata.srcKey}'")
            val length = s3Service.putObject(srcKey, sourceBucket, pageToSave.toString(), "text/markdown")
            ResponseEntity.ok(body = APIResult.OK("Updated file $srcKey, $length bytes"))
        } else {
            info("Creating new file...")
            val length = s3Service.putObject(srcKey, sourceBucket, pageToSave.toString(), "text/markdown")
            ResponseEntity.ok(body = APIResult.OK("Saved new file $srcKey, $length bytes"))
        }
    }

    override fun info(message: String) = println("INFO: PageController: $message")
    override fun warn(message: String) = println("WARN: PageController: $message")
    override fun error(message: String) = println("ERROR: PageController: $message")
}