package org.liamjd.cantilever.api.controllers

import com.charleskorn.kaml.Yaml
import kotlinx.datetime.toKotlinInstant
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
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import java.net.URLDecoder
import java.nio.charset.Charset

class TemplateController(sourceBucket: String) : KoinComponent, APIController(sourceBucket) {

    /**
     * Load a handlebars template file with the specified 'srcKey' and return it as a [HandlebarsTemplate] response
     */
    fun loadHandlebarsSource(request: Request<Unit>): ResponseEntity<APIResult<HandlebarsTemplate>> {
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
                    val template = Template(
                        handlebarSource,
                        templateObj.lastModified().toKotlinInstant(),
                        metadata
                    )
                    val handlebarsTemplate = HandlebarsTemplate(template, body.stripFrontMatter())
                    ResponseEntity.ok(body = APIResult.Success(handlebarsTemplate))
                } else {
                    error("Handlebars file $decoded not found in bucket $sourceBucket")
                    ResponseEntity.notFound(body = APIResult.Error("Handlebars file $decoded not found in bucket $sourceBucket"))
                }
            } else {
                error("Handlebars file $decoded not found in bucket $sourceBucket")
                ResponseEntity.notFound(body = APIResult.Error("Handlebars file $decoded not found in bucket $sourceBucket"))
            }
        } else {
            error("Invalid request for null source file")
            ResponseEntity.badRequest(body = APIResult.Error("Invalid request for null source file"))
        }
    }

    /**
     * Save a handlebars template file, which contains a key, sections, name and body
     */
    fun saveTemplate(request: Request<HandlebarsTemplate>): ResponseEntity<APIResult<String>> {
        val handlebarsContent = request.body
        val srcKey = URLDecoder.decode(handlebarsContent.template.key, Charset.defaultCharset())

        // both branches do the same thing
        return if (s3Service.objectExists(srcKey, sourceBucket)) {
            info("Updating existing file '${handlebarsContent.template.key}'")
            val length = writeTemplateFile(handlebarsContent, srcKey)
            contentTree.updateTemplate(
                ContentNode.TemplateNode(
                    srcKey = handlebarsContent.template.key,
                    title = handlebarsContent.template.metadata.name,
                    sections = handlebarsContent.template.metadata.sections?.toMutableList() ?: mutableListOf()
                )
            )
            ResponseEntity.ok(
                body =
                APIResult.OK("Updated file ${handlebarsContent.template.key}, $length bytes")
            )
        } else {
            info("Creating new file '${handlebarsContent.template.key}'")
            val length = writeTemplateFile(handlebarsContent, srcKey)
            contentTree.insertTemplate(
                ContentNode.TemplateNode(
                    srcKey = handlebarsContent.template.key,
                    title = handlebarsContent.template.metadata.name,
                    sections = handlebarsContent.template.metadata.sections?.toMutableList() ?: mutableListOf()
                )
            )
            ResponseEntity.ok(body = APIResult.OK("Updated file ${handlebarsContent.template.key}, $length bytes"))
        }
    }

    /**
     * Load the handlebars template file and extract its metadata
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
                    ResponseEntity.serverError(body = APIResult.Error("Could not deserialize template $srcKey. Error was ${se.message}"))
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
        return if (s3Service.objectExists(S3_KEY.metadataKey, sourceBucket)) {
            loadContentTree()
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
            ResponseEntity.notFound(body = APIResult.Error(message = "Cannot find file '${S3_KEY.metadataKey}' in bucket $sourceBucket"))
        }
    }

    /**
     * Build the frontmatter for the metadata for a handlebars template file
     */
    private fun buildFrontmatterForTemplate(template: Template): String {
        val sBuilder = StringBuilder()
        sBuilder.append("---\n")
        sBuilder.append("name: ${template.metadata.name}\n")
        sBuilder.append("sections:\n")
        template.metadata.sections?.forEach {
            sBuilder.append(" - $it\n")
        }
        sBuilder.append("---\n")
        return sBuilder.toString()
    }

    /**
     * Write a handlebars template file to S3, combining the metadata and the body into a single string
     */
    private fun writeTemplateFile(handlebarsContent: HandlebarsTemplate, key: String): Int {
        val frontmatter = buildFrontmatterForTemplate(template = handlebarsContent.template)
        val body = frontmatter + handlebarsContent.body
        return s3Service.putObject(key, sourceBucket, body, "text/html")
    }

    override fun info(message: String) = println("INFO: TemplateController: $message")
    override fun warn(message: String) = println("WARN: TemplateController: $message")
    override fun error(message: String) = println("ERROR: TemplateController: $message")
}