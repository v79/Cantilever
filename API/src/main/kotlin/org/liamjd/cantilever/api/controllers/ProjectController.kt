package org.liamjd.cantilever.api.controllers

import com.charleskorn.kaml.Yaml
import kotlinx.datetime.Clock
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.S3_KEY.pagesKey
import org.liamjd.cantilever.common.S3_KEY.pagesPrefix
import org.liamjd.cantilever.common.S3_KEY.postsKey
import org.liamjd.cantilever.common.S3_KEY.postsPrefix
import org.liamjd.cantilever.common.S3_KEY.projectKey
import org.liamjd.cantilever.common.S3_KEY.templatesKey
import org.liamjd.cantilever.common.S3_KEY.templatesPrefix
import org.liamjd.cantilever.common.getFrontMatter
import org.liamjd.cantilever.models.*
import org.liamjd.cantilever.routing.MimeType
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.impl.extractPageModel
import org.liamjd.cantilever.services.impl.extractPostMetadata
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

private const val APP_JSON = "application/json"

/**
 * Manages all the project-wide configuration and json models
 * TODO: there is a lot of duplication in this class
 */
class ProjectController(val sourceBucket: String) : KoinComponent, APIController {

    private val s3Service: S3Service by inject()

    /**
     * Return the 'cantilever.yaml' project definition file, in yaml format.
     */
    fun getProject(request: Request<Unit>): ResponseEntity<APIResult<CantileverProject>> {
        info("Retrieving 'cantilever.yaml' file")
        return if (s3Service.objectExists(projectKey, sourceBucket)) {
            val projectYaml = s3Service.getObjectAsString(projectKey, sourceBucket)
            try {
                val project = Yaml.default.decodeFromString(CantileverProject.serializer(), projectYaml)
                ResponseEntity.ok(body = APIResult.Success(value = project))
            } catch (se: SerializationException) {
                error(se.message ?: "Error deserializing cantilever.yaml. Project is broken.")
                ResponseEntity.serverError(
                    body = APIResult.Error(
                        message = se.message ?: "Error deserializing cantilever.yaml. Project is broken."
                    )
                )
            }
        } else {
            error("Cannot find file '$projectKey' in bucket '$sourceBucket'. Project is broken.")
            ResponseEntity.notFound(body = APIResult.Error(message = "Cannot find file '$projectKey' in bucket '$sourceBucket'. Project is broken."))
        }
    }

    /**
     * Update the 'cantilever.yaml' project definition file. This will come to us as yaml document, not json, but it will return as json.
     */
    fun updateProjectDefinition(request: Request<CantileverProject>): ResponseEntity<APIResult<CantileverProject>> {
        info("Updating 'cantilever.yaml' file")
        val updatedDefinition = request.body
        if (updatedDefinition.projectName.isBlank()) {
            return ResponseEntity.badRequest(APIResult.Error(message = "Unable to update project definition where 'project name' is blank"))
        }
        info("Updated project: $updatedDefinition")
        val yamlToSave = Yaml.default.encodeToString(CantileverProject.serializer(), request.body)
        val jsonResponse = Json.encodeToString(CantileverProject.serializer(), request.body)
        s3Service.putObject(
            projectKey,
            sourceBucket,
            yamlToSave,
            MimeType.yaml.toString()
        )
        return ResponseEntity.ok(body = APIResult.Success(value = updatedDefinition))
    }

    /**
     * Return a list of all the [PostMeta]s
     */
    fun getPosts(request: Request<Unit>): ResponseEntity<APIResult<PostList>> {
        println("ProjectController: Retrieving all posts")
        return if (s3Service.objectExists(postsKey, sourceBucket)) {
            val postListJson = s3Service.getObjectAsString(postsKey, sourceBucket)
            val postList = Json.decodeFromString(PostList.serializer(), postListJson)
            val sorted = postList.posts.sortedBy { it.date }
            ResponseEntity.ok(
                body = APIResult.Success(
                    value = PostList(
                        count = sorted.size,
                        posts = sorted,
                        lastUpdated = postList.lastUpdated
                    )
                )
            )
        } else {
            ResponseEntity.notFound(body = APIResult.Error(message = "Cannot find file '$postsKey' in bucket '$sourceBucket'. To regenerate from sources, call PUT /project/posts/rebuild"))
        }
    }

    /**
     * Return a list of all the [PageMeta]s
     */
    fun getPages(request: Request<Unit>): ResponseEntity<APIResult<PageTree>> {
        info("Retrieving all pages")
        return if (s3Service.objectExists(pagesKey, sourceBucket)) {
            val pageListJson = s3Service.getObjectAsString(pagesKey, sourceBucket)
            val pageTree = Json.decodeFromString(PageTree.serializer(), pageListJson)
            ResponseEntity.ok(body = APIResult.Success(value = pageTree))
        } else {
            error("Cannot find file '$pagesKey' in bucket '$sourceBucket'. To regenerate from sources, call PUT /project/pages/rebuild")
            ResponseEntity.notFound(body = APIResult.Error(message = "Cannot find file '$pagesKey' in bucket '$sourceBucket'. To regenerate from sources, call PUT /project/pages/rebuild"))
        }
    }

