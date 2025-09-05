package org.liamjd.cantilever.api.controllers

import com.charleskorn.kaml.Yaml
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.liamjd.apiviaduct.routing.Request
import org.liamjd.apiviaduct.routing.Response
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.common.SOURCE_TYPE
import org.liamjd.cantilever.common.getFrontMatter
import org.liamjd.cantilever.models.ContentMetaDataBuilder
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.TemplateMetadata

/**
 * Generate metadata across posts, pages, templates etc
 **/
class MetadataController(sourceBucket: String, generationBucket: String) : KoinComponent,
    APIController(sourceBucket, generationBucket) {

    /**
     * Perform a complete scan of the sources/ bucket and rebuild the dynamoDB records for all nodes
     */
    fun rebuildFromSources(request: Request<Unit>): Response<APIResult<String>> {
        val domain = request.headers["cantilever-project-domain"]!!
        info("Rebuilding $domain metadata from sources")
        val sourcesFolder = domain + "/" + S3_KEY.sources + "/"
        val postsFolder = domain + "/" + S3_KEY.postsPrefix
        val pagesFolder = domain + "/" + S3_KEY.pagesPrefix
        val staticsFolder = domain + "/" + S3_KEY.staticsPrefix
        val templatesFolder = domain + "/" + S3_KEY.templatesPrefix
        val imagesFolder = domain + "/" + S3_KEY.imagesPrefix
        var filesProcessed = 0
        var postsCount = 0
        var pagesCount = 0
        var imagesCount = 0
        var templatesCount = 0
        var staticsCount = 0
        var folderCount = 0
        val posts = mutableListOf<ContentNode.PostNode>()
        val pages = mutableListOf<ContentNode.PageNode>()
        val templates = mutableListOf<ContentNode.TemplateNode>()
        val statics = mutableListOf<ContentNode.StaticNode>()
        val images = mutableListOf<ContentNode.ImageNode>()
        val folders = mutableSetOf<ContentNode.FolderNode>()
        // TODO: this only returns 1000 items, need to paginate
        val items = s3Service.listObjects(sourcesFolder, sourceBucket)
        if (items.hasContents()) {
            // Special cases are 'sources/', 'sources/posts/',  etc. - these should be ignored. But not 'sources/pages/' as I need that as the parent folder for pages.
            val ignoreList =
                listOf(sourcesFolder, postsFolder, templatesFolder, staticsFolder, imagesFolder)
            info("Folder ignore list: $ignoreList")
            items.contents().forEach {
                // Folders (items with a name ending in "/" may be real or implied in S3
                // I.E. not every folder actually exists in s3
                if (it.key() !in ignoreList) {
                    info("Processing ${it.key()}")
                    if (it.key().endsWith("/") && it.key().startsWith(pagesFolder) && !isIgnored(it.key(), ignoreList)) {
                        val folder = ContentNode.FolderNode(it.key())
                        info("Found page folder ${it.key()} and created node $folder")
                        folders.add(folder)
                        folderCount++
                        filesProcessed++
                    } else {
                        if (it.key().startsWith(postsFolder) && it.key() != postsFolder) {
                            val post = buildPostNode(it.key())
                            posts.add(post)
                            postsCount++
                            filesProcessed++
                        }
                        if (it.key().startsWith(pagesFolder) && it.key() != pagesFolder) {
                            val page = buildPageNode(it.key())
                            pages.add(page)
                            pagesCount++
                            filesProcessed++
                            // ensure folder nodes exist for this page's full parent chain
                            addFolderChainForPage(page, pagesFolder, ignoreList, folders)
                        }
                        if (it.key().startsWith(templatesFolder) && it.key() != templatesFolder) {
                            val template = buildTemplateNode(it.key())
                            templates.add(template)
                            templatesCount++
                            filesProcessed++
                        }
                        if (it.key().startsWith(staticsFolder) && it.key() != staticsFolder) {
                            val static = ContentNode.StaticNode(srcKey = it.key(),url = "<unknown>")
                            static.fileType = it.key().substringAfterLast(".")
                            statics.add(static)
                            staticsCount++
                            filesProcessed++
                        }
                        if (it.key().startsWith(imagesFolder) && it.key() != imagesFolder) {
                            val media = ContentNode.ImageNode(it.key())
                            images.add(media)
                            imagesCount++
                            filesProcessed++
                        }
                    }
                }
            }

            // sort posts
            posts.sortByDescending { it.date }

            runBlocking {
                // update database entries
                pages.forEach { page ->
                    val parent = folders.find { folder -> folder.srcKey == page.parent }
                    if (parent != null) {
                        if (!parent.children.contains(page.srcKey)) {
                            parent.children.add(page.srcKey)
                        }
                        if (page.isRoot) {
                            parent.indexPage = page.srcKey
                        }
                    }
                    dynamoDBService.upsertContentNode(page.srcKey, domain, SOURCE_TYPE.Pages, page, page.attributes)
                }
                folders.forEach { folder ->
                    folder.indexPage = pages.find { it.parent == folder.srcKey && it.isRoot }?.srcKey ?: ""
                    info("Upserting folder ${folder.srcKey} with index page '${folder.indexPage}'")
                    dynamoDBService.upsertContentNode(folder.srcKey, domain, SOURCE_TYPE.Folders, folder, emptyMap())
                }
                statics.forEach { static ->
                    dynamoDBService.upsertContentNode(static.srcKey, domain, SOURCE_TYPE.Statics, static, emptyMap())
                }
                templates.forEach { template ->
                    dynamoDBService.upsertContentNode(
                        template.srcKey,
                        domain,
                        SOURCE_TYPE.Templates,
                        template,
                        emptyMap()
                    )
                }
                // this is our chance to refresh the next/previous links between posts, based on the date
                posts.forEachIndexed { index, post ->
                    val nextPost = if (index > 0) posts[index - 1] else null
                    val previousPost = if (index < posts.size - 1) posts[index + 1] else null
                    post.next = nextPost?.slug ?: ""
                    post.prev = previousPost?.slug ?: ""
                    dynamoDBService.upsertContentNode(post.srcKey, domain, SOURCE_TYPE.Posts, post, post.attributes)
                }
            }

            info("Found $filesProcessed files in sources/ bucket")

        } else {
            error("No source files found in 'sources/ which match the requirements to persist in the node database.")
            return Response.serverError(
                body = APIResult.Error(
                    statusText = "No source files found in $sourceBucket which match the requirements to to persist in the node database."
                )
            )
        }

        return Response.ok(
            body = APIResult.Success(
                "Rebuild node database for $domain with $filesProcessed ($postsCount posts, $pagesCount pages, $imagesCount images, $templatesCount templates, $staticsCount statics, $folderCount folders)"
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
        val frontMatter = templateContents.getFrontMatter()
        val metadata = Yaml.default.decodeFromString(TemplateMetadata.serializer(), frontMatter)
        return ContentNode.TemplateNode(
            srcKey = templateKey,
            title = metadata.name,
            sections = metadata.sections ?: emptyList()
        )
    }

    /**
     * Return true if the key should be ignored based on the ignore list, treating trailing slashes as equivalent
     */
    private fun isIgnored(key: String, ignoreList: List<String>): Boolean {
        val k = key.trimEnd('/')
        return ignoreList.any { it.trimEnd('/') == k }
    }

    /**
     * Ensure FolderNodes are created for the full parent chain of the given page, within the pages folder tree only.
     * Also add the page as a child of its immediate parent folder.
     */
    private fun addFolderChainForPage(
        page: ContentNode.PageNode,
        pagesFolder: String,
        ignoreList: List<String>,
        folders: MutableSet<ContentNode.FolderNode>
    ) {
        val pagesRoot = pagesFolder.trimEnd('/') + "/"
        val parentPath = page.parent // already ends with "/"
        if (!parentPath.startsWith(pagesRoot)) return
        // build relative path under pages root
        val relative = parentPath.removePrefix(pagesRoot).trimEnd('/')
        if (relative.isEmpty()) return // page in root pages folder; we don't want to create a folder called sources/pages//
        val parts = relative.split('/')
        var current = pagesRoot
        for ((index, part) in parts.withIndex()) {
            current += "$part/"
            if (!isIgnored(current, ignoreList)) {
                val existing = folders.find { it.srcKey == current }
                if (existing == null) {
                    folders.add(ContentNode.FolderNode(current))
                }
            }
            // if this is the last part, it's the immediate parent; add page as child
            if (index == parts.lastIndex) {
                val parentFolder = folders.find { it.srcKey == current }
                if (parentFolder != null && !parentFolder.children.contains(page.srcKey)) {
                    parentFolder.children.add(page.srcKey)
                }
            }
        }
    }

    override fun info(message: String) = println("INFO: MetadataController: $message")
    override fun warn(message: String) = println("WARN: MetadataController: $message")
    override fun error(message: String) = println("ERROR: MetadataController: $message")
}