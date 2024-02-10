package org.liamjd.cantilever.api.controllers

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.SerializationException
import org.koin.core.component.KoinComponent
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.common.getFrontMatter
import org.liamjd.cantilever.common.stripFrontMatter
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.Template
import org.liamjd.cantilever.models.TemplateMetadata
import org.liamjd.cantilever.models.rest.HandlebarsTemplate
import org.liamjd.cantilever.models.rest.TemplateListDTO
import org.liamjd.cantilever.models.rest.TemplateUseDTO
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import java.net.URLDecoder
import java.nio.charset.Charset

class TemplateController(sourceBucket: String) : KoinComponent, APIController(sourceBucket) {

    /**
     * Load a handlebars template file with the specified 'srcKey' and return it as a [ContentNode.TemplateNode] response
     */
    fun loadHandlebarsSource(request: Request<Unit>): ResponseEntity<APIResult<ContentNode.TemplateNode>> {
        val handlebarSource = request.pathParameters["srcKey"]
        return if (handlebarSource != null) {
            val decoded = URLDecoder.decode(handlebarSource, Charset.defaultCharset())
            info("Loading handlebar file $decoded")
            return if (s3Service.objectExists(decoded, sourceBucket)) {
                // TODO: replace with s3service.objectExists
                val templateObj = s3Service.getObject(decoded, sourceBucket)
                if (templateObj != null) {
                    val body = s3Service.getObjectAsString(decoded, sourceBucket)
                    val frontmatter = body.getFrontMatter()
                    // TODO: this throws exception if a value is missing from the frontmatter, even though it should encode the default
                    val metadata = Yaml.default.decodeFromString(TemplateMetadata.serializer(), frontmatter)
                    info("Handlebar frontmatter: $metadata")

                    val templateNode: ContentNode.TemplateNode = ContentNode.TemplateNode(
                        srcKey = decoded,
                        title = metadata.name,
                        sections = metadata.sections?.toMutableList() ?: mutableListOf()
                    ).also { it.body = body.stripFrontMatter() }
                    ResponseEntity.ok(body = APIResult.Success(templateNode))
                } else {
                    error("Handlebars file $decoded not found in bucket $sourceBucket")
                    ResponseEntity.notFound(
                        body = APIResult.Error("Handlebars file $decoded not found in bucket $sourceBucket")
                    )
                }
            } else {
                error("Handlebars file $decoded not found in bucket $sourceBucket")
                ResponseEntity.notFound(
                    body = APIResult.Error("Handlebars file $decoded not found in bucket $sourceBucket")
                )
            }
        } else {
            error("Invalid request for null source file")
            ResponseEntity.badRequest(body = APIResult.Error("Invalid request for null source file"))
        }
    }

