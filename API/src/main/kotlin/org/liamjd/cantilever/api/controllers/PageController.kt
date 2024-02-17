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
import org.liamjd.cantilever.models.rest.ReassignIndexRequestDTO
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
        if(request.headers["cantilever-project-domain"] === null) {
            error("Missing required header 'cantilever-project-domain'")
            return ResponseEntity.badRequest(body = APIResult.Error("Missing required header 'cantilever-project-domain'"))
        }
        val projectKeyHeader = request.headers["cantilever-project-domain"]!!
        return if (s3Service.objectExists(S3_KEY.metadataKey, sourceBucket)) {
            loadContentTree(projectKeyHeader)
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
                body = APIResult.Error(statusText = "Cannot find file '${S3_KEY.metadataKey}' in bucket $sourceBucket")
            )
        }
    }

    /**
     * Load a markdown file with the specified `srcKey` and return it as [MarkdownPageDTO] response
     */
    fun loadMarkdownSource(request: Request<Unit>): ResponseEntity<APIResult<MarkdownPageDTO>> {
        if(request.headers["cantilever-project-domain"] === null) {
            error("Missing required header 'cantilever-project-domain'")
            return ResponseEntity.badRequest(body = APIResult.Error("Missing required header 'cantilever-project-domain'"))
        }
        val projectKeyHeader = request.headers["cantilever-project-domain"]!!
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
        if(request.headers["cantilever-project-domain"] === null) {
            error("Missing required header 'cantilever-project-domain'")
            return ResponseEntity.badRequest(body = APIResult.Error("Missing required header 'cantilever-project-domain'"))
        }
        val projectKeyHeader = request.headers["cantilever-project-domain"]!!
        val folderName = request.pathParameters["folderName"]
        return if (folderName != null) {
            loadContentTree(projectKeyHeader) // TODO: can we move this to the abstract class? Calling it for every function seems wasteful
            // folder name must be web safe
            val slugged = URLDecoder.decode(folderName, Charset.defaultCharset()).toS3Key()
            info("Creating folder '$slugged'")
            if (!s3Service.objectExists(slugged, sourceBucket)) {
                val result = s3Service.createFolder(slugged, sourceBucket)
                if (result != 0) {
                    ResponseEntity.serverError(body = APIResult.Error("Folder '$slugged' was not created"))
                }
                contentTree.insertFolder(ContentNode.FolderNode(folderName))
                saveContentTree(projectKeyHeader)

                ResponseEntity.ok(body = APIResult.OK("Folder '$slugged' created"))
            } else {
                warn("Folder '$slugged' already exists")
                ResponseEntity.accepted(body = APIResult.OK("Folder '$slugged' already exists"))
            }
        } else {
            ResponseEntity.badRequest(body = APIResult.Error("Cannot create a folder with no name"))
        }
    }

    /**
     * Save a [MarkdownPageDTO] to the sources bucket
     */
    fun saveMarkdownPageSource(request: Request<ContentNode.PageNode>): ResponseEntity<APIResult<String>> {
        if(request.headers["cantilever-project-domain"] === null) {
            error("Missing required header 'cantilever-project-domain'")
            return ResponseEntity.badRequest(body = APIResult.Error("Missing required header 'cantilever-project-domain'"))
        }
        val projectKeyHeader = request.headers["cantilever-project-domain"]!!
        info("saveMarkdownPageSource")
        val pageToSave = request.body
        pageToSave.also {
            info(
                "PageToSave: ${it.title} has ${it.attributes.keys.size} attributes and ${it.sections.keys.size} sections"
            )
        }
        val srcKey = URLDecoder.decode(pageToSave.srcKey, Charset.defaultCharset())
        return if (s3Service.objectExists(srcKey, sourceBucket)) {
            info("Updating existing file '${pageToSave.srcKey}'")
            val length =
                s3Service.putObjectAsString(srcKey, sourceBucket, convertNodeToMarkdown(pageToSave), "text/markdown")
            contentTree.updatePage(pageToSave)
            saveContentTree(projectKeyHeader)
            ResponseEntity.ok(body = APIResult.OK("Updated file $srcKey, $length bytes"))
        } else {
            info("Creating new file...")
            val length =
                s3Service.putObjectAsString(srcKey, sourceBucket, convertNodeToMarkdown(pageToSave), "text/markdown")
            contentTree.insertPage(pageToSave)
            saveContentTree(projectKeyHeader)
            ResponseEntity.ok(body = APIResult.OK("Saved new file $srcKey, $length bytes"))
        }
    }

    /**
     * Delete a markdown page from the sources bucket and update the content tree
     */
    fun deleteMarkdownPageSource(request: Request<Unit>): ResponseEntity<APIResult<String>> {
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
                val pageNode = contentTree.getNode(decoded)
                if (pageNode != null && pageNode is ContentNode.PageNode) {
                    info("Deleting markdown file $decoded")
                    s3Service.deleteObject(decoded, sourceBucket)
                    contentTree.deletePage(pageNode)
                    saveContentTree(projectKeyHeader)
                    ResponseEntity.ok(body = APIResult.OK("Source $decoded deleted"))
                } else {
                    error("Could not delete $decoded; object not found or was not a PageNode")
                    ResponseEntity.ok(
                        body = APIResult.Error("Could not delete $decoded; object not found or was not a Page")
                    )
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
     * Return a list of all the folders which contain pages (i.e. under /sources/pages/)
     */
    fun getFolders(request: Request<Unit>): ResponseEntity<APIResult<FolderListDTO>> {
        if(request.headers["cantilever-project-domain"] === null) {
            error("Missing required header 'cantilever-project-domain'")
            return ResponseEntity.badRequest(body = APIResult.Error("Missing required header 'cantilever-project-domain'"))
        }
        val projectKeyHeader = request.headers["cantilever-project-domain"]!!
        val folders = listOf(
            ContentNode.FolderNode(S3_KEY.pagesPrefix)
        ) + contentTree.items.filterIsInstance<ContentNode.FolderNode>()
            .filter { it.srcKey.startsWith(S3_KEY.pagesPrefix) }
        val dto = FolderListDTO(folders.size, folders)
        return ResponseEntity.ok(body = APIResult.Success(dto))
    }

    /**
     * Delete a folder from the sources bucket and update the content tree.
     * The folder must be empty and contain no pages.
     */
    fun deleteFolder(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        if(request.headers["cantilever-project-domain"] === null) {
            error("Missing required header 'cantilever-project-domain'")
            return ResponseEntity.badRequest(body = APIResult.Error("Missing required header 'cantilever-project-domain'"))
        }
        val projectKeyHeader = request.headers["cantilever-project-domain"]!!
        val folderKey = request.pathParameters["srcKey"]
        return if (folderKey != null) {
            val decoded = URLDecoder.decode(folderKey, Charset.defaultCharset())
            if (s3Service.objectExists(decoded, sourceBucket)) {
                val folderNode = contentTree.getNode(decoded)
                if (folderNode != null && folderNode is ContentNode.FolderNode) {
                    if (folderNode.children.isEmpty()) {
                        info("Deleting folder $decoded")
                        s3Service.deleteObject(decoded, sourceBucket)
                        contentTree.deleteFolder(folderNode)
                        saveContentTree(projectKeyHeader)
                        ResponseEntity.ok(body = APIResult.OK("Folder $decoded deleted"))
                    } else {
                        warn("Folder $decoded is not empty so it was not deleted")
                        ResponseEntity.badRequest(body = APIResult.Error("Folder $decoded is not empty"))
                    }
                } else {
                    ResponseEntity.badRequest(body = APIResult.Error("Folder $decoded not found"))
                }
            } else {
                ResponseEntity.badRequest(body = APIResult.Error("Folder $decoded not found"))
            }
        } else {
            ResponseEntity.badRequest(body = APIResult.Error("No folderKey specified"))
        }
    }

    /**
     * Reassign the index page of a folder to another page
     * @param request [ReassignIndexRequestDTO] containing the source and destination pages and the folder
     */
    fun reassignIndex(request: Request<ReassignIndexRequestDTO>): ResponseEntity<APIResult<String>> {
        if(request.headers["cantilever-project-domain"] === null) {
            error("Missing required header 'cantilever-project-domain'")
            return ResponseEntity.badRequest(body = APIResult.Error("Missing required header 'cantilever-project-domain'"))
        }
        val projectKeyHeader = request.headers["cantilever-project-domain"]!!
        val requestBody = request.body
        loadContentTree(projectKeyHeader)
        val from = requestBody.from
        val to = requestBody.to
        val folder = requestBody.folder
        // check to see that each of these exist
        try {
            if (s3Service.objectExists(from, sourceBucket) && s3Service.objectExists(to, sourceBucket)) {
                // confirm that from is the index of the folder
                val folderNode = contentTree.getNode(
                    if (folder.endsWith("/")) {
                        folder
                    } else {
                        "$folder/"
                    }
                )
                if (folderNode is ContentNode.FolderNode) {
                    if (folderNode.indexPage == from) {
                        // update the metadata for each of these pages
                        val fromString = s3Service.getObjectAsString(from, sourceBucket)
                        val toString = s3Service.getObjectAsString(to, sourceBucket)
                        val fromMeta =
                            ContentMetaDataBuilder.PageBuilder.buildCompletePageFromSourceString(fromString, from)
                        val updatedFrom = fromMeta.copy(isRoot = false)

                         val toMeta =
                            ContentMetaDataBuilder.PageBuilder.buildCompletePageFromSourceString(toString, to)
                        val updatedTo = toMeta.copy(isRoot = true)

                        println("Writing updated pages '$from' and '$to' to S3 bucket $sourceBucket")
                        s3Service.putObjectAsString(from, sourceBucket, convertNodeToMarkdown(updatedFrom), "text/markdown")
                        s3Service.putObjectAsString(to, sourceBucket, convertNodeToMarkdown(updatedTo), "text/markdown")
                        println("Updating content tree")
                        contentTree.updatePage(updatedFrom)
                        contentTree.updatePage(updatedTo)
                        contentTree.updateFolder(folderNode.copy(indexPage = to))
                        saveContentTree(projectKeyHeader)
                        return ResponseEntity.ok(
                            body = APIResult.OK("Reassigned index from $from to $to in folder $folder")
                        )

                    } else {
                        return ResponseEntity.badRequest(body = APIResult.Error("$from is not the index of $folder"))
                    }
                } else {
                    return ResponseEntity.badRequest(body = APIResult.Error("$folder is not a folder"))
                }
            } else {
                return ResponseEntity.badRequest(body = APIResult.Error("Source files not found"))
            }
        } catch (e: Exception) {
            return ResponseEntity.serverError(body = APIResult.Error("Error: ${e.message}"))
        }
    }

    /**
     * Build a [MarkdownPageDTO] object from the source specified
     */
    @Deprecated("Use ContentMetaDataBuilder.PageBuilder.buildCompletePageFromSourceString instead")
    private fun buildMarkdownPage(srcKey: String): MarkdownPageDTO {
        val markdown = s3Service.getObjectAsString(srcKey, sourceBucket)
        val pageMeta = ContentMetaDataBuilder.PageBuilder.buildCompletePageFromSourceString(markdown, srcKey)
        return MarkdownPageDTO(pageMeta)
    }

    /**
     * Convert a [ContentNode.PageNode] to a markdown, with each section string
     */
    private fun convertNodeToMarkdown(page: ContentNode.PageNode): String {
        val separator = "---"
        val sBuilder = StringBuilder()
        sBuilder.apply {
            appendLine(separator)
            appendLine("title: ${page.title}")
            appendLine("templateKey: ${page.templateKey}")
            if (page.isRoot) {
                appendLine("isRoot: true")
            }
            page.attributes.forEach {
                appendLine("#${it.key}: ${it.value}")
            }
            page.sections.forEach {
                appendLine("$separator #${it.key}")
                appendLine(it.value)
            }
        }
        return sBuilder.toString().trim()
    }

    override fun info(message: String) = println("INFO: PageController: $message")
    override fun warn(message: String) = println("WARN: PageController: $message")
    override fun error(message: String) = println("ERROR: PageController: $message")
}