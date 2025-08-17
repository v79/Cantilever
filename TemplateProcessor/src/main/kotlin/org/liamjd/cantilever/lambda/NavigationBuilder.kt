package org.liamjd.cantilever.lambda

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.liamjd.cantilever.common.SOURCE_TYPE
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.services.DynamoDBService
import java.time.format.DateTimeFormatter


class NavigationBuilder(private val dynamoDBService: DynamoDBService, private val domain: String) {

    // Sate is stored as a string in the format "yyyy-MM-dd"
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Return a useful map of [ContentNode.PostNode] objects
     */
    fun getPostNavigationObjects(
        currentPost: ContentNode.PostNode
    ): Map<String, ContentNode.PostNode?> {
        val navMap: MutableMap<String, ContentNode.PostNode?> = mutableMapOf()
        val postList = filterPosts()

        navMap["@prev"] = getPrevPost(currentPost)
        navMap["@first"] = getFirstPost(postList)
        navMap["@last"] = getLastPost(postList)
        navMap["@next"] = getNextPost(currentPost, postList)
        return navMap.toMap()
    }

    /**
     * Posts are sorted most-recent first, so the previous should be further down the list
     */
    private fun getPrevPost(
        currentPost: ContentNode.PostNode
    ): ContentNode.PostNode? {
        // To find the previous post, we need to query the database for all posts with a date before the current post
        // sorted by date, descending, and then return only the first.
        val currentPostDateAsString = dateFormatter.format(
            currentPost.date.atStartOfDayIn(TimeZone.currentSystemDefault())
                .toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime()
        )
        println("Looking for previous post before date: $currentPostDateAsString")
        runBlocking {
            dynamoDBService.getKeyListMatchingAttributes(domain, SOURCE_TYPE.Posts,
                mapOf("date" to currentPostDateAsString))

        }

        return null
    }

    /**
     * Posts are sorted most-recent first, so the previous should be further up the list
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
        }
    }

    /**
     * Return a useful map of [ContentNode.PostNode] objects, sorted by date
     */
    fun filterPosts(): List<ContentNode.PostNode> {
//        return contentTree.items.filterIsInstance<ContentNode.PostNode>().sortedByDescending { it.date }
        return emptyList()
    }

    /**
     * Return a useful map of [ContentNode.PageNode] objects, sorted by lastUpdated date
     */
    fun filterPages(): List<ContentNode.PageNode> {
//        return contentTree.items.filterIsInstance<ContentNode.PageNode>().sortedByDescending { it.lastUpdated }
        return emptyList()
    }

    /**
     * Posts are sorted most-recent first, so the earliest post is the last in the list
     */
    private fun getFirstPost(posts: List<ContentNode.PostNode>): ContentNode.PostNode? {
        return posts.lastOrNull()
    }

    /**
     * Posts are sorted most-recent first, so the latest post is the first in the list
     */
    private fun getLastPost(posts: List<ContentNode.PostNode>): ContentNode.PostNode? {
        return posts.firstOrNull()
    }
}
