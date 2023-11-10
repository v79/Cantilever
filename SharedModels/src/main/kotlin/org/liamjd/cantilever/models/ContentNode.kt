package org.liamjd.cantilever.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    abstract val lastUpdated: Instant

    /**
     * A folder is a node in the tree which contains other nodes
     */
    @Serializable
    @SerialName("folder")
    data class FolderNode(
        override val srcKey: String,
        override val lastUpdated: Instant = Clock.System.now(),
        val children: MutableList<SrcKey> = mutableListOf(),
        var indexPage: SrcKey? = null,
    ) : ContentNode() {
        val count: Int
            get() = children.size
    }

    /**
     * A page is a node in the tree which represents a page on the site. A page belongs to a folder, and may be the index page for that folder
     */
    @Serializable
    @SerialName("page")
    data class PageNode(
        override val srcKey: String,
        override val lastUpdated: Instant = Clock.System.now(),
        val title: String,
        val templateKey: String,
        val url: String,
        val isRoot: Boolean,
        val attributes: Map<String, String>,
        val sections: Map<String, String>,
    ) : ContentNode() {
        var parent: SrcKey? = null
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
        val templateKey: String,
        val date: LocalDate,
        val slug: String,
        @EncodeDefault
        val attributes: Map<String, String> = emptyMap()
    ) : ContentNode() {

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
        var next: SrcKey? = null
        var prev: SrcKey? = null
    }

    /**
     * A template is a node in the tree which represents a handlebars template. It is used to generate the final HTML for a page
     */
    @Serializable
    @SerialName("template")
    data class TemplateNode(
        override val srcKey: String,
        override val lastUpdated: Instant = Clock.System.now(),
        val title: String,
        val sections: List<String> = emptyList(),
    ) : ContentNode()

    /**
     * A static is a node in the tree which represents a static file, such as a .css file. It is copied to the generated bucket without modification
     */
    @Serializable
    @SerialName("static")
    data class StaticNode(
        override val srcKey: String,
        override val lastUpdated: Instant = Clock.System.now(),
    ) : ContentNode(

    ) {
        var fileType: String? = null
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
        templates.add(templateNode)
    }

    /**
     * Insert a static file, such as a .css file or image, into the tree
     */
    fun insertStatic(staticNode: ContentNode.StaticNode) {
        statics.add(staticNode)
    }

    /**
     * Delete a template from the tree.
     */
    fun deleteTemplate(templateNode: ContentNode.TemplateNode) {
        templates.remove(templateNode)
    }

    /**
     * Insert a folder into the tree.
     */
    fun insertFolder(folderNode: ContentNode.FolderNode) {
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
     * Insert a page into the tree. It also attempts to associate the page with its parent folder.
     */
    fun insertPage(page: ContentNode.PageNode) {
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
     *
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
            existing.parent?.let { existingParent ->
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
     * Find a node in the tree, based on its srcKey
     */
    private fun getNode(srcKey: SrcKey): ContentNode? {
        return items.find { it.srcKey == srcKey }
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
}