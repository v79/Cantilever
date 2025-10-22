package org.liamjd.cantilever.api.controllers

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.koin.core.component.KoinComponent
import org.liamjd.apiviaduct.routing.Request
import org.liamjd.apiviaduct.routing.Response
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.SOURCE_TYPE
import org.liamjd.cantilever.common.toS3Key
import org.liamjd.cantilever.models.ContentMetaDataBuilder
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.rest.FolderListDTO
import org.liamjd.cantilever.models.rest.MarkdownPageDTO
import org.liamjd.cantilever.models.rest.PageListDTO
import org.liamjd.cantilever.models.rest.ReassignIndexRequestDTO
import java.net.URLDecoder
import java.nio.charset.Charset

/**
 * Load, save and delete Pages from the S3 bucket
 */
class PageController(sourceBucket: String, generationBucket: String) : KoinComponent,
    APIController(sourceBucket, generationBucket) {

    /**
     * Return a list of all the pages
     * @return [PageListDTO] object containing the list of Pages, a count and the last updated date/time
     */
    fun getPages(request: Request<Unit>): Response<APIResult<PageListDTO>> {
        val domain = request.headers["cantilever-project-domain"]

        return if (domain.isNullOrBlank()) {
            Response.badRequest(body = APIResult.Error("Invalid project key'"))
        } else {
            val pageList = runBlocking {
                val pages = getPagesFromDB(domain)
                PageListDTO(count = pages.size, lastUpdated = Clock.System.now(), pages = pages)
            }
            Response.ok(body = APIResult.Success(value = pageList))
        }
    }

    /**
     * Return a list of all Page nodes for the given domain
     */
    private suspend fun getPagesFromDB(domain: String): List<ContentNode.PageNode> {
        return dynamoDBService.listAllNodesForProject(domain, SOURCE_TYPE.Pages)
            .filterIsInstance<ContentNode.PageNode>()
    }

    /**
     * Load a Markdown file with the specified `srcKey` and return it as [MarkdownPageDTO] response
     */
    fun loadMarkdownSource(request: Request<Unit>): Response<APIResult<MarkdownPageDTO>> {
        val markdownSource = request.pathParameters["srcKey"]
        return if (markdownSource != null) {
            val decoded = URLDecoder.decode(markdownSource, Charset.defaultCharset())
            info("Loading Markdown file $decoded")
            return if (s3Service.objectExists(decoded, sourceBucket)) {
                val mdPage = buildMarkdownPage(decoded)
                Response.ok(body = APIResult.Success(mdPage))
            } else {
                error("File '$decoded' not found")
                Response.notFound(
                    body = APIResult.Error("Markdown file $decoded not found in bucket $sourceBucket")
                )
            }
        } else {
            Response.badRequest(body = APIResult.Error("Invalid request for null source file"))
        }
    }

    /**
     * Create a folder in S3 to store pages, i.e. under /sources/pages/
     * This should be the full path.
     */
    fun createFolder(request: Request<Unit>): Response<APIResult<String>> {
        val folderName = URLDecoder.decode(
            request.pathParameters["folderName"], Charset.defaultCharset()
        )
        return if (folderName != null) {
            // folder name must be web safe
            val slugged = URLDecoder.decode(folderName, Charset.defaultCharset()).toS3Key()
            info("Creating folder '$slugged'")
            if (!s3Service.objectExists(slugged, sourceBucket)) {
                val result = s3Service.createFolder(slugged, sourceBucket)
                if (result != 0) {
                    Response.serverError(body = APIResult.Error("Folder '$slugged' was not created"))
                }
                Response.ok(body = APIResult.OK("Folder '$slugged' created"))
            } else {
                warn("Folder '$slugged' already exists")
                Response.accepted(body = APIResult.OK("Folder '$slugged' already exists"))
            }
        } else {
            Response.badRequest(body = APIResult.Error("Cannot create a folder with no name"))
        }
    }

    /**
     * Save a [MarkdownPageDTO] to the `sources` bucket
     */
    fun saveMarkdownPageSource(request: Request<ContentNode.PageNode>): Response<APIResult<String>> {
        info("saveMarkdownPageSource")
        val pageToSave = request.body
        val domain = request.headers["cantilever-project-domain"]!!

        pageToSave.also {
            info(
                "PageToSave: ${it.title} has ${it.attributes.keys.size} attributes and ${it.sections.keys.size} sections"
            )
        }

        val srcKey = URLDecoder.decode(pageToSave.srcKey, Charset.defaultCharset())
        return if (s3Service.objectExists(srcKey, sourceBucket)) {
            info("Updating existing file '${pageToSave.srcKey}'")
            val length =
                s3Service.putObjectAsString(
                    srcKey,
                    sourceBucket,
                    convertNodeToMarkdown(pageToSave, domain),
                    "text/markdown"
                )
            Response.ok(body = APIResult.OK("Updated file $srcKey, $length bytes"))
        } else {
            info("Creating new file with srcKey '${pageToSave.srcKey}'")
            val length =
                s3Service.putObjectAsString(
                    srcKey,
                    sourceBucket,
                    convertNodeToMarkdown(pageToSave, domain),
                    "text/markdown"
                )
            Response.ok(body = APIResult.OK("Saved new file $srcKey, $length bytes"))
        }
    }

    /**
     * Delete a Markdown page from the `sources` bucket
     */
    fun deleteMarkdownPageSource(request: Request<Unit>): Response<APIResult<String>> {
        val markdownSource = request.pathParameters["srcKey"]
        if (request.headers["cantilever-project-domain"] === null) {
            error("Missing required header 'cantilever-project-domain'")
            return Response.badRequest(
                body = APIResult.Error("Missing required header 'cantilever-project-domain'")
            )
        }
        return if (markdownSource != null) {
            val decoded = URLDecoder.decode(markdownSource, Charset.defaultCharset())
            return if (s3Service.objectExists(decoded, sourceBucket)) {
                info("Deleting markdown file $decoded")
                s3Service.deleteObject(decoded, sourceBucket)
                Response.ok(body = APIResult.OK("Source $decoded deleted"))
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
     * Return a list of all the folders which contain pages (i.e. under /sources/pages/)
     */
    fun getFolders(request: Request<Unit>): Response<APIResult<FolderListDTO>> {
        val domain = request.headers["cantilever-project-domain"]
        return if (domain.isNullOrBlank()) {
            Response.badRequest(body = APIResult.Error("Invalid project key'"))
        } else {
            val folderList = runBlocking {
                val folders = getFoldersFromDB(domain)
                FolderListDTO(count = folders.size, folders = folders)
            }
            Response.ok(body = APIResult.Success(value = folderList))
        }
    }

    /**
     * Return a list of all Folder nodes for the given domain
     */
    private suspend fun getFoldersFromDB(domain: String): List<ContentNode.FolderNode> {
        return dynamoDBService.listAllNodesForProject(domain, SOURCE_TYPE.Folders)
            .filterIsInstance<ContentNode.FolderNode>()
    }

    /**
     * Delete a folder from the `sources` bucket.
     * The folder must be empty and contain no pages.
     */
    fun deleteFolder(request: Request<Unit>): Response<APIResult<String>> {
        val domain = request.headers["cantilever-project-domain"]
        if (domain.isNullOrBlank()) {
            return Response.badRequest(
                body = APIResult.Error("Invalid project domain'")
            )
        } else {
            val folderKey = request.pathParameters["srcKey"]
            return if (folderKey != null) {
                runBlocking {
                    val decoded = URLDecoder.decode(folderKey, Charset.defaultCharset())
                    if (s3Service.objectExists(decoded, sourceBucket)) {
                        val folderNode = dynamoDBService.getContentNode(decoded, domain, SOURCE_TYPE.Folders)
                        if (folderNode != null && folderNode is ContentNode.FolderNode) {
                            if (folderNode.children.isEmpty()) {
                                info("Deleting folder $decoded")
                                s3Service.deleteObject(decoded, sourceBucket)
                                Response.ok(body = APIResult.OK("Folder $decoded deleted"))
                            } else {
                                warn("Folder $decoded is not empty so it was not deleted")
                                Response.badRequest(body = APIResult.Error("Folder $decoded is not empty"))
                            }
                        } else {
                            Response.badRequest(body = APIResult.Error("Folder $decoded not found"))
                        }
                    } else {
                        Response.badRequest(body = APIResult.Error("Folder $decoded not found"))
                    }
                }
            } else {
                Response.badRequest(body = APIResult.Error("No folderKey specified"))
            }
        }
    }

    /**
     * Reassign the index page of a folder to another page
     * @param request [ReassignIndexRequestDTO] containing the source and destination pages and the folder
     */
    fun reassignIndex(request: Request<ReassignIndexRequestDTO>): Response<APIResult<String>> {
        val domain = request.headers["cantilever-project-domain"]!!
        val requestBody = request.body
        val from = requestBody.from
        val to = requestBody.to
        val folder = requestBody.folder
        // check to see that each of these exist
        info("Reassigning index from $from to $to in folder $folder")
        try {
            if (s3Service.objectExists(from, sourceBucket) && s3Service.objectExists(to, sourceBucket)) {
                // confirm that from is the index of the folder
                runBlocking {
                    val folderNode = dynamoDBService.getContentNode(
                        srcKey = folder,
                        projectDomain = domain,
                        contentType = SOURCE_TYPE.Folders
                    )
                    return@runBlocking if (folderNode is ContentNode.FolderNode) {
                        if (folderNode.indexPage == from) {
                            // update the metadata for each of these pages
                            val fromString = s3Service.getObjectAsString(from, sourceBucket)
                            val toString = s3Service.getObjectAsString(to, sourceBucket)
                            val fromMeta =
                                ContentMetaDataBuilder.PageBuilder.buildCompletePageFromSourceString(
                                    fromString,
                                    from
                                )
                            val updatedFrom = fromMeta.copy(isRoot = false)

                            val toMeta =
                                ContentMetaDataBuilder.PageBuilder.buildCompletePageFromSourceString(toString, to)
                            val updatedTo = toMeta.copy(isRoot = true)

                            println("Writing updated pages '$from' and '$to' to S3 bucket $sourceBucket")
                            s3Service.putObjectAsString(
                                from, sourceBucket, convertNodeToMarkdown(updatedFrom, domain), "text/markdown"
                            )
                            s3Service.putObjectAsString(
                                to,
                                sourceBucket,
                                convertNodeToMarkdown(updatedTo, domain),
                                "text/markdown"
                            )
                            println("Updating records in DynamoDB to reassign index from $from to $to")
                            dynamoDBService.upsertContentNode(
                                updatedFrom.srcKey,
                                domain,
                                SOURCE_TYPE.Pages,
                                updatedFrom,
                                updatedFrom.attributes
                            )
                            dynamoDBService.upsertContentNode(
                                updatedTo.srcKey,
                                domain,
                                SOURCE_TYPE.Pages,
                                updatedTo,
                                updatedTo.attributes
                            )
                            dynamoDBService.upsertContentNode(
                                folderNode.srcKey,
                                domain,
                                SOURCE_TYPE.Folders,
                                folderNode.copy(indexPage = to),
                                emptyMap() // no attributes to update yet
                            )
                        }
                        Response.ok(
                            body = APIResult.OK("Reassigned index from $from to $to in folder $folder")
                        )

                    } else {
                        Response.badRequest(body = APIResult.Error("$from is not the index of $folder"))
                    }
                }
            } else {
                return Response.badRequest(body = APIResult.Error("Source files not found for index reassignment"))
            }
        } catch (e: Exception) {
            return Response.serverError(body = APIResult.Error("Error: ${e.message}"))
        }
        return Response.serverError(body = APIResult.Error("Unknown error occurred during index reassignment"))
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
     * Convert a [ContentNode.PageNode] to a Markdown, with each section string
     */
    private fun convertNodeToMarkdown(page: ContentNode.PageNode, domain: String): String {
        val separator = "---"
        val sBuilder = StringBuilder()
        // templateKey must not have the domain prefix; it probably won't but this is a safeguard
        val templateKey = page.templateKey.removePrefix("${domain}/")
        sBuilder.apply {
            appendLine(separator)
            appendLine("title: ${page.title}")
            appendLine("templateKey: $templateKey")
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