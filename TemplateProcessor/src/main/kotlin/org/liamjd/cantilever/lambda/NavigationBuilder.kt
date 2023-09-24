package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.LambdaLogger
import kotlinx.serialization.json.Json
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.models.PostList
import org.liamjd.cantilever.models.PostMeta
import org.liamjd.cantilever.models.PostMetadata
import org.liamjd.cantilever.services.S3Service

context(LambdaLogger)
class NavigationBuilder(private val s3Service: S3Service) {

    /**
     * Return a useful map of [PostMeta] objects
     */
    fun getPostNavigationObjects(currentPost: PostMetadata, sourceBucket: String): Map<String, PostMeta?> {
        val navMap: MutableMap<String, PostMeta?> = mutableMapOf()
        if (s3Service.objectExists(S3_KEY.postsKey, sourceBucket)) {
            val postListJson = s3Service.getObjectAsString(S3_KEY.postsKey, sourceBucket)
            val postList = Json.decodeFromString<PostList>(postListJson)
            navMap["@prev"] = getPrevPost(currentPost, postList.posts)
            navMap["@first"] = getFirstPost(postList.posts)
            navMap["@last"] = getLastPost(postList.posts)
            navMap["@next"] = getNextPost(currentPost, postList.posts)
        } else {
            error("Unable to find posts.json for project; unable to build navigation")
        }
        return navMap.toMap()
    }

    /**
     * Posts are sorted most recent first, so the previous should be further down the list
     */
    private fun getPrevPost(currentPost: PostMetadata, posts: List<PostMeta>): PostMeta? {
        val currentPostInList = posts.find { it.url == currentPost.slug }
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
    private fun getNextPost(currentPost: PostMetadata, posts: List<PostMeta>): PostMeta? {
        val currentPostInList = posts.find { it.url == currentPost.slug }
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
     * Posts are sorted most recent first, so the earliest post is the last in the list
     */
    private fun getFirstPost(posts: List<PostMeta>): PostMeta? {
        return posts.lastOrNull()
    }

    /**
     * Posts are sorted most recent first, so the latest post is the first in the list
     */
    private fun getLastPost(posts: List<PostMeta>): PostMeta? {
        return posts.firstOrNull()
    }
}