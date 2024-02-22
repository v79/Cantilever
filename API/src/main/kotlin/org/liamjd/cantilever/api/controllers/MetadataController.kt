package org.liamjd.cantilever.api.controllers

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.common.getFrontMatter
import org.liamjd.cantilever.models.ContentMetaDataBuilder
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.ContentTree
import org.liamjd.cantilever.models.TemplateMetadata
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity

/**
 * Generate metadata across posts, pages, templates etc
 **/
class MetadataController(sourceBucket: String, generationBucket: String) : KoinComponent, APIController(sourceBucket, generationBucket) {

    /**
     * Perform a complete scan of the sources/ bucket and rebuild the metadata.json file in the generated/ folder
     */
    fun rebuildFromSources(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val projectKeyHeader = request.headers["cantilever-project-domain"]!!
        info("Rebuilding $projectKeyHeader metadata from sources")
        val projectMetadataKey = projectKeyHeader + "/" + S3_KEY.metadataKey
        val sourcesFolder = projectKeyHeader + "/" + S3_KEY.sources
        val postsFolder = projectKeyHeader + "/" + S3_KEY.postsPrefix
        val pagesFolder = projectKeyHeader + "/" + S3_KEY.pagesPrefix
        val staticsFolder = projectKeyHeader + "/" + S3_KEY.staticsPrefix
        val templatesFolder = projectKeyHeader + "/" + S3_KEY.templatesPrefix
        val imagesFolder = projectKeyHeader + "/" + S3_KEY.imagesPrefix
        val contentTree = ContentTree()
        var filesProcessed = 0
        var postsCount = 0
        var pagesCount = 0
        var imagesCount = 0
        var templatesCount = 0
        var staticsCount = 0
        var folderCount = 0
        // TODO: this only returns 1000 items, need to paginate
        val items = s3Service.listObjects(sourcesFolder, sourceBucket)
        if (items.hasContents()) {
            // Special cases are 'sources/', 'sources/posts/', 'sources/pages/' - these should be ignored
            val ignoreList =
                listOf(sourcesFolder, postsFolder, pagesFolder)
            items.contents().forEach {
                if (it.key() !in ignoreList) {
                    info("Processing ${it.key()}")
                    if (it.key().endsWith("/")) {
                        val folder = ContentNode.FolderNode(it.key())
                        contentTree.insertFolder(folder)
                        folderCount++
                        filesProcessed++
                    } else {
                        if (it.key().startsWith(postsFolder) && it.key() != postsFolder) {
                            val post = buildPostNode(it.key())
                            contentTree.insertPost(post)
                            postsCount++
                            filesProcessed++
                        }
                        if (it.key().startsWith(pagesFolder) && it.key() != pagesFolder) {
                            val page = buildPageNode(it.key())
                            contentTree.insertPage(page)
                            pagesCount +=
                                filesProcessed++
                        }
                        if (it.key().startsWith(templatesFolder) && it.key() != templatesFolder) {
                            val template = buildTemplateNode(it.key())
                            contentTree.insertTemplate(template)
                            templatesCount++
                            filesProcessed++
                        }
                        if (it.key().startsWith(staticsFolder) && it.key() != staticsFolder) {
                            val static = ContentNode.StaticNode(it.key())
                            static.fileType = it.key().substringAfterLast(".")
                            contentTree.insertStatic(static)
                            staticsCount++
                            filesProcessed++
                        }
                        if (it.key().startsWith(imagesFolder) && it.key() != imagesFolder) {
                            val media = ContentNode.ImageNode(it.key())
                            contentTree.insertImage(media)
                            imagesCount++
                            filesProcessed++
                        }
                    }
                }
            }

            // now update folder children
            contentTree.items.forEach {
                if (it is ContentNode.PageNode) {
                    val parent =
                        contentTree.items.find { node -> node.srcKey == it.parent + "/" } as ContentNode.FolderNode?
                    parent?.children?.add(it.srcKey)
                    if (it.isRoot) {
                        parent?.indexPage = it.srcKey
                    }
                }
            }

            info("Found $filesProcessed files in sources/ bucket; writing metadata.json to generated/ bucket")
            val pretty = Json { prettyPrint = true }
            val treeJson = pretty.encodeToString(ContentTree.serializer(), contentTree)
            s3Service.putObjectAsString(projectMetadataKey, generationBucket, treeJson, "application/json")
        } else {
            error("No source files found in 'sources/ which match the requirements to build a project metadata' file.")
            return ResponseEntity.serverError(
                body = APIResult.Error(
                    statusText = "No source files found in $sourceBucket which match the requirements to build a ${S3_KEY.postsKey} file."
                )
            )
        }

        return ResponseEntity.ok(
            body = APIResult.Success(
                "Rebuilt metadata.json file for $projectKeyHeader with $filesProcessed ($postsCount posts, $pagesCount pages, $imagesCount images, $templatesCount templates, $staticsCount statics)"
            )
        )
    }

    /**
     * Build a [ContentNode.PostNode] from the given key
     */
    private fun buildPostNode(postKey: String): ContentNode.PostNode {
        val postContents = s3Service.getObjectAsString(postKey, sourceBucket)
        return ContentMetaDataBuilder.PostBuilder.buildFromSourceString(postContents.getFrontMatter(), postKey)
    }

    /**
     * Build a [ContentNode.PageNode] from the given key
     */
    private fun buildPageNode(pageKey: String): ContentNode.PageNode {
        val pageContents = s3Service.getObjectAsString(pageKey, sourceBucket)
        val metadata = ContentMetaDataBuilder.PageBuilder.buildFromSourceString(
            sourceString = pageContents,
            srcKey = pageKey
        )
        val parentFolder = pageKey.substringBeforeLast("/") + "/"
        metadata.parent = parentFolder
        return metadata
    }

    /**
     * Build a [ContentNode.TemplateNode] from the given key
     */
    private fun buildTemplateNode(templateKey: String): ContentNode.TemplateNode {
        val templateContents = s3Service.getObjectAsString(templateKey, sourceBucket)
        val frontmatter = templateContents.getFrontMatter()
        val metadata = Yaml.default.decodeFromString(TemplateMetadata.serializer(), frontmatter)
        return ContentNode.TemplateNode(
            srcKey = templateKey,
            title = metadata.name,
            sections = metadata.sections ?: emptyList()
        )
    }

    override fun info(message: String) = println("INFO: MetadataController: $message")
    override fun warn(message: String) = println("WARN: MetadataController: $message")
    override fun error(message: String) = println("ERROR: MetadataController: $message")
}