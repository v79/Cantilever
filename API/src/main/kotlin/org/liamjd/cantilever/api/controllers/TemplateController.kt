package org.liamjd.cantilever.api.controllers

import com.charleskorn.kaml.Yaml
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.SerializationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.getFrontMatter
import org.liamjd.cantilever.models.HandlebarsContent
import org.liamjd.cantilever.models.Template
import org.liamjd.cantilever.models.TemplateMetadata
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import org.liamjd.cantilever.services.S3Service
import java.net.URLDecoder
import java.nio.charset.Charset

class TemplateController(val sourceBucket: String) : KoinComponent, APIController {

    private val s3Service: S3Service by inject()

    /**
     * Load a handlebars template file with the specified 'srcKey' and return it as a [HandlebarsContent] response
     */
    fun loadHandlebarsSource(request: Request<Unit>): ResponseEntity<APIResult<HandlebarsContent>> {
        val handlebarSource = request.pathParameters["srcKey"]
        return if (handlebarSource != null) {
            val decoded = URLDecoder.decode(handlebarSource, Charset.defaultCharset())
            info("Loading handlebar file $decoded")
            return if (s3Service.objectExists(decoded, sourceBucket)) {
                val templateObj = s3Service.getObject(decoded, sourceBucket)
                if (templateObj != null) {
                    val body = s3Service.getObjectAsString(decoded, sourceBucket)
                    val frontmatter = body.getFrontMatter()
                    val metadata = Yaml.default.decodeFromString(TemplateMetadata.serializer(), frontmatter)
                    val template = Template(handlebarSource, metadata.name, templateObj.lastModified().toKotlinInstant(), emptyList())
                    val handlebarsContent = HandlebarsContent(template, body)
                    ResponseEntity.ok(body = APIResult.Success(handlebarsContent))
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
     * Save a handlebars template file
     */
    fun saveTemplate(request: Request<HandlebarsContent>): ResponseEntity<APIResult<String>> {
        val handlebarsContent = request.body
        val srcKey = URLDecoder.decode(handlebarsContent.template.key, Charset.defaultCharset())

        // both branches do the same thing
        return if (s3Service.objectExists(srcKey, sourceBucket)) {
            info("Updating existing file '${handlebarsContent.template.key}'")
            val length = s3Service.putObject(srcKey, sourceBucket, handlebarsContent.body, "text/html")
            ResponseEntity.ok(body = APIResult.OK("Updated file ${handlebarsContent.template.key}, $length bytes"))
        } else {
            info("Creating new file '${handlebarsContent.template.key}'")
            val length = s3Service.putObject(srcKey, sourceBucket, handlebarsContent.body, "text/html")
            ResponseEntity.ok(body = APIResult.OK("Saved new file $srcKey, $length bytes"))
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
                } catch (se: SerializationException){
                    ResponseEntity.serverError(body = APIResult.Error("Could not deserialize template $srcKey. Error was ${se.message}"))
                }
            } else {
               ResponseEntity.badRequest(body = APIResult.Error("Could not find template $srcKey"))
           }
        } else {
            ResponseEntity.badRequest(body = APIResult.Error("Invalid template key"))
        }
    }

    override fun info(message: String) = println("INFO: TemplateController: $message")
    override fun warn(message: String) = println("WARN: TemplateController: $message")
    override fun error(message: String) = println("ERROR: TemplateController: $message")
}