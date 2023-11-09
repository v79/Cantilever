package org.liamjd.cantilever.api.controllers

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.liamjd.cantilever.services.S3Service
import org.koin.core.component.inject
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.common.getFrontMatter
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.ContentTree
import org.liamjd.cantilever.models.TemplateMetadata
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import org.liamjd.cantilever.services.impl.extractPageModel
import org.liamjd.cantilever.services.impl.extractPostMetadata

/**
 * Generate metadata across posts, pages, templates etc */
class MetadataController(val sourceBucket: String) : KoinComponent, APIController {

    private val s3Service: S3Service by inject()

    fun rebuildFromSources(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        info("Rebuilding project metadata from sources")
        val contentTree = ContentTree()
        var filesProcessed = 0
        // TODO: this only returns 1000 items, need to paginate
        val items = s3Service.listObjects("sources/", sourceBucket)
        if (items.hasContents()) {
            // Special cases are 'sources/', 'sources/posts/', 'sources/pages/', 'sources/templates/' - these should be ignored
            val ignoreList = listOf("sources/posts/", "sources/pages/", "sources/templates/", "sources/")
            items.contents().forEach {
                if (it.key() !in ignoreList) {
                    if (it.key().endsWith("/")) {
                        val folder = ContentNode.FolderNode(it.key())
                        info(folder.toString())
                        contentTree.insertFolder(folder)
                        filesProcessed++
                    } else {
                        if (it.key().startsWith("sources/posts/") && it.key() != "sources/posts/") {
                            val post = buildPostNode(it.key())
                            info(post.toString())
                            contentTree.insertPost(post)
                            filesProcessed++
                        }
                        if (it.key().startsWith("sources/pages/") && it.key() != "sources/pages/") {
                            val page = buildPageNode(it.key())
                            info(page.toString())
                            // TODO: really want to add to its parent folder, not the root
                            contentTree.insertPage(page)
                            filesProcessed++
                        }
                        if (it.key().startsWith("sources/templates/") && it.key() != "sources/templates/") {
                            val template = buildTemplateNode(it.key())
                            info(template.toString())
                            contentTree.insertTemplate(template)
                            filesProcessed++
                        }
                        if (it.key().startsWith("sources/statics/") && it.key() != "sources/statics/") {
                            val static = ContentNode.StaticNode(it.key())
                            static.fileType = it.key().substringAfterLast(".")
                            info(static.toString())
                            contentTree.insertStatic(static)
                            filesProcessed++
                        }
                    }
                }
            }

            info("Found $filesProcessed files in sources/ bucket; writing metadata.json to generated/ bucket")
            val pretty = Json { prettyPrint = true }
            val treeJson = pretty.encodeToString(ContentTree.serializer(), contentTree)
            s3Service.putObject("generated/metadata.json", sourceBucket, treeJson, "application/json")
        } else {
            error("No source files found in 'sources/ which match the requirements to build a project metadata' file.")
            return ResponseEntity.serverError(body = APIResult.Error(message = "No source files found in $sourceBucket which match the requirements to build a ${S3_KEY.postsKey} file."))
        }

        return ResponseEntity.notImplemented(APIResult.Error(message = "Metadata generation is a WIP"))
    }

    private fun buildPostNode(postKey: String): ContentNode.PostNode {
        val postContents = s3Service.getObjectAsString(postKey, sourceBucket)
        val frontmatter = extractPostMetadata(postKey, postContents)
        val post = ContentNode.PostNode(
            srcKey = postKey,
            title = frontmatter.title,
            templateKey = frontmatter.template,
            date = frontmatter.date,
            slug = frontmatter.slug,
            attributes = mapOf("customattributes" to "notsupportedyet-forposts")
        )
        return post
    }

    private fun buildPageNode(pageKey: String): ContentNode.PageNode {
        val pageContents = s3Service.getObjectAsString(pageKey, sourceBucket)
        val frontmatter = extractPageModel(pageKey, pageContents)
        val page = ContentNode.PageNode(
            srcKey = pageKey,
            title = frontmatter.title,
            templateKey = frontmatter.templateKey,
            url = frontmatter.url,
            isRoot = frontmatter.isRoot,
            attributes = frontmatter.attributes,
            sections = frontmatter.sections.keys.associateWith { "" }
        )
        return page
    }

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
}