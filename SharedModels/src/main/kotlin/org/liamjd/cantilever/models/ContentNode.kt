package org.liamjd.cantilever.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.*
import org.liamjd.cantilever.common.S3_KEY
import kotlin.time.ExperimentalTime

/**
 * A SrcKey is a String representation of the S3 bucket object key
 * It is used to uniquely identify a file in the bucket
 **/
typealias SrcKey = String // an S3 bucket object key

/**
 * A ContentNode is a representation of a file in the S3 bucket. It is used to build the ContentTree, which is used to generate the metadata representation of the site
 */
@Serializable
@OptIn(ExperimentalSerializationApi::class)
sealed class ContentNode {
    abstract val srcKey: SrcKey
    @OptIn(ExperimentalTime::class)
    abstract val lastUpdated: Instant
    abstract val url: String

    /**
     * A folder is a node in the tree which contains other nodes
     */
    @Serializable
    @SerialName("folder")
    data class FolderNode @OptIn(ExperimentalTime::class) constructor(
        override val srcKey: SrcKey,
        override val lastUpdated: Instant = Clock.System.now(),
        val children: MutableList<SrcKey> = mutableListOf(),
        var indexPage: SrcKey? = null,
    ) : ContentNode() {
        val count: Int
            get() = children.size

        override val url = srcKey.removePrefix(S3_KEY.pagesPrefix)
    }

    /**
     * A page is a node in the tree which represents a page on the site. A page belongs to a folder, and may be the index page for that folder
     */
    @Serializable
    @SerialName("page")
    data class PageNode(
        override val srcKey: SrcKey,
        override val lastUpdated: Instant = Clock.System.now(),
        val title: String,
        val templateKey: SrcKey,
        val slug: String,
        val isRoot: Boolean,
        val attributes: Map<String, String>,
        val sections: Map<String, String>,
        var parent: SrcKey = ""
    ) : ContentNode() {

        @Transient
        val type: String? =
            null // this is used by the front end to determine if it's a page or a post, but is  not needed here

        // I might actually want to generate both index.html and the slug version of the file but for now let's just do index.html or slug
        override val url: String
            get() {
                // intended url would be www.cantilevers.org/parentFolder/slug
                // but parent looks like wwww.cantilevers.org/sources/pages/parentFolder
                val parentFolder = parent.replaceFirst("/sources/pages","")
                return if (isRoot) {
                    "${parentFolder}/index.html"
                } else {
                    "$parentFolder/$slug"
                }
            }
    }

    /**
     * ---
     * title: Exploring authentication of routes
     * templateKey: sources/templates/post.html.hbs
     * date: 2023-01-31
     * slug: exploring-route-authentication
     * ---
     */
    @Serializable
    @SerialName("post")
    data class PostNode(
        override val lastUpdated: Instant = Clock.System.now(),
        val title: String,
        val templateKey: SrcKey,
        val date: LocalDate,
        val slug: String,
        @EncodeDefault
        val attributes: Map<String, String> = emptyMap()
    ) : ContentNode() {

        // I'd like to make this configurable, but let's try a folder structure based on the date
        override val url: String
            get() {
                val year = date.year.toString()
                val month = date.monthNumber.toString().padStart(2, '0')
                return "posts/$year/$month/$slug"
            }

        /**
         * secondary constructor for when we know the srcKey
         */
        constructor(
            srcKey: String,
            title: String,
            templateKey: String,
            date: LocalDate,
            slug: String,
            attributes: Map<String, String> = emptyMap()
        ) : this(
            title = title,
            templateKey = templateKey,
            date = date,
            slug = slug,
            attributes = attributes
        ) {
            this.srcKey = srcKey
        }

        /**
         * secondary constructor to convert a temporary [PostYaml] object into a [PostNode]
         */
        internal constructor(postYaml: PostYaml) : this(
            title = postYaml.title,
            templateKey = postYaml.templateKey,
            date = postYaml.date,
            slug = postYaml.slug,
            attributes = postYaml.attributes
        )

        override lateinit var srcKey: String
        var body: String = ""
        var next: SrcKey? = null
        var prev: SrcKey? = null
    }

