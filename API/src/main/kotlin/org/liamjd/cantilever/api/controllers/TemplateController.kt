package org.liamjd.cantilever.api.controllers

import com.charleskorn.kaml.Yaml
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.koin.core.component.KoinComponent
import org.liamjd.apiviaduct.routing.Request
import org.liamjd.apiviaduct.routing.Response
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.SOURCE_TYPE
import org.liamjd.cantilever.common.getFrontMatter
import org.liamjd.cantilever.common.stripFrontMatter
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.TemplateMetadata
import org.liamjd.cantilever.models.rest.TemplateListDTO
import org.liamjd.cantilever.models.rest.TemplateUseDTO
import java.net.URLDecoder
import java.nio.charset.Charset

class TemplateController(sourceBucket: String, generationBucket: String) : KoinComponent,
    APIController(sourceBucket, generationBucket) {

    /**
     * Load a Handlebars template file with the specified 'srcKey' and return it as a [ContentNode.TemplateNode] response
     */
    fun loadHandlebarsSource(request: Request<Unit>): Response<APIResult<ContentNode.TemplateNode>> {
        val handlebarSource = request.pathParameters["srcKey"]
        val domain = request.headers["cantilever-project-domain"]!!
        return if (handlebarSource != null) {
            val decoded = URLDecoder.decode(handlebarSource, Charset.defaultCharset())
            info("Loading handlebar file $decoded")
            return if (s3Service.objectExists(decoded, sourceBucket)) {
                // TODO: replace with s3service.objectExists
                val templateObj = s3Service.getObject(decoded, sourceBucket)
                if (templateObj != null) {
                    val body = s3Service.getObjectAsString(decoded, sourceBucket)
                    val frontMatter = body.getFrontMatter()
                    // TODO: this throws exception if a value is missing from the frontMatter, even though it should encode the default
                    val metadata = Yaml.default.decodeFromString(TemplateMetadata.serializer(), frontMatter)
                    val templateNode: ContentNode.TemplateNode = ContentNode.TemplateNode(
                        srcKey = decoded,
                        title = metadata.name,
                        sections = metadata.sections?.toMutableList() ?: mutableListOf()
                    ).also { it.body = body.stripFrontMatter() }
                    Response.ok(body = APIResult.Success(templateNode))
                } else {
                    error("Handlebars file $decoded not found in bucket $sourceBucket")
                    Response.notFound(
                        body = APIResult.Error("Handlebars file $decoded not found in bucket $sourceBucket")
                    )
                }
            } else {
                error("Handlebars file $decoded not found in bucket $sourceBucket")
                Response.notFound(
                    body = APIResult.Error("Handlebars file $decoded not found in bucket $sourceBucket")
                )
            }
        } else {
            error("Invalid request for null source file")
            Response.badRequest(body = APIResult.Error("Invalid request for null source file"))
        }
    }

    /**
     * Save a Handlebars template file, which contains a key, sections, name and body
     */
    fun saveTemplate(request: Request<ContentNode.TemplateNode>): Response<APIResult<String>> {
        val templateNode = request.body
        val domain = request.headers["cantilever-project-domain"]!!
        val srcKey = URLDecoder.decode(templateNode.srcKey, Charset.defaultCharset())

        // both branches do the same thing
        return if (s3Service.objectExists(srcKey, sourceBucket)) {
            info("Updating existing file '${templateNode.srcKey}'")
            val length = s3Service.putObjectAsString(
                templateNode.srcKey,
                sourceBucket,
                convertTemplateToYamlString(templateNode),
                "text/plain"
            )
            templateNode.body = ""
            Response.ok(
                body =
                    APIResult.OK("Updated file ${templateNode.srcKey}, $length bytes")
            )
        } else {
            info("Creating new file '$domain/$srcKey' by copying from '${templateNode.srcKey}'")
            // we need to update the sourceKey to include the projectHeaderKey, and we need to remove the body before adding into the contentTree
            val newNode = templateNode.copy(srcKey = "$domain/$srcKey")
            newNode.body = templateNode.body
            val length = s3Service.putObjectAsString(
                "$domain/$srcKey",
                sourceBucket,
                convertTemplateToYamlString(newNode),
                "text/plain"
            )
            newNode.body = ""
            Response.ok(
                body =
                    APIResult.OK("Updated file ${newNode.srcKey}, $length bytes")
            )
        }
    }

    /**
     * Return the list of templates
     */
    fun getTemplates(request: Request<Unit>): Response<APIResult<TemplateListDTO>> {
        val projectKeyHeader = request.headers["cantilever-project-domain"]

        return if (projectKeyHeader.isNullOrBlank()) {
            Response.badRequest(body = APIResult.Error("Invalid project key"))
        } else {
            val templates = runBlocking {
                val templates = dynamoDBService.listAllNodesForProject(projectKeyHeader, SOURCE_TYPE.Templates)
                    .filterIsInstance<ContentNode.TemplateNode>()
                TemplateListDTO(
                    count = templates.size,
                    lastUpdated = Clock.System.now(),
                    templates = templates
                )
            }
            Response.ok(body = APIResult.Success(value = templates))
        }
    }

    /**
     * Get a list of all the nodes which use a given template, and their keys
     */
    fun getTemplateUsage(request: Request<Unit>): Response<APIResult<TemplateUseDTO>> {
        val templateKey = request.pathParameters["srcKey"]
        val projectKeyHeader = request.headers["cantilever-project-domain"]!!
        return if (templateKey != null) {
            runBlocking {
                val decoded = URLDecoder.decode(templateKey, Charsets.UTF_8)
                info("Looking for uses of template $decoded")
                val srcKey = decoded.removePrefix("$projectKeyHeader/")
                val posts = dynamoDBService.getKeyListMatchingTemplate(projectKeyHeader, SOURCE_TYPE.Posts, srcKey)
                val pages = dynamoDBService.getKeyListMatchingTemplate(projectKeyHeader, SOURCE_TYPE.Pages, srcKey)
                val count = pages.size + posts.size
                val dto = TemplateUseDTO(count = count, pageKeys = pages, postKeys = posts)
                info("Found ${dto.count} uses, across ${dto.pageKeys.size} pages and ${dto.postKeys.size} posts")
                Response.ok(APIResult.Success(dto))
            }
        } else {
            Response.badRequest(APIResult.Error(statusText = "Invalid request with null templateKey"))
        }
    }

    /**
     * Delete a Handlebars template file if there are no pages or posts using it
     */
    fun deleteTemplate(request: Request<Unit>): Response<APIResult<String>> {
        val templateKey = request.pathParameters["srcKey"]
        val projectKeyHeader = request.headers["cantilever-project-domain"]!!
        if (templateKey != null) {
            runBlocking {
                val decoded = URLDecoder.decode(templateKey, Charsets.UTF_8)
                info("Attempting to deleting template '$decoded'; ensuring no pages or posts use the template")
                val pages = dynamoDBService.getKeyListMatchingTemplate(projectKeyHeader, SOURCE_TYPE.Pages, decoded)
                val posts = dynamoDBService.getKeyListMatchingTemplate(projectKeyHeader, SOURCE_TYPE.Posts, decoded)
                val response = if (pages.isEmpty() && posts.isEmpty()) {
                    info("No pages or posts use this template, deleting from bucket")
                    s3Service.deleteObject(
                        decoded,
                        sourceBucket
                    ) // relying on FileUploadController to respond to the delete event and remove the record from DynamoDB
                    Response.ok(APIResult.OK("Deleted template $decoded"))
                } else {
                    error("Cannot delete template $decoded, it is used by ${pages.size} pages and ${posts.size} posts")
                    Response.badRequest(
                        APIResult.Error(
                            "Cannot delete template $decoded, it is used by ${pages.size} pages and ${posts.size} posts"
                        )
                    )
                }
                return@runBlocking response
            }
        } else {
            return Response.badRequest(APIResult.Error(statusText = "Invalid request with null templateKey"))
        }
        return Response.serverError(APIResult.Error("Server error while attempting to delete template; should not have reached here"))
    }

    /**
     * The front end gives us a JSON representation of a Handlebars template, which we need to convert to a YAML front matter and body
     */
    private fun convertTemplateToYamlString(template: ContentNode.TemplateNode): String {
        val sBuilder: StringBuilder = StringBuilder()
        sBuilder.appendLine("---")
        sBuilder.appendLine("name: ${template.title}")
        if (template.sections.isNotEmpty()) {
            sBuilder.appendLine("sections:")
            template.sections.forEach {
                sBuilder.appendLine("  - $it")
            }
        }
        sBuilder.appendLine("---")
        sBuilder.appendLine(template.body)
        return sBuilder.toString()
    }

    override fun info(message: String) = println("INFO: TemplateController: $message")
    override fun warn(message: String) = println("WARN: TemplateController: $message")
    override fun error(message: String) = println("ERROR: TemplateController: $message")
}