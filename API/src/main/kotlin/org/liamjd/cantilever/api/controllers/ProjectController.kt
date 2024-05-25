package org.liamjd.cantilever.api.controllers

import com.charleskorn.kaml.Yaml
import kotlinx.datetime.Clock
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.liamjd.apiviaduct.routing.Request
import org.liamjd.apiviaduct.routing.Response
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.MimeType
import org.liamjd.cantilever.common.S3_KEY.pagesKey
import org.liamjd.cantilever.common.S3_KEY.pagesPrefix
import org.liamjd.cantilever.common.S3_KEY.postsKey
import org.liamjd.cantilever.common.S3_KEY.templatesKey
import org.liamjd.cantilever.common.S3_KEY.templatesPrefix
import org.liamjd.cantilever.common.getFrontMatter
import org.liamjd.cantilever.models.*

private const val APP_JSON = "application/json"

/**
 * Manages all the project-wide configuration and json models
 * TODO: there is a lot of duplication in this class
 */
class ProjectController(sourceBucket: String, generationBucket: String) : KoinComponent, APIController(sourceBucket, generationBucket) {

    /**
     * Return the 'cantilever.yaml' project definition file, in yaml format.
     */
    fun getProject(request: Request<Unit>): Response<APIResult<CantileverProject>> {
        val projectKey = request.pathParameters["projectKey"]
        if (projectKey.isNullOrEmpty()) {
            error("Unable to retrieve project definition where 'project key' is blank")
            return Response.badRequest(
                APIResult.Error(statusText = "Unable to retrieve project definition where 'project key' is blank")
            )
        }
        info("Retrieving '$projectKey' file")
        return if (s3Service.objectExists(projectKey, sourceBucket)) {
            val projectYaml = s3Service.getObjectAsString(projectKey, sourceBucket)
            try {
                val project = Yaml.default.decodeFromString(CantileverProject.serializer(), projectYaml)
                Response.ok(body = APIResult.Success(value = project))
            } catch (se: SerializationException) {
                error(se.message ?: "Error deserializing '$projectKey'. Project is broken.")
                Response.serverError(
                    body = APIResult.Error(
                        statusText = se.message ?: "Error deserializing '$projectKey'. Project is broken."
                    )
                )
            }
        } else {
            error("Cannot find file '$projectKey' in bucket '$sourceBucket'. Project is broken.")
            Response.notFound(
                body = APIResult.Error(
                    statusText = "Cannot find file '$projectKey' in bucket '$sourceBucket'."
                )
            )
        }
    }

    /**
     * Return a list of all the projects
     */
    fun getProjectList(request: Request<Unit>): Response<APIResult<List<Pair<String, String>>>> {
        info("Retrieving list of projects")
        val projects = s3Service.listObjectsDelim("", "/", sourceBucket)
        val projectList = mutableListOf<Pair<String, String>>()
        projects.contents().forEach { obj ->
            if (obj.key().endsWith(".yaml")) {
                val projectYaml = s3Service.getObjectAsString(obj.key(), sourceBucket)
                try {
                    val project = Yaml.default.decodeFromString(CantileverProject.serializer(), projectYaml)
                    projectList.add(Pair(obj.key(), project.projectName))
                } catch (se: SerializationException) {
                    error(se.message ?: "Error deserializing ${obj.key()}. Project is broken.")
                }
            }
        }
        return Response.ok(body = APIResult.Success(value = projectList))
    }

    /**
     * Update the 'cantilever.yaml' project definition file. This will come to us as yaml document, not json, but it will return as json.
     */
    fun updateProjectDefinition(request: Request<CantileverProject>): Response<APIResult<CantileverProject>> {
        info("Updating '<project>.yaml' file")
        val updatedDefinition = request.body
        if (updatedDefinition.projectName.isBlank() || updatedDefinition.domain.isBlank()) {
            return Response.badRequest(
                APIResult.Error(statusText = "Unable to update project definition where 'project name' or 'domain' is blank")
            )
        }
        info("Updated project: $updatedDefinition")

        val yamlToSave = Yaml.default.encodeToString(CantileverProject.serializer(), request.body)
        val jsonResponse = Json.encodeToString(CantileverProject.serializer(), request.body)
        s3Service.putObjectAsString(
            updatedDefinition.projectKey,
            sourceBucket,
            yamlToSave,
            MimeType.yaml.toString()
        )
        return Response.ok(body = APIResult.Success(value = updatedDefinition))
    }