    /**
     * A template is a node in the tree which represents a handlebars template. It is used to generate the final HTML for a page
     */
    @Serializable
    @SerialName("template")
    data class TemplateNode(
        override val srcKey: SrcKey,
        override val lastUpdated: Instant = Clock.System.now(),
        val title: String,
        val sections: List<String> = emptyList(),
    ) : ContentNode() {
        override val url = "" // irrelevant for templates
        var body: String = ""
    }

    /**
     * A static is a node in the tree which represents a static file, such as a .css file. It is copied to the generated bucket without modification
     */
    @Serializable
    @SerialName("static")
    data class StaticNode(
        override val srcKey: SrcKey,
        override val lastUpdated: Instant = Clock.System.now(),
    ) : ContentNode(
    ) {
        var fileType: String? = null
        override val url = srcKey.removePrefix(S3_KEY.sources) + "/"
    }

    /**
     * An image is a node in the tree which represents an image file. It is copied to the generated bucket without modification
     */
    @Serializable
    @SerialName("image")
    data class ImageNode(
        override val srcKey: SrcKey,
        override val lastUpdated: Instant = Clock.System.now(),
    ) : ContentNode(
    ) {
        var contentType: String? = null
        override val url = "" // irrelevant for images, or rather, handled differently because of the image resolutions

        // should I be recording image resolutions here?
    }
}

/**
 * Exception thrown when attempting to delete a folder which contains children
 */
class FolderNotEmptyException(message: String) : Exception(message)

/**
 * This is a utility class because [ContentNode.PostNode] requires the `srcKey` property, which will not exist in the yaml frontmatter for a post.
 * I had hoped to avoid this duplication of classes, but I can't see a way around it because making `srcKey` abstract in [ContentNode] does not work.
 */
@Serializable
internal class PostYaml(
    val title: String,
    val templateKey: String,
    val date: LocalDate,
    val slug: String,
    val attributes: Map<String, String> = emptyMap()
)

/**
 * A ContentTree is a representation of the entire site, as a tree of ContentNodes
 * It is split into items (pages, folders and posts), templates and statics
 */
@Serializable
class ContentTree {

    val items: MutableList<ContentNode> = mutableListOf()
    val templates: MutableList<ContentNode.TemplateNode> = mutableListOf()
    val statics: MutableList<ContentNode.StaticNode> = mutableListOf()
    val images: MutableList<ContentNode.ImageNode> = mutableListOf()

    val postCount = items.filterIsInstance<ContentNode.PostNode>().size // FIXME: not working as I expected
    val pageCount = items.filterIsInstance<ContentNode.PageNode>().size // FIXME: not working as I expected

    /**
     * Insert a node into the tree. It performs the appropriate insert based on the type of node.
     */
    fun insert(node: ContentNode) {
        when (node) {
            is ContentNode.FolderNode -> insertFolder(node)
            is ContentNode.PageNode -> insertPage(node)
            is ContentNode.PostNode -> insertPost(node)
            is ContentNode.TemplateNode -> insertTemplate(node)
            is ContentNode.StaticNode -> insertStatic(node)
            is ContentNode.ImageNode -> insertImage(node)
        }
    }

    /**
     * Insert a list of nodes into the tree.
     */
    fun insertAll(nodes: List<ContentNode>) {
        nodes.forEach { insert(it) }
    }

    /**
     * Insert a Template into the tree
     */
    fun insertTemplate(templateNode: ContentNode.TemplateNode) {
        if (templates.contains(templateNode)) {
            templates.remove(templateNode)
        }
        templates.add(templateNode)
    }

    /**
     * Insert a static file, such as a .css file or image, into the tree
     */
    fun insertStatic(staticNode: ContentNode.StaticNode) {
        if (statics.contains(staticNode)) {
            statics.remove(staticNode)
        }
        statics.add(staticNode)
    }

