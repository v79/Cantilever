package org.liamjd.cantilever.api.controllers

import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.api.models.RawJsonString
import org.liamjd.cantilever.models.Post
import org.liamjd.cantilever.models.PostList
import org.liamjd.cantilever.models.Template
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.impl.extractPostMetadata
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

class ProjectController(val sourceBucket: String,) : KoinComponent {

    private val s3Service: S3Service by inject()

    fun getPosts(request: Request<Unit>) : ResponseEntity<APIResult<RawJsonString>> {
        println("ProjectController: Retrieving all posts")
        return if(s3Service.objectExists(postsKey,sourceBucket)) {
            val postListJson = s3Service.getObjectAsString(postsKey,sourceBucket)
            ResponseEntity.ok(body = APIResult.JsonSuccess(jsonString = RawJsonString(postListJson)))
        } else {
            ResponseEntity.serverError(body = APIResult.Error(message = "Cannot find file '$postsKey' in bucket '$sourceBucket'. To regenerate from sources, call PUT /project/posts/rebuild"))
        }
    }

    fun getPages() {

    }

    fun getTemplates() {

    }

    fun rebuildPostList(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val posts = s3Service.listObjects(postsPrefix,sourceBucket)
        println("ProjectController: Rebuilding all posts from sources in $postsPrefix. ${posts.keyCount()} posts found.")
        var filesProcessed = 0
        if(posts.hasContents()) {
            val list = mutableListOf<Post>()
            posts.contents().forEach { obj ->
                if(obj.key().endsWith(".md")) {
                    println("Extracting metadata from file '${obj.key()}'")
                    val markdownSource = s3Service.getObjectAsString(obj.key(),sourceBucket)
                    val postMetadata  = extractPostMetadata(obj.key(),markdownSource)
                    val templateKey = templateSourcesKey + postMetadata.template + ".html.hbs"
                    val template = try {
                        val lastModified = obj.lastModified().toKotlinInstant()
                        Template(templateKey, lastModified)
                    } catch (nske: NoSuchKeyException) {
                        println("Cannot find template file '$templateKey'; aborting for file '${obj.key()}'")
                        return@forEach
                    }
                    list.add(Post(
                        title = postMetadata.title,
                        srcKey = obj.key(),
                        url = postMetadata.slug,
                        date =  postMetadata.date,
                        lastUpdated = postMetadata.lastModified,
                        templateKey = template.key
                    ))
                    filesProcessed++
                } else {
                    println("Skipping non-markdown file '${obj.key()}'")
                }
            }
            list.sortByDescending { it.date }
            val postList = PostList(list,filesProcessed)
            val listJson = Json.encodeToString(PostList.serializer(),postList)
            println("Saving PostList JSON file (${listJson.length} bytes)")
            s3Service.putObject(postsKey,sourceBucket,listJson,"application/json")
        } else {
            return ResponseEntity.serverError(body = APIResult.Error(message = "No source files found in $sourceBucket which match the requirements to build a project structure file."))
        }
        return ResponseEntity.ok(body = APIResult.Success("Written new '${postsKey}' with $filesProcessed markdown files processed"))

    }

    fun rebuildPageList() {

    }

    fun rebuildTemplateList() {

    }

    companion object {
        const val postsKey = "generated/posts.json"
        const val pagesKey = "generated/pages.json"
        const val templatesKey = "generated/templates.json"
        const val postsPrefix = "sources/posts/"
        const val templateSourcesKey = "templates/"
    }
}