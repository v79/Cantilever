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


    fun getNavigationObjects(currentPost: PostMetadata, sourceBucket: String): Map<String, PostMeta?> {
        val navMap: MutableMap<String,PostMeta?> = mutableMapOf()
        val postList: PostList
        if (s3Service.objectExists(S3_KEY.postsKey, sourceBucket)) {
            val postListJson = s3Service.getObjectAsString(S3_KEY.postsKey, sourceBucket)
            postList = Json.decodeFromString<PostList>(postListJson)
            val prevPost = getPrevPost(currentPost, postList.posts)
            navMap["@prev"] = prevPost
        } else {
            error("Unable to find posts.json for project; unable to build navigation")
        }
        return navMap.toMap()
    }

    private fun getPrevPost(currentPost: PostMetadata, posts: List<PostMeta>): PostMeta? {
        val currentPostInList = posts.find { it.srcKey == currentPost.slug }
        return if (currentPostInList != null) {
            val index = posts.indexOf(currentPostInList)
            if (index > 0) {
                posts[index - 1]
            } else {
                null
            }
        } else {
            error("Could not find post ${currentPost.slug} in list of posts!")
            null
        }
    }

    private fun getFirstPost(postList: PostList): PostMeta? {
        return postList.posts.firstOrNull()
    }

    private fun getLastPost(postList: PostList): PostMeta? {
        return postList.posts.lastOrNull()
    }
}