    /**
     * Insert an image into the tree
     */
    fun insertImage(imageNode: ContentNode.ImageNode) {
        if (images.contains(imageNode)) {
            images.remove(imageNode)
        }
        images.add(imageNode)
    }

    /**
     * Delete a template from the tree.
     */
    fun deleteTemplate(templateNode: ContentNode.TemplateNode) {
        println("Removing template ${templateNode.srcKey} from ContentTree")
        templates.removeIf { it.srcKey == templateNode.srcKey }
    }

    /**
     * Delete an image from the tree
     */
    fun deleteImage(imageNode: ContentNode.ImageNode) {
        println("Removing image ${imageNode.srcKey} from ContentTree")
        images.removeIf { it.srcKey == imageNode.srcKey }
    }

    /**
     * Insert a folder into the tree.
     */
    fun insertFolder(folderNode: ContentNode.FolderNode) {
        // not sure I want to delete first, but I don't want duplicates either
        items.add(folderNode)
    }

    /**
     * Delete a folder from the tree. Throws an exception if the folder contains children.
     */
    fun deleteFolder(folderNode: ContentNode.FolderNode) {
        if (folderNode.children.isNotEmpty()) {
            throw FolderNotEmptyException("Cannot delete a folder that contains children")
        }
        items.remove(folderNode)
    }

    /**
     * Update a folder from the tree
     */
    fun updateFolder(folderNode: ContentNode.FolderNode) {
        val existing = items.find { it.srcKey == folderNode.srcKey } as ContentNode.FolderNode?
        if (existing != null) {
            items.remove(existing)
            items.add(folderNode)
        }
    }

    /**
     * Insert a page into the tree. It also attempts to associate the page with its parent folder.
     */
    fun insertPage(page: ContentNode.PageNode) {
        // check if page exists first??
        items.add(page)
        val parent = items.find { it.srcKey == page.parent } as ContentNode.FolderNode?
        parent?.children?.add(page.srcKey).also {
            if (page.isRoot) {
                parent?.indexPage = page.srcKey
            }
        }
    }

    /**
     *  Insert a page into the tree, and associate it with the specified parent folder.
     */
    fun insertPage(page: ContentNode.PageNode, folder: ContentNode.FolderNode) {
        // check if page exists first??
        items.add(page)
        folder.children.add(page.srcKey)
        page.parent = folder.srcKey
        if (page.isRoot) {
            folder.indexPage = page.srcKey
        }
    }

    /**
     * Update a page in the tree. This is used when a page is edited and saved.
     * This does NOT update the parent folder, so if the page has been moved, the parent folder must be updated separately, or call reparentPage()
     */
    fun updatePage(page: ContentNode.PageNode) {
        val existing = items.find { it.srcKey == page.srcKey } as ContentNode.PageNode?
        if (existing != null) {
            val existingParent = existing.parent
            page.parent = existingParent
            items.remove(existing)
            items.add(page)
        }
    }

    /**
     * Update a post in the tree. This does a delete then insert, so the prev and next links are updated.
     */
    fun updatePost(post: ContentNode.PostNode) {
        val existing = items.find { it.srcKey == post.srcKey } as ContentNode.PostNode?
        if (existing != null) {
            deletePost(existing)
            insertPost(post)
        }
    }

    /**
     * Update a template in the tree. This does a delete then insert
     */
    fun updateTemplate(template: ContentNode.TemplateNode) {
        val existing = templates.find { it.srcKey == template.srcKey }
        if (existing != null) {
            deleteTemplate(existing)
            insertTemplate(template)
        }
    }

    /**
     * Delete a page from the tree. This also updates the parent folder.
     */
    fun deletePage(page: ContentNode.PageNode) {
        items.remove(page)
        val parent = items.find { it.srcKey == page.parent } as ContentNode.FolderNode?
        parent?.children?.remove(page.srcKey)
    }

