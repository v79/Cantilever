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
        val sourcesFolder = domain + "/" + S3_KEY.sources
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
        val folders = mutableListOf<ContentNode.FolderNode>()
        // TODO: this only returns 1000 items, need to paginate
        val items = s3Service.listObjects(sourcesFolder, sourceBucket)
        if (items.hasContents()) {
            // Special cases are 'sources/', 'sources/posts/', 'sources/pages/' - these should be ignored
            val ignoreList =
                listOf(sourcesFolder, postsFolder, pagesFolder)
            items.contents().forEach {
                if (it.key() !in ignoreList) {
                    info("Processing ${it.key()}")
                    if (it.key().endsWith("/") && it.key() !in ignoreList) {
                        val folder = ContentNode.FolderNode(it.key())
                        info("Found folder ${it.key()} and created node $folder")
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
                            pagesCount +=
                                filesProcessed++
                        }
                        if (it.key().startsWith(templatesFolder) && it.key() != templatesFolder) {
                            val template = buildTemplateNode(it.key())
                            templates.add(template)
                            templatesCount++
                            filesProcessed++
                        }
                        if (it.key().startsWith(staticsFolder) && it.key() != staticsFolder) {
                            val static = ContentNode.StaticNode(it.key())
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
                    val parent =
                        contentTree.items.find { node -> node.srcKey == page.parent + "/" } as ContentNode.FolderNode?
                    parent?.children?.add(page.srcKey)
                    if (page.isRoot) {
                        parent?.indexPage = page.srcKey
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
                "Rebuild node database for $domain with $filesProcessed ($postsCount posts, $pagesCount pages, $imagesCount images, $templatesCount templates, $staticsCount statics)"
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

    override fun info(message: String) = println("INFO: MetadataController: $message")
    override fun warn(message: String) = println("WARN: MetadataController: $message")
    override fun error(message: String) = println("ERROR: MetadataController: $message")
}