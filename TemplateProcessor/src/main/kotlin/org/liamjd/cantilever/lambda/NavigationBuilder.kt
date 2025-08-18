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

/**
 * Queries the DynamoDB database to build a navigation map for a given post.
 */
class NavigationBuilder(private val dynamoDBService: DynamoDBService, private val domain: String) {

    // Sate is stored as a string in the format "yyyy-MM-dd"
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Return a useful map of [ContentNode.PostNode] objects. It will contain the following keys:
     * - `@prev`: The previous post, if it exists
     * - `@first`: The first post in the list, if it exists
     * - `@last`: The last post in the list, if it exists
     * - `@next`: The next post, if it exists
     */
    fun getPostNavigationObjects(
        currentPost: ContentNode.PostNode
    ): Map<String, ContentNode.PostNode?> {
        val navMap: MutableMap<String, ContentNode.PostNode?> = mutableMapOf()

        navMap["@prev"] = getPrevPost(currentPost)
        navMap["@first"] = getFirstPost()
        navMap["@last"] = getLastPost()
        navMap["@next"] = getNextPost(currentPost)
        println("Navigation map built: $navMap")
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
        val prevPost = runBlocking {
            val prevPost = getNodeMatching(domain, currentPostDateAsString, "<")
            return@runBlocking prevPost
        }
        return prevPost
    }

    /**
     * Posts are sorted most-recent first, so the next should be further down the list
     */
    private fun getNextPost(
        currentPost: ContentNode.PostNode
    ): ContentNode.PostNode? {
        val currentPostDateAsString = dateFormatter.format(
            currentPost.date.atStartOfDayIn(TimeZone.currentSystemDefault())
                .toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime()
        )
        println("Looking for next post after date: $currentPostDateAsString")
        val prevPost = runBlocking {
            val prevPost = getNodeMatching(domain, currentPostDateAsString, ">")
            return@runBlocking prevPost
        }
        return prevPost
    }

    /**
     * Posts are sorted most-recent first, so the earliest post is the last in the list
     */
    private fun getFirstPost(): ContentNode.PostNode? {
        val firstPost = runBlocking {
            val firstPostKey = dynamoDBService.getFirstOrLastKeyFromLSI(
                projectDomain = domain,
                contentType = SOURCE_TYPE.Posts,
                lsiName = "Type-Date",
                operation = "first"
            )
            if (firstPostKey != null) {
                val firstPost = dynamoDBService.getContentNode(
                    projectDomain = domain,
                    contentType = SOURCE_TYPE.Posts,
                    srcKey = firstPostKey
                )
                firstPost
            } else {
                println("No first post found")
                null
            }

        }
        return firstPost as ContentNode.PostNode?
    }

    /**
     * Posts are sorted most-recent first, so the latest post is the first in the list
     */
    private fun getLastPost(): ContentNode.PostNode? {
        val lastPost = runBlocking {
            val lastPostKey = dynamoDBService.getFirstOrLastKeyFromLSI(
                projectDomain = domain,
                contentType = SOURCE_TYPE.Posts,
                lsiName = "Type-Date",
                operation = "last"
            )
            if (lastPostKey != null) {
                val lastPost = dynamoDBService.getContentNode(
                    projectDomain = domain,
                    contentType = SOURCE_TYPE.Posts,
                    srcKey = lastPostKey
                )
                lastPost
            } else {
                println("No last post found")
                null
            }

        }
        return lastPost as ContentNode.PostNode?
    }

    /**
     * Helper function to get a post node matching the given date and operation.
     * This is used to find the previous or next post based on the current post's date.
     */
    private suspend fun getNodeMatching(
        domain: String,
        currentPostDateAsString: String,
        operation: String
    ): ContentNode.PostNode? {
        val matchingKeys = dynamoDBService.getKeyListFromLSI(
            projectDomain = domain,
            contentType = SOURCE_TYPE.Posts,
            lsiName = "Type-Date",
            "date" to currentPostDateAsString,
            operation = operation,
            limit = 1,
            descending = true
        )
        return if (matchingKeys.isNotEmpty()) {
            val matchingPost = dynamoDBService.getContentNode(
                projectDomain = domain,
                contentType = SOURCE_TYPE.Posts,
                srcKey = matchingKeys.first()
            )
            if (matchingPost is ContentNode.PostNode) {
                matchingPost
            } else {
                println("Expected PostNode but got ${matchingPost?.javaClass?.simpleName}")
                null
            }
        } else {
            println("No previous post found for date: $currentPostDateAsString")
            null
        }
    }
}