    /**
     * Move a page from one folder to another
     */
    fun reparentPage(pageNode: ContentNode.PageNode, newParent: ContentNode.FolderNode) {
        val existing = items.find { it.srcKey == pageNode.srcKey } as ContentNode.PageNode?
        if (existing != null) {
            existing.parent.let { existingParent ->
                val existingParentNode = getNode(existingParent) as ContentNode.FolderNode?
                existingParentNode?.children?.remove(pageNode.srcKey)
                existingParentNode?.indexPage = null
            }
            pageNode.parent = newParent.srcKey
            if (pageNode.isRoot) {
                newParent.indexPage = pageNode.srcKey
            }
            newParent.children.add(pageNode.srcKey)
        }
    }

    /**
     * Insert a post into the tree. This also updates the prev and next links for all sibling posts.
     */
    fun insertPost(post: ContentNode.PostNode) {
        items.add(post)
        val previous =
            items.filterIsInstance<ContentNode.PostNode>().filter { it.date < post.date }.maxByOrNull { it.date }
        val next =
            items.filterIsInstance<ContentNode.PostNode>().filter { it.date > post.date }.minByOrNull { it.date }
        post.prev = previous?.srcKey
        post.next = next?.srcKey
        previous?.next = post.srcKey
        next?.prev = post.srcKey
    }

    /**
     * Delete a post from the tree. This also updates the prev and next links for all sibling posts.
     */
    fun deletePost(post: ContentNode.PostNode) {
        items.remove(post)
        val previous =
            items.filterIsInstance<ContentNode.PostNode>().filter { it.date < post.date }.maxByOrNull { it.date }
        val next =
            items.filterIsInstance<ContentNode.PostNode>().filter { it.date > post.date }.minByOrNull { it.date }
        previous?.next = next?.srcKey
        next?.prev = previous?.srcKey
    }

    /**
     * Delete a post from the tree. This also updates the prev and next links for all sibling posts.
     */
    fun deletePost(srcKey: SrcKey) {
        val post = getNode(srcKey) as ContentNode.PostNode?
        post?.let { deletePost(it) }
    }

    /**
     * Get the next post in the tree, based on the current post's srcKey
     */
    fun getNextPost(postKey: SrcKey): ContentNode.PostNode? {
        val post = items.find { it.srcKey == postKey } as ContentNode.PostNode?
        return post?.next?.let { nextKey ->
            items.find { it.srcKey == nextKey } as ContentNode.PostNode?
        }
    }

    /**
     * Get the previous post in the tree, based on the current post's srcKey
     */
    fun getPrevPost(postKey: SrcKey): ContentNode.PostNode? {
        val post = items.find { it.srcKey == postKey } as ContentNode.PostNode?
        return post?.prev?.let { prevKey ->
            items.find { it.srcKey == prevKey } as ContentNode.PostNode?
        }
    }

    /**
     * Find a node in the tree, based on its srcKey.
     */
    fun getNode(srcKey: SrcKey): ContentNode? {
        return items.find { it.srcKey == srcKey }
    }

    fun getTemplate(srcKey: SrcKey): ContentNode.TemplateNode? {
        return templates.find { it.srcKey == srcKey }
    }

    /**
     * Find all posts which have the specified templateKey
     */
    fun getPagesForTemplate(templateKey: String): List<ContentNode.PageNode> {
        return items.filterIsInstance<ContentNode.PageNode>().filter { it.templateKey == templateKey }
    }

    /**
     * Find all posts which have the specified templateKey
     */
    fun getPostsForTemplate(templateKey: String): List<ContentNode.PostNode> {
        return items.filterIsInstance<ContentNode.PostNode>().filter { it.templateKey == templateKey }
    }

    /**
     * Clear the tree in advance of re-loading all the data from the S3 bucket
     */
    fun clear() {
        items.clear()
        templates.clear()
        statics.clear()
        images.clear()
    }
}