    /**
     * Return a list of all the [Template]s
     */
    fun getTemplates(request: Request<Unit>): ResponseEntity<APIResult<TemplateList>> {
        info("Retrieving templates pages")
        return if (s3Service.objectExists(templatesKey, sourceBucket)) {
            val templateListJson = s3Service.getObjectAsString(templatesKey, sourceBucket)
            val templateList = Json.decodeFromString(TemplateList.serializer(), templateListJson)
            ResponseEntity.ok(body = APIResult.Success(value = templateList))
        } else {
            error("Cannot find file '$templatesKey' in bucket '$sourceBucket'. To regenerate from sources, call PUT /project/templates/rebuild")
            ResponseEntity.notFound(body = APIResult.Error(message = "Cannot find file '$templatesKey' in bucket '$sourceBucket'. To regenerate from sources, call PUT /project/templates/rebuild"))
        }
    }

    /**
     * Rebuild the generated/posts.json file which contains the metadata for all the [PostMeta]s in the project.
     */
    @Deprecated("This will be replaced with [MetadataController.rebuildFromSources]")
    fun rebuildPostList(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val posts = s3Service.listObjects(postsPrefix, sourceBucket)
        info("Rebuilding all posts from sources in '$postsPrefix'. ${posts.keyCount()} posts found.")
        var filesProcessed = 0
        if (posts.hasContents()) {
            val list = mutableListOf<PostMeta>()
            posts.contents().forEach { obj ->
                if (obj.key().endsWith(".md")) {
                    info("Extracting metadata from file '${obj.key()}'")
                    val markdownSource = s3Service.getObjectAsString(obj.key(), sourceBucket)
                    val postMetadata = extractPostMetadata(obj.key(), markdownSource)
                    val templateKey = templatesPrefix + postMetadata.template + ".html.hbs"
                    val template = try {
                        val lastModified = obj.lastModified().toKotlinInstant()
                        val templateMetadata = TemplateMetadata("", emptyList())
                        Template(templateKey, lastModified, templateMetadata)
                    } catch (nske: NoSuchKeyException) {
                        error("Cannot find template file '$templateKey'; aborting for file '${obj.key()}'")
                        return@forEach
                    }
                    list.add(
                        PostMeta(
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
                    warn("Skipping non-markdown file '${obj.key()}'")
                }
            }
            info("Sorting output, most recent first")
            list.sortByDescending { it.date }
            val postList = PostList(posts = list, count = filesProcessed, lastUpdated = Clock.System.now())
            val listJson = Json.encodeToString(PostList.serializer(), postList)
            info("Saving PostList JSON file (${listJson.length} bytes)")
            s3Service.putObject(postsKey, sourceBucket, listJson, APP_JSON)
        } else {
            error("No source files found in $sourceBucket which match the requirements to build a $postsKey file.")
            return ResponseEntity.serverError(body = APIResult.Error(message = "No source files found in $sourceBucket which match the requirements to build a $postsKey file."))
        }
        return ResponseEntity.ok(body = APIResult.Success("Written new '${postsKey}' with $filesProcessed markdown files processed"))

    }

    /**
     * Rebuild the generated/pages.json file which contains the metadata for all the Pages and Page folders in the project.
     */
    @Deprecated("This will be replaced with [MetadataController.rebuildFromSources]")
    fun rebuildPageTree(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val objectsResponse = s3Service.listObjects(pagesPrefix, sourceBucket)
        info("Rebuilding all pages from sources in '$pagesPrefix'. ${objectsResponse.keyCount()} pages found.")
        val folderList = s3Service.listFolders(pagesPrefix, sourceBucket).toMutableSet()
        var filesProcessed = 0
        if (objectsResponse.hasContents()) {
            val pageList = mutableListOf<PageTreeNode.PageMeta>()
            // get the common deliminators, aka the folders
            if (folderList.isNotEmpty()) {
                info("Common prefixes (folders): $folderList")
            }
            // then get the individual files
            objectsResponse.contents().forEach { obj ->
                if (obj.key().endsWith(".md")) {
                    info("Extracting metadata from file '${obj.key()}'")
                    val markdownSource = s3Service.getObjectAsString(obj.key(), sourceBucket)
                    val pageModel = extractPageModel(obj.key(), markdownSource)
                    val templateKey = templatesPrefix + pageModel.templateKey + ".html.hbs"
                    val template = try {
                        val lastModified = obj.lastModified().toKotlinInstant()
                        val templateMetadata = TemplateMetadata("", emptyList())
                        Template(templateKey, lastModified, templateMetadata)
                    } catch (nske: NoSuchKeyException) {
                        error("Cannot find template file '$templateKey'; aborting for file '${obj.key()}'")
                        return@forEach
                    }
                    // we don't need to store the full contents of the sections in this file (just as we don't store the body in posts.json)
                    pageList.add(
                        PageTreeNode.PageMeta(
                            nodeType = "page",
                            title = pageModel.title,
                            srcKey = pageModel.srcKey,
                            templateKey = pageModel.templateKey,
                            url = pageModel.url,
                            sections = buildMap { pageModel.sections.keys.forEach { key -> put(key, "") } },
                            attributes = pageModel.attributes,
                            lastUpdated = obj.lastModified().toKotlinInstant()
                        )
                    )
                    filesProcessed++
                } else {
                    if (obj.key().endsWith("/")) {
                        folderList.add(obj.key())
                    }
                    warn("Skipping non-markdown file '${obj.key()}'")
                }
            }

            // folderList contains a flat list of all folders ('common prefixes')
            // pageList contains a flat list of_all_ pages
            // we need to create FolderNodes for each of the items in FolderList, and add the appropriate children from pageList

            val folders = mutableListOf<PageTreeNode.FolderNode>()
            folderList.forEach {
                folders.add(PageTreeNode.FolderNode(nodeType = "folder", srcKey = it, children = null, isRoot = false))
            }
            var totalCount = folders.size

            pageList.forEach { page ->
                val match =
                    folders.find { folder -> folder.srcKey.substringBeforeLast("/") == page.srcKey.substringBeforeLast("/") }
                if (match != null) {
                    if (match.children == null) {
                        match.children = mutableListOf()
                    }
                    info("Adding page '${page.srcKey}' to folder '${match.srcKey}'")
                    match.children?.add(page)
                    match.count++
                    totalCount++
                }
            }

            val combined: MutableList<PageTreeNode> = mutableListOf()
            combined.addAll(folders)
            val containerFolder = PageTreeNode.FolderNode("folder", pagesPrefix, combined, false)
            containerFolder.count = totalCount
            val pageTree = PageTree(lastUpdated = Clock.System.now(), container = containerFolder)
            val treeJson = Json.encodeToString(PageTree.serializer(), pageTree)
            info("Saving PageList JSON file (${treeJson.length} bytes)")
            s3Service.putObject(pagesKey, sourceBucket, treeJson, APP_JSON)
        } else {
            return ResponseEntity.serverError(body = APIResult.Error(message = "No source files found in $sourceBucket which match the requirements to build a $postsKey file."))
        }
        return ResponseEntity.ok(body = APIResult.Success("Written new '$postsKey' with $filesProcessed markdown files processed"))
    }

    /**
     * Rebuild the generated/templates.json file which contains the metadata for all the Templates in the project.
     */
    @Deprecated("This will be replaced with [MetadataController.rebuildFromSources]")
    fun rebuildTemplateList(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val templates = s3Service.listObjects(templatesPrefix, sourceBucket)
        info("Rebuilding all templates from sources in '$pagesPrefix'. ${templates.keyCount()} templates found.")
        var filesProcessed = 0
        if (templates.hasContents()) {
            val list = mutableListOf<Template>()
            templates.contents().forEach { obj ->
                try {
                    if (obj.key().endsWith(".hbs")) {
                        val lastModified = obj.lastModified().toKotlinInstant()
                        val templateString = s3Service.getObjectAsString(obj.key(), sourceBucket)
                        val frontMatter = templateString.getFrontMatter()
                        val metadata = Yaml.default.decodeFromString(TemplateMetadata.serializer(), frontMatter)
                        list.add(
                            Template(obj.key(), lastModified, metadata)
                        )
                        filesProcessed++
                    } else {
                        warn("Skipping non-hbs file '${obj.key()}'")
                    }
                } catch (se: SerializationException) {
                    error("Error deserializing template file ${obj.key()}. Error was: ${se.message}")
                }
            }
            list.sortByDescending { it.lastUpdated }
            val templateList =
                TemplateList(templates = list.toList(), count = filesProcessed, lastUpdated = Clock.System.now())
            val listJson = Json.encodeToString(TemplateList.serializer(), templateList)
            info("Saving PageList JSON file (${listJson.length} bytes)")
            s3Service.putObject(templatesKey, sourceBucket, listJson, APP_JSON)
        } else {
            error("No source files found in $sourceBucket which match the requirements to build a $templatesKey file.")
            return ResponseEntity.serverError(body = APIResult.Error(message = "No source files found in $sourceBucket which match the requirements to build a $templatesKey file."))
        }
        return ResponseEntity.ok(body = APIResult.Success("Written new '$templatesKey' with $filesProcessed markdown files processed"))
    }

    override fun info(message: String) = println("INFO: ProjectController: $message")
    override fun warn(message: String) = println("WARN: ProjectController: $message")
    override fun error(message: String) = println("ERROR: ProjectController: $message")
}