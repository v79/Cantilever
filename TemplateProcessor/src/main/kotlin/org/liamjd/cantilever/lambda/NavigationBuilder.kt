package org.liamjd.cantilever.lambda

import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.ContentTree

class NavigationBuilder(private val contentTree: ContentTree) {

    /**
     * Return a useful map of [ContentNode.PostNode] objects
     */
    fun getPostNavigationObjects(
        currentPost: ContentNode.PostNode
    ): Map<String, ContentNode.PostNode?> {
        val navMap: MutableMap<String, ContentNode.PostNode?> = mutableMapOf()
        val postList = filterPosts()

        navMap["@prev"] = getPrevPost(currentPost, postList)
        navMap["@first"] = getFirstPost(postList)
        navMap["@last"] = getLastPost(postList)
        navMap["@next"] = getNextPost(currentPost, postList)
        return navMap.toMap()
    }

    /**
     * Posts are sorted most recent first, so the previous should be further down the list
     */
    private fun getPrevPost(
        currentPost: ContentNode.PostNode,
        posts: List<ContentNode.PostNode>
    ): ContentNode.PostNode? {
        val currentPostInList = posts.find { it.slug == currentPost.slug }
        return if (currentPostInList != null) {
            val index = posts.indexOf(currentPostInList)
            if (index < (posts.size - 1)) {
                posts[index + 1]
            } else {
                null
            }
        } else {
            error("Could not find post ${currentPost.slug} in list of posts!")
            null
        }
    }

    /**
     * Posts are sorted most recent first, so the previous should be further up the list
     */
    private fun getNextPost(
        currentPost: ContentNode.PostNode,
        posts: List<ContentNode.PostNode>
    ): ContentNode.PostNode? {
        val currentPostInList = posts.find { it.slug == currentPost.slug }
        return if (currentPostInList != null) {
            val index = posts.indexOf(currentPostInList)
            if (index == 0) {
                null
            } else {
                posts[index - 1]
            }
        } else {
            error("Could not find post ${currentPost.slug} in list of posts!")
            null
        }
    }

    /**
     * Return a useful map of [ContentNode.PostNode] objects, sorted by date
     */
    fun filterPosts(): List<ContentNode.PostNode> {
        return contentTree.items.filterIsInstance<ContentNode.PostNode>().sortedByDescending { it.date }
    }

    /**
     * Return a useful map of [ContentNode.PageNode] objects, sorted by lastUpdated date
     */
    fun filterPages(): List<ContentNode.PageNode> {
        return contentTree.items.filterIsInstance<ContentNode.PageNode>().sortedByDescending { it.lastUpdated }
    }

    /**
     * Posts are sorted most recent first, so the earliest post is the last in the list
     */
    private fun getFirstPost(posts: List<ContentNode.PostNode>): ContentNode.PostNode? {
        return posts.lastOrNull()
    }

    /**
     * Posts are sorted most recent first, so the latest post is the first in the list
     */
    private fun getLastPost(posts: List<ContentNode.PostNode>): ContentNode.PostNode? {
        return posts.firstOrNull()
    }
}
