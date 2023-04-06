package org.liamjd.cantilever.api.controllers

import kotlinx.datetime.Clock
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.models.*
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.impl.extractPageModel
import org.liamjd.cantilever.services.impl.extractPostMetadata
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

/**
 * Manages all the project-wide configuration and json models
 * TODO: there is a lot of duplication in this class
 */
class ProjectController(val sourceBucket: String) : KoinComponent, APIController {

    private val s3Service: S3Service by inject()

    /**
     * Return a list of all the [Post]s
     */
    fun getPosts(request: Request<Unit>): ResponseEntity<APIResult<PostList>> {
        println("ProjectController: Retrieving all posts")
        return if (s3Service.objectExists(postsKey, sourceBucket)) {
            val postListJson = s3Service.getObjectAsString(postsKey, sourceBucket)
            val postList = Json.decodeFromString(PostList.serializer(), postListJson)
            val sorted = postList.posts.sortedBy { it.date }
            ResponseEntity.ok(body = APIResult.Success(value = PostList(count = sorted.size,posts = sorted, lastUpdated = postList.lastUpdated)))
        } else {
            ResponseEntity.notFound(body = APIResult.Error(message = "Cannot find file '$postsKey' in bucket '$sourceBucket'. To regenerate from sources, call PUT /project/posts/rebuild"))
        }
    }

    /**
     * Return a list of all the [Page]s
     */
    fun getPages(request: Request<Unit>): ResponseEntity<APIResult<PageList>> {
        println("ProjectController: Retrieving all pages")
        return if (s3Service.objectExists(pagesKey, sourceBucket)) {
            val pageListJson = s3Service.getObjectAsString(pagesKey, sourceBucket)
            val pageList = Json.decodeFromString(PageList.serializer(), pageListJson)
            ResponseEntity.ok(body = APIResult.Success(value = pageList))
        } else {
            ResponseEntity.notFound(body = APIResult.Error(message = "Cannot find file '$pagesKey' in bucket '$sourceBucket'. To regenerate from sources, call PUT /project/pages/rebuild"))
        }
    }

    /**
     * Return a list of all the [Template]s
     */
    fun getTemplates(request: Request<Unit>): ResponseEntity<APIResult<TemplateList>> {
        println("ProjectController: Retrieving templates pages")
        return if (s3Service.objectExists(templatesKey, sourceBucket)) {
            val templateListJson = s3Service.getObjectAsString(templatesKey, sourceBucket)
            val templateList = Json.decodeFromString(TemplateList.serializer(), templateListJson)
            ResponseEntity.ok(body = APIResult.Success(value = templateList))
        } else {
            ResponseEntity.notFound(body = APIResult.Error(message = "Cannot find file '$templatesKey' in bucket '$sourceBucket'. To regenerate from sources, call PUT /project/templates/rebuild"))
        }
    }

    /**
     * Rebuild the generated/posts.json file which contains the metadata for all the [Post]s in the project.
     */
    fun rebuildPostList(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val posts = s3Service.listObjects(postsPrefix, sourceBucket)
        println("ProjectController: Rebuilding all posts from sources in '$postsPrefix'. ${posts.keyCount()} posts found.")
        var filesProcessed = 0
        if (posts.hasContents()) {
            val list = mutableListOf<Post>()
            posts.contents().forEach { obj ->
                if (obj.key().endsWith(".md")) {
                    println("Extracting metadata from file '${obj.key()}'")
                    val markdownSource = s3Service.getObjectAsString(obj.key(), sourceBucket)
                    val postMetadata = extractPostMetadata(obj.key(), markdownSource)
                    val templateKey = templatesPrefix + postMetadata.template + ".html.hbs"
                    val template = try {
                        val lastModified = obj.lastModified().toKotlinInstant()
                        Template(templateKey, lastModified)
                    } catch (nske: NoSuchKeyException) {
                        println("Cannot find template file '$templateKey'; aborting for file '${obj.key()}'")
                        return@forEach
                    }
                    list.add(
                        Post(
                            title = postMetadata.title,
                            srcKey = obj.key(),
                            url = postMetadata.slug,
                            date = postMetadata.date,
                            lastUpdated = postMetadata.lastModified,
                            templateKey = template.key
                        )
                    )
                    filesProcessed++
                } else {
                    println("Skipping non-markdown file '${obj.key()}'")
                }
            }
            println("Sorting output")
            list.sortByDescending { it.date }
            val postList = PostList(posts = list, count = filesProcessed, lastUpdated = Clock.System.now())
            val listJson = Json.encodeToString(PostList.serializer(), postList)
            println("Saving PostList JSON file (${listJson.length} bytes)")
            s3Service.putObject(postsKey, sourceBucket, listJson, "application/json")
        } else {
            return ResponseEntity.serverError(body = APIResult.Error(message = "No source files found in $sourceBucket which match the requirements to build a $postsKey file."))
        }
        return ResponseEntity.ok(body = APIResult.Success("Written new '${postsKey}' with $filesProcessed markdown files processed"))

    }

