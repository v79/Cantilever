package org.liamjd.cantilever.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias SrcKey = String // an S3 bucket object key

@Serializable
sealed class ContentNode {
    abstract val srcKey: SrcKey
    abstract val lastUpdated: Instant

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

    @Serializable
    @SerialName("post")
    data class PostNode(
        override val srcKey: String,
        override val lastUpdated: Instant = Clock.System.now(),
        val title: String,
        val templateKey: String,
        val date: LocalDate,
        val slug: String,
        val attributes: Map<String, String>
    ) : ContentNode() {
        var next: SrcKey? = null
        var prev: SrcKey? = null
    }

    @Serializable
    @SerialName("template")
    data class TemplateNode(
        override val srcKey: String,
        override val lastUpdated: Instant = Clock.System.now(),
        val title: String,
        val sections: List<String> = emptyList(),
    ) : ContentNode()

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

class FolderNotEmptyException(message: String) : Exception(message)

@Serializable
class ContentTree {

    val items: MutableList<ContentNode> = mutableListOf()
    val templates: MutableList<ContentNode.TemplateNode> = mutableListOf()
    val statics: MutableList<ContentNode.StaticNode> = mutableListOf()

    fun insert(node: ContentNode) {
        when (node) {
            is ContentNode.FolderNode -> insertFolder(node)
            is ContentNode.PageNode -> insertPage(node)
            is ContentNode.PostNode -> insertPost(node)
            is ContentNode.TemplateNode -> insertTemplate(node)
            is ContentNode.StaticNode -> insertStatic(node)
        }
    }

    fun insertAll(nodes: List<ContentNode>) {
        nodes.forEach { insert(it) }
    }

    fun insertTemplate(templateNode: ContentNode.TemplateNode) {
        templates.add(templateNode)
    }

    fun insertStatic(staticNode: ContentNode.StaticNode) {
        statics.add(staticNode)
    }

    fun deleteTemplate(templateNode: ContentNode.TemplateNode) {
        templates.remove(templateNode)
    }

    fun insertFolder(folderNode: ContentNode.FolderNode) {
        items.add(folderNode)
    }

    fun deleteFolder(folderNode: ContentNode.FolderNode) {
        if (folderNode.children.isNotEmpty()) {
            throw FolderNotEmptyException("Cannot delete a folder that contains children")
        }
        items.remove(folderNode)
    }

    fun insertPage(page: ContentNode.PageNode) {
        items.add(page)
        val parent = items.find { it.srcKey == page.parent } as ContentNode.FolderNode?
        parent?.children?.add(page.srcKey).also {
            if (page.isRoot) {
                parent?.indexPage = page.srcKey
            }
        }
    }

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

    fun updatePost(post: ContentNode.PostNode) {
        val existing = items.find { it.srcKey == post.srcKey } as ContentNode.PostNode?
        if (existing != null) {
            deletePost(existing)
            insertPost(post)
        }
    }

    fun deletePage(page: ContentNode.PageNode) {
        items.remove(page)
        val parent = items.find { it.srcKey == page.parent } as ContentNode.FolderNode?
        parent?.children?.remove(page.srcKey)
    }

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

    fun deletePost(post: ContentNode.PostNode) {
        items.remove(post)
        val previous =
            items.filterIsInstance<ContentNode.PostNode>().filter { it.date < post.date }.maxByOrNull { it.date }
        val next =
            items.filterIsInstance<ContentNode.PostNode>().filter { it.date > post.date }.minByOrNull { it.date }
        previous?.next = next?.srcKey
        next?.prev = previous?.srcKey
    }

    fun getNextPost(postKey: SrcKey): ContentNode.PostNode? {
        val post = items.find { it.srcKey == postKey } as ContentNode.PostNode?
        return post?.next?.let { nextKey ->
            items.find { it.srcKey == nextKey } as ContentNode.PostNode?
        }
    }

    fun getPrevPost(postKey: SrcKey): ContentNode.PostNode? {
        val post = items.find { it.srcKey == postKey } as ContentNode.PostNode?
        return post?.prev?.let { prevKey ->
            items.find { it.srcKey == prevKey } as ContentNode.PostNode?
        }
    }

    private fun getNode(srcKey: SrcKey): ContentNode? {
        return items.find { it.srcKey == srcKey }
    }

    fun getPagesForTemplate(templateKey: String): List<ContentNode.PageNode> {
        return items.filterIsInstance<ContentNode.PageNode>().filter { it.templateKey == templateKey }
    }

    fun getPostsForTemplate(templateKey: String): List<ContentNode.PostNode> {
        return items.filterIsInstance<ContentNode.PostNode>().filter { it.templateKey == templateKey }
    }
}
