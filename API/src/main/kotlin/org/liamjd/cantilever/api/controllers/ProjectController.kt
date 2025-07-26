package org.liamjd.cantilever.api.controllers

import com.charleskorn.kaml.Yaml
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.apiviaduct.routing.Request
import org.liamjd.apiviaduct.routing.Response
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.S3_KEY.pagesKey
import org.liamjd.cantilever.common.S3_KEY.pagesPrefix
import org.liamjd.cantilever.common.S3_KEY.postsKey
import org.liamjd.cantilever.common.S3_KEY.templatesKey
import org.liamjd.cantilever.common.S3_KEY.templatesPrefix
import org.liamjd.cantilever.common.getFrontMatter
import org.liamjd.cantilever.models.*
import org.liamjd.cantilever.models.dynamodb.Project
import org.liamjd.cantilever.services.DynamoDBService

private const val APP_JSON = "application/json"

/**
 * Manages all the project-wide configuration and json models
 * TODO: there is a lot of duplication in this class
 */
class ProjectController(sourceBucket: String, generationBucket: String) : KoinComponent, APIController(sourceBucket, generationBucket) {
    
    private val dynamoDBService: DynamoDBService by inject()


    /**
     * Return the project definition from DynamoDB.
     */
    fun getProject(request: Request<Unit>): Response<APIResult<CantileverProject>> {
        val projectKey = request.pathParameters["projectKey"]
        if (projectKey.isNullOrEmpty()) {
            error("Unable to retrieve project definition where 'project key' is blank")
            return Response.badRequest(
                APIResult.Error(statusText = "Unable to retrieve project definition where 'project key' is blank")
            )
        }

        // Extract domain and project name from the project key
        // Project key format is "domain.yaml"
        val domain = projectKey.substringBefore(".yaml")

        info("Retrieving project for domain '$domain'")

        return runBlocking {
            val dbProject = dynamoDBService.getProject(domain)

            if (dbProject != null) {
                // Convert Project to CantileverProject
                val cantileverProject = CantileverProject(
                    domain = dbProject.domain,
                    projectName = dbProject.projectName,
                    author = dbProject.author,
                    dateFormat = dbProject.dateFormat,
                    dateTimeFormat = dbProject.dateTimeFormat
                )
                Response.ok(body = APIResult.Success(value = cantileverProject))
            } else {
                error("Cannot find project for domain '$domain'")
                Response.notFound(
                    body = APIResult.Error(
                        statusText = "Cannot find project for domain '$domain'"
                    )
                )
            }
        }
    }

    /**
     * Return a list of all the projects from DynamoDB
     */
    fun getProjectList(request: Request<Unit>): Response<APIResult<List<Pair<String, String>>>> {
        info("Retrieving list of projects from DynamoDB")
        
        return runBlocking {
            val projects = dynamoDBService.listAllProjects()
            val projectList = projects.map { project -> 
                Pair("${project.domain}.yaml", project.projectName)
            }
            
            Response.ok(body = APIResult.Success(value = projectList))
        }
    }

    /**
     * Update the project definition in DynamoDB.
     */
    fun updateProjectDefinition(request: Request<CantileverProject>): Response<APIResult<CantileverProject>> {
        info("Updating project definition in DynamoDB")
        val updatedDefinition = request.body
        if (updatedDefinition.projectName.isBlank() || updatedDefinition.domain.isBlank()) {
            return Response.badRequest(
                APIResult.Error(statusText = "Unable to update project definition where 'project name' or 'domain' is blank")
            )
        }
        info("Updated project: $updatedDefinition")

        // Convert CantileverProject to Project
        val project = Project(
            domain = updatedDefinition.domain,
            projectName = updatedDefinition.projectName,
            author = updatedDefinition.author,
            dateFormat = updatedDefinition.dateFormat,
            dateTimeFormat = updatedDefinition.dateTimeFormat
        )
        
        return runBlocking {
            dynamoDBService.saveProject(project)
            Response.ok(body = APIResult.Success(value = updatedDefinition))
        }
    }

    /**
     * Create a new project in DynamoDB
     */
    fun createProject(request: Request<CantileverProject>): Response<APIResult<String>> {
        info("Creating new project in DynamoDB")
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
        
        return runBlocking {
            // Check if the project already exists
            val existingProject = dynamoDBService.getProject(newProject.domain)
            if (existingProject != null) {
                error("Project ${newProject.domain} already exists")
                return@runBlocking Response.conflict(
                    APIResult.Error(statusText = "Project ${newProject.domain} already exists")
                )
            }
            
            // Convert CantileverProject to Project
            val project = Project(
                domain = newProject.domain,
                projectName = newProject.projectName,
                author = newProject.author,
                dateFormat = newProject.dateFormat,
                dateTimeFormat = newProject.dateTimeFormat
            )
            
            dynamoDBService.saveProject(project)
            
            Response.ok(
                body = APIResult.Success(value = "Successfully created project ${newProject.projectName}")
            )
        }
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

