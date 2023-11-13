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
class MetadataController(sourceBucket: String) : KoinComponent, APIController(sourceBucket) {

    /**
     * Perform a complete scan of the sources/ bucket and rebuild the metadata.json file in the generated/ folder
     */
    fun rebuildFromSources(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        info("Rebuilding project metadata from sources")
        val contentTree = ContentTree()
        var filesProcessed = 0
        // TODO: this only returns 1000 items, need to paginate
        val items = s3Service.listObjects(S3_KEY.sourcesPrefix, sourceBucket)
        if (items.hasContents()) {
            // Special cases are 'sources/', 'sources/posts/', 'sources/pages/', 'sources/templates/' - these should be ignored
            val ignoreList =
                listOf(S3_KEY.postsPrefix, S3_KEY.pagesPrefix, S3_KEY.templatesPrefix, S3_KEY.sourcesPrefix)
            items.contents().forEach {
                if (it.key() !in ignoreList) {
                    info("Processing ${it.key()}")
                    if (it.key().endsWith("/")) {
                        val folder = ContentNode.FolderNode(it.key())
                        contentTree.insertFolder(folder)
                        filesProcessed++
                    } else {
                        if (it.key().startsWith(S3_KEY.postsPrefix) && it.key() != S3_KEY.postsPrefix) {
                            val post = buildPostNode(it.key())
                            contentTree.insertPost(post)
                            filesProcessed++
                        }
                        if (it.key().startsWith(S3_KEY.pagesPrefix) && it.key() != S3_KEY.pagesPrefix) {
                            val page = buildPageNode(it.key())
                            contentTree.insertPage(page)
                            filesProcessed++
                        }
                        if (it.key().startsWith(S3_KEY.templatesPrefix) && it.key() != S3_KEY.templatesPrefix) {
                            val template = buildTemplateNode(it.key())
                            contentTree.insertTemplate(template)
                            filesProcessed++
                        }
                        if (it.key().startsWith(S3_KEY.staticsPrefix) && it.key() != S3_KEY.staticsPrefix) {
                            val static = ContentNode.StaticNode(it.key())
                            static.fileType = it.key().substringAfterLast(".")
                            contentTree.insertStatic(static)
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
            s3Service.putObject(S3_KEY.metadataKey, sourceBucket, treeJson, "application/json")
        } else {
            error("No source files found in 'sources/ which match the requirements to build a project metadata' file.")
            return ResponseEntity.serverError(body = APIResult.Error(message = "No source files found in $sourceBucket which match the requirements to build a ${S3_KEY.postsKey} file."))
        }

        return ResponseEntity.notImplemented(APIResult.Error(message = "Metadata generation is a WIP"))
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
        val parentFolder = pageKey.substringBeforeLast("/")
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
        val template = ContentNode.TemplateNode(
            srcKey = templateKey,
            title = metadata.name,
            sections = metadata.sections ?: emptyList()
        )
        return template
    }

    override fun info(message: String) = println("INFO: MetadataController: $message")
    override fun warn(message: String) = println("WARN: MetadataController: $message")
    override fun error(message: String) = println("ERROR: MetadataController: $message")
}