    /**
     * Save a handlebars template file, which contains a key, sections, name and body
     */
    fun saveTemplate(request: Request<ContentNode.TemplateNode>): ResponseEntity<APIResult<String>> {
        val templateNode = request.body
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
            contentTree.updateTemplate(
                templateNode
            )
            saveContentTree()
            ResponseEntity.ok(
                body =
                APIResult.OK("Updated file ${templateNode.srcKey}, $length bytes")
            )
        } else {
            info("Creating new file '${templateNode.srcKey}'")
            val length = s3Service.putObjectAsString(
                templateNode.srcKey,
                sourceBucket,
                convertTemplateToYamlString(templateNode),
                "text/plain"
            )
            templateNode.body = ""
            contentTree.insertTemplate(
                templateNode
            )
            saveContentTree()
            ResponseEntity.ok(
                body =
                APIResult.OK("Updated file ${templateNode.srcKey}, $length bytes")
            )
        }
    }

    /**
     * Load the handlebars template file and extract its metadata
     * TODO: return a [TemplateNode] instead of a [TemplateMetadata]
     */
    fun getTemplateMetadata(request: Request<Unit>): ResponseEntity<APIResult<TemplateMetadata>> {
        val handlebarKey = request.pathParameters["templateKey"]
        return if (handlebarKey != null) {
            val srcKey = URLDecoder.decode(handlebarKey, Charset.defaultCharset())
            return if (s3Service.objectExists(srcKey, sourceBucket)) {
                try {
                    info("Loading metadata for template $handlebarKey")
                    val template = s3Service.getObjectAsString(srcKey, sourceBucket)
                    val frontmatter = template.getFrontMatter()
                    val metadata = Yaml.default.decodeFromString(TemplateMetadata.serializer(), frontmatter)
                    ResponseEntity.ok(body = APIResult.Success(metadata))
                } catch (se: SerializationException) {
                    ResponseEntity.serverError(
                        body = APIResult.Error("Could not deserialize template $srcKey. Error was ${se.message}")
                    )
                }
            } else {
                ResponseEntity.badRequest(body = APIResult.Error("Could not find template $srcKey"))
            }
        } else {
            ResponseEntity.badRequest(body = APIResult.Error("Invalid template key"))
        }
    }

    /**
     * Return the list of templates
     */
    fun getTemplates(request: Request<Unit>): ResponseEntity<APIResult<TemplateListDTO>> {
        return if (loadContentTree()) {
            info("Fetching all posts from metadata.json")
            val lastUpdated = s3Service.getUpdatedTime(S3_KEY.metadataKey, sourceBucket)
            val templates = contentTree.templates.sortedBy { it.title }
            val templateList = TemplateListDTO(
                count = templates.size,
                lastUpdated = lastUpdated,
                templates = templates
            )
            ResponseEntity.ok(body = APIResult.Success(value = templateList))
        } else {
            error("Cannot find file '$S3_KEY.metadataKey' in bucket $sourceBucket")
            ResponseEntity.notFound(
                body = APIResult.Error(message = "Cannot find file '${S3_KEY.metadataKey}' in bucket $sourceBucket")
            )
        }
    }

    /**
     * Get a list of all the nodes which use a given template, and their keys
     */
    fun getTemplateUsage(request: Request<Unit>): ResponseEntity<APIResult<TemplateUseDTO>> {
        val templateKey = request.pathParameters["srcKey"]
        if (templateKey != null) {
            return if (loadContentTree()) {
                val decoded = URLDecoder.decode(templateKey, Charsets.UTF_8)
                info("Looking for uses of template $decoded")
                val pages = contentTree.getPagesForTemplate(decoded).map { it.srcKey }
                val posts = contentTree.getPostsForTemplate(decoded).map { it.srcKey }
                val count = pages.size + posts.size
                val dto = TemplateUseDTO(count = count, pageKeys = pages, postKeys = posts)
                info("Found ${dto.count} uses, across ${dto.pageKeys.size} pages and ${dto.postKeys.size} posts")
                ResponseEntity.ok(APIResult.Success(dto))
            } else {
                error("Cannot find file '$S3_KEY.metadataKey' in bucket $sourceBucket")
                ResponseEntity.notFound(
                    body = APIResult.Error(message = "Cannot find file '${S3_KEY.metadataKey}' in bucket $sourceBucket")
                )
            }
        }
        return ResponseEntity.badRequest(APIResult.Error(message = "Invalid request with null templateKey"))
    }

    /**
     * Delete a handlebars template file if there are no pages or posts using it
     */
    fun deleteTemplate(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val templateKey = request.pathParameters["srcKey"]
        if (templateKey != null) {
            return if (loadContentTree()) {
                val decoded = URLDecoder.decode(templateKey, Charsets.UTF_8)
                info("Deleting template $decoded")
                val pages = contentTree.getPagesForTemplate(decoded)
                val posts = contentTree.getPostsForTemplate(decoded)
                return if (pages.isEmpty() && posts.isEmpty()) {
                    info("No pages or posts use this template, deleting")
                    val templateNode = contentTree.getTemplate(decoded)
                    return if (templateNode == null) {
                        error("Could not find template $decoded in content tree")
                        ResponseEntity.badRequest(APIResult.Error("Could not find template $decoded in content tree"))
                    } else {
                        val deleted = s3Service.deleteObject(decoded, sourceBucket)
                        contentTree.deleteTemplate(templateNode)
                        saveContentTree()
                        ResponseEntity.ok(APIResult.OK("Deleted template $decoded"))
                    }
                } else {
                    error("Cannot delete template $decoded, it is used by ${pages.size} pages and ${posts.size} posts")
                    ResponseEntity.badRequest(
                        APIResult.Error(
                            "Cannot delete template $decoded, it is used by ${pages.size} pages and ${posts.size} posts"
                        )
                    )
                }
            } else {
                error("Cannot find file '$S3_KEY.metadataKey' in bucket $sourceBucket")
                ResponseEntity.notFound(
                    body = APIResult.Error(message = "Cannot find file '${S3_KEY.metadataKey}' in bucket $sourceBucket")
                )
            }
        }
        return ResponseEntity.badRequest(APIResult.Error(message = "Invalid request with null templateKey"))
    }

    /**
     * The front end gives us a JSON representation of a handlebars template, which we need to convert to a YAML frontmatter & body
     */
    private fun convertTemplateToYamlString(template: ContentNode.TemplateNode): String {
        val templateMetadata = TemplateMetadata(
            name = template.title,
            sections = template.sections
        )
        val sBuilder: StringBuilder = StringBuilder()
        sBuilder.appendLine("---")
        sBuilder.appendLine(Yaml.default.encodeToString(TemplateMetadata.serializer(), templateMetadata))
        sBuilder.appendLine("---")
        sBuilder.appendLine(template.body)
        return sBuilder.toString()
    }

    override fun info(message: String) = println("INFO: TemplateController: $message")
    override fun warn(message: String) = println("WARN: TemplateController: $message")
    override fun error(message: String) = println("ERROR: TemplateController: $message")
}