    /**
     * Rebuild the generated/pages.json file which contains the metadata for all the [Pages]s in the project.
     */
    fun rebuildPageList(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val pages = s3Service.listObjects(pagesPrefix, sourceBucket)
        println("ProjectController: Rebuilding all pages from sources in '$pagesPrefix'. ${pages.keyCount()} pages found.")
        var filesProcessed = 0
        if (pages.hasContents()) {
            val list = mutableListOf<Page>()
            pages.contents().forEach { obj ->
                if (obj.key().endsWith(".md")) {
                    println("Extracting metadata from file '${obj.key()}'")
                    val markdownSource = s3Service.getObjectAsString(obj.key(), sourceBucket)
                    val pageModel = extractPageModel(obj.key(), markdownSource)
                    val templateKey = templatesPrefix + pageModel.templateKey + ".html.hbs"
                    val template = try {
                        val lastModified = obj.lastModified().toKotlinInstant()
                        Template(templateKey, lastModified)
                    } catch (nske: NoSuchKeyException) {
                        println("Cannot find template file '$templateKey'; aborting for file '${obj.key()}'")
                        return@forEach
                    }
                    list.add(
                        Page(
                            title = pageModel.title,
                            srcKey = pageModel.srcKey,
                            templateKey = pageModel.templateKey,
                            url = pageModel.url,
                            sectionKeys = pageModel.sections.keys,
                            attributeKeys = pageModel.attributes.keys
                        )
                    )
                    filesProcessed++
                } else {
                    println("Skipping non-markdown file '${obj.key()}'")
                }
            }
            list.sortByDescending { it.lastUpdated }
            val pageList = PageList(pages = list.toList(), count = filesProcessed, lastUpdated = Clock.System.now())
            val listJson = Json.encodeToString(PageList.serializer(), pageList)
            println("Saving PageList JSON file (${listJson.length} bytes)")
            s3Service.putObject(pagesKey, sourceBucket, listJson, "application/json")
        } else {
            return ResponseEntity.serverError(body = APIResult.Error(message = "No source files found in $sourceBucket which match the requirements to build a $postsKey file."))
        }
        return ResponseEntity.ok(body = APIResult.Success("Written new '$postsKey' with $filesProcessed markdown files processed"))
    }

    /**
     * Rebuild the generated/templates.json file which contains the metadata for all the [Templates]s in the project.
     */
    fun rebuildTemplateList(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val templates = s3Service.listObjects(templatesPrefix, sourceBucket)
        println("ProjectController: Rebuilding all templates from sources in '$pagesPrefix'. ${templates.keyCount()} templates found.")
        var filesProcessed = 0
        if (templates.hasContents()) {
            val list = mutableListOf<Template>()
            templates.contents().forEach { obj ->
                if (obj.key().endsWith(".hbs")) {
                    val lastModified = obj.lastModified().toKotlinInstant()
                    list.add(
                        Template(obj.key(), lastModified)
                    )
                    filesProcessed++
                } else {
                    println("Skipping non-hbs file '${obj.key()}'")
                }
            }
            list.sortByDescending { it.lastUpdated }
            val templateList = TemplateList(templates = list.toList(), count = filesProcessed, lastUpdated = Clock.System.now())
            val listJson = Json.encodeToString(TemplateList.serializer(), templateList)
            println("Saving PageList JSON file (${listJson.length} bytes)")
            s3Service.putObject(templatesKey, sourceBucket, listJson, "application/json")
        } else {
            return ResponseEntity.serverError(body = APIResult.Error(message = "No source files found in $sourceBucket which match the requirements to build a $templatesKey file."))
        }
        return ResponseEntity.ok(body = APIResult.Success("Written new '$templatesKey' with $filesProcessed markdown files processed"))
    }

    companion object {
        const val postsKey = "generated/posts.json"
        const val pagesKey = "generated/pages.json"
        const val templatesKey = "generated/templates.json"
        const val postsPrefix = "sources/posts/"
        const val pagesPrefix = "sources/pages/"
        const val templatesPrefix = "templates/"
    }
}