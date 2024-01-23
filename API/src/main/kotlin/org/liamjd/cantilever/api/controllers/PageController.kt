package org.liamjd.cantilever.api.controllers

import org.koin.core.component.KoinComponent
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.common.toS3Key
import org.liamjd.cantilever.models.ContentMetaDataBuilder
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.rest.FolderListDTO
import org.liamjd.cantilever.models.rest.MarkdownPageDTO
import org.liamjd.cantilever.models.rest.PageListDTO
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import java.net.URLDecoder
import java.nio.charset.Charset

/**
 * Load, save and delete Pages from the S3 bucket
 */
class PageController(sourceBucket: String) : KoinComponent, APIController(sourceBucket) {

    /**
     * Return a list of all the pages in the content tree
     * @return [PageListDTO] object containing the list of Pages, a count and the last updated date/time
     */
    fun getPages(request: Request<Unit>): ResponseEntity<APIResult<PageListDTO>> {
        return if (s3Service.objectExists(S3_KEY.metadataKey, sourceBucket)) {
            loadContentTree()
            info("Fetching all pages from metadata.json")
            val lastUpdated = s3Service.getUpdatedTime(S3_KEY.metadataKey, sourceBucket)
            val pages = contentTree.items.filterIsInstance<ContentNode.PageNode>()
//            val folders = contentTree.items.filterIsInstance<ContentNode.FolderNode>().filter { it.srcKey.startsWith(S3_KEY.pagesPrefix) }
            val sorted = pages.sortedByDescending { it.srcKey }
//            sorted.forEach { println(it.srcKey) }
            val pageList = PageListDTO(
                count = sorted.size,
                lastUpdated = lastUpdated,
                pages = sorted
            )
            ResponseEntity.ok(body = APIResult.Success(value = pageList))
        } else {
            error("Cannot find file '${S3_KEY.metadataKey}' in bucket $sourceBucket")
            ResponseEntity.notFound(
                body = APIResult.Error(message = "Cannot find file '${S3_KEY.metadataKey}' in bucket $sourceBucket")
            )
        }
    }

    /**
     * Load a markdown file with the specified `srcKey` and return it as [MarkdownPageDTO] response
     */
    fun loadMarkdownSource(request: Request<Unit>): ResponseEntity<APIResult<MarkdownPageDTO>> {
        val markdownSource = request.pathParameters["srcKey"]
        return if (markdownSource != null) {
            val decoded = URLDecoder.decode(markdownSource, Charset.defaultCharset())
            info("Loading Markdown file $decoded")
            return if (s3Service.objectExists(decoded, sourceBucket)) {
                val mdPage = buildMarkdownPage(decoded)
                ResponseEntity.ok(body = APIResult.Success(mdPage))
            } else {
                error("File '$decoded' not found")
                ResponseEntity.notFound(
                    body = APIResult.Error("Markdown file $decoded not found in bucket $sourceBucket")
                )
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
                if (result != 0) {
                    ResponseEntity.serverError(body = APIResult.Error("Folder '$slugged' was not created"))
                }
                contentTree.insertFolder(ContentNode.FolderNode(folderName))
                saveContentTree()

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
     * Save a [MarkdownPageDTO] to the sources bucket
     */
    fun saveMarkdownPageSource(request: Request<MarkdownPageDTO>): ResponseEntity<APIResult<String>> {
        info("saveMarkdownPageSource")
        val pageToSave = request.body
        pageToSave.also {
            info(
                "PageToSave: ${it.metadata.title} has ${it.metadata.attributes.keys.size} attributes and ${it.metadata.sections.keys.size} sections"
            )
        }
        val srcKey = URLDecoder.decode(pageToSave.metadata.srcKey, Charset.defaultCharset())
        return if (s3Service.objectExists(srcKey, sourceBucket)) {
            info("Updating existing file '${pageToSave.metadata.srcKey}'")
            val length = s3Service.putObjectAsString(srcKey, sourceBucket, pageToSave.toString(), "text/markdown")
            contentTree.updatePage(pageToSave.metadata).also { saveContentTree() }
            ResponseEntity.ok(body = APIResult.OK("Updated file $srcKey, $length bytes"))
        } else {
            info("Creating new file...")
            val length = s3Service.putObjectAsString(srcKey, sourceBucket, pageToSave.toString(), "text/markdown")
            contentTree.insertPage(pageToSave.metadata).also { saveContentTree() }
            ResponseEntity.ok(body = APIResult.OK("Saved new file $srcKey, $length bytes"))
        }
    }

    /**
     * Return a list of all the folders which contain pages (i.e. under /sources/pages/)
     */
    fun getFolders(request: Request<Unit>): ResponseEntity<APIResult<FolderListDTO>> {
        val folders = listOf(
            ContentNode.FolderNode(S3_KEY.pagesPrefix)
        ) + contentTree.items.filterIsInstance<ContentNode.FolderNode>()
            .filter { it.srcKey.startsWith(S3_KEY.pagesPrefix) }
        val dto = FolderListDTO(folders.size, folders)
        return ResponseEntity.ok(body = APIResult.Success(dto))
    }

    /**
     * Build a [MarkdownPageDTO] object from the source specified
     */
    private fun buildMarkdownPage(srcKey: String): MarkdownPageDTO {
        val markdown = s3Service.getObjectAsString(srcKey, sourceBucket)
        val pageMeta = ContentMetaDataBuilder.PageBuilder.buildCompletePageFromSourceString(markdown, srcKey)
        return MarkdownPageDTO(pageMeta)
    }

    override fun info(message: String) = println("INFO: PageController: $message")
    override fun warn(message: String) = println("WARN: PageController: $message")
    override fun error(message: String) = println("ERROR: PageController: $message")
}