    /**
     * Create a new project
     * This will come to us as yaml document, not json
     * The yaml file will be based on the domain name, e.g. www.example.com.yaml
     */
    fun createProject(request: Request<CantileverProject>): Response<APIResult<String>> {
        info("Creating new project")
        val newProject = request.body
        if (newProject.projectName.isBlank()) {
            return Response.badRequest(
                APIResult.Error(statusText = "Unable to create project where 'project name' is blank")
            )
        }
        if (newProject.domain.isBlank()) {
            return Response.badRequest(
                APIResult.Error(statusText = "Unable to create project where 'domain' is blank")
            )
        }
        info("New project: $newProject")
        val projectKey = newProject.projectKey
        // TODO: check if the project already exists
        if (s3Service.objectExists(projectKey, sourceBucket)) {
            error("Project ${newProject.domain} already exists")
            return Response.conflict(
                APIResult.Error(statusText = "Project ${newProject.domain} already exists")
            )
        }
        val yamlToSave = Yaml.default.encodeToString(CantileverProject.serializer(), request.body)
        s3Service.putObjectAsString(
            projectKey,
            sourceBucket,
            yamlToSave,
            MimeType.yaml.toString()
        )
        return Response.ok(
            body = APIResult.Success(value = "Successfully created project ${newProject.projectName}")
        )
    }

    /**
     * Return a list of all the [PostMeta]s
     */
    @Deprecated("Replaced with [PostController.getPosts]")
    fun getPosts(request: Request<Unit>): Response<APIResult<PostList>> {
        info("ProjectController: Retrieving all posts")
        return if (s3Service.objectExists(postsKey, sourceBucket)) {
            val postListJson = s3Service.getObjectAsString(postsKey, sourceBucket)
            val postList = Json.decodeFromString(PostList.serializer(), postListJson)
            val sorted = postList.posts.sortedBy { it.date }
            Response.ok(
                body = APIResult.Success(
                    value = PostList(
                        count = sorted.size,
                        posts = sorted,
                        lastUpdated = postList.lastUpdated
                    )
                )
            )
        } else {
            Response.notFound(
                body = APIResult.Error(
                    statusText = "Cannot find file '$postsKey' in bucket '$sourceBucket'. To regenerate from sources, call PUT /project/posts/rebuild"
                )
            )
        }
    }

    /**
     * Return a list of all the [PageMeta]s
     */
    @Deprecated("Replaced with [PageController.getPages]")
    fun getPages(request: Request<Unit>): Response<APIResult<PageTree>> {
        info("Retrieving all pages")
        return if (s3Service.objectExists(pagesKey, sourceBucket)) {
            val pageListJson = s3Service.getObjectAsString(pagesKey, sourceBucket)
            val pageTree = Json.decodeFromString(PageTree.serializer(), pageListJson)
            Response.ok(body = APIResult.Success(value = pageTree))
        } else {
            error(
                "Cannot find file '$pagesKey' in bucket '$sourceBucket'. To regenerate from sources, call PUT /project/pages/rebuild"
            )
            Response.notFound(
                body = APIResult.Error(
                    statusText = "Cannot find file '$pagesKey' in bucket '$sourceBucket'. To regenerate from sources, call PUT /project/pages/rebuild"
                )
            )
        }
    }

    /**
     * Return a list of all the [Template]s
     */
    @Deprecated("Replaced with [TemplateController.getTemplates]")
    fun getTemplates(request: Request<Unit>): Response<APIResult<TemplateList>> {
        info("Retrieving templates pages")
        return if (s3Service.objectExists(templatesKey, sourceBucket)) {
            val templateListJson = s3Service.getObjectAsString(templatesKey, sourceBucket)
            val templateList = Json.decodeFromString(TemplateList.serializer(), templateListJson)
            Response.ok(body = APIResult.Success(value = templateList))
        } else {
            error(
                "Cannot find file '$templatesKey' in bucket '$sourceBucket'. To regenerate from sources, call PUT /project/templates/rebuild"
            )
            Response.notFound(
                body = APIResult.Error(
                    statusText = "Cannot find file '$templatesKey' in bucket '$sourceBucket'. To regenerate from sources, call PUT /project/templates/rebuild"
                )
            )
        }
    }


    /**
     * Rebuild the generated/templates.json file which contains the metadata for all the Templates in the project.
     */
    @Deprecated("This will be replaced with [MetadataController.rebuildFromSources]")
    fun rebuildTemplateList(request: Request<Unit>): Response<APIResult<String>> {
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
            s3Service.putObjectAsString(templatesKey, sourceBucket, listJson, APP_JSON)
        } else {
            error("No source files found in $sourceBucket which match the requirements to build a $templatesKey file.")
            return Response.serverError(
                body = APIResult.Error(
                    statusText = "No source files found in $sourceBucket which match the requirements to build a $templatesKey file."
                )
            )
        }
        return Response.ok(
            body = APIResult.Success("Written new '$templatesKey' with $filesProcessed markdown files processed")
        )
    }

    override fun info(message: String) = println("INFO: ProjectController: $message")
    override fun warn(message: String) = println("WARN: ProjectController: $message")
    override fun error(message: String) = println("ERROR: ProjectController: $message")
}

