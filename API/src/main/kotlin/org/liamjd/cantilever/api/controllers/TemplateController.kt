package org.liamjd.cantilever.api.controllers

import kotlinx.datetime.toKotlinInstant
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.models.HandlebarsContent
import org.liamjd.cantilever.models.Template
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
            println("TemplateController loading handlebar file $decoded")
            return if (s3Service.objectExists(decoded, sourceBucket)) {
                val templateObj = s3Service.getObject(decoded, sourceBucket)
                if (templateObj != null) {
                    val template = Template(handlebarSource, templateObj.lastModified().toKotlinInstant())
                    val body = s3Service.getObjectAsString(decoded, sourceBucket)
                    val handlebarsContent = HandlebarsContent(template, body)
                    ResponseEntity.ok(body = APIResult.Success(handlebarsContent))
                } else {
                    println("TemplateController: File '$decoded' not found")
                    ResponseEntity.notFound(body = APIResult.Error("Handlebars file $decoded not found in bucket $sourceBucket"))
                }
            } else {
                println("TemplateController: File '$decoded' not found")
                ResponseEntity.notFound(body = APIResult.Error("Handlebars file $decoded not found in bucket $sourceBucket"))
            }
        } else {
            ResponseEntity.badRequest(body = APIResult.Error("Invalid request for null source file"))
        }
    }

    /**
     * Save a handlebars template file
     */
    fun saveTemplate(request: Request<HandlebarsContent>): ResponseEntity<APIResult<String>> {
        println("TemplateController: saveTemplate $request")
        val handlebarsContent = request.body
        val srcKey = URLDecoder.decode(handlebarsContent.template.key,Charset.defaultCharset())

        return if (s3Service.objectExists(srcKey, sourceBucket)) {
            println("Updating existing file '${handlebarsContent.template.key}'")
            println(handlebarsContent.body.take(100))
            val length = s3Service.putObject(srcKey,sourceBucket,handlebarsContent.body,"text/html")
            ResponseEntity.ok(body = APIResult.OK("Updated file ${handlebarsContent.template.key}, $length bytes"))
        } else {
            println("Creating new file...")
            println(handlebarsContent.template)
            val length = s3Service.putObject(srcKey, sourceBucket, handlebarsContent.body, "text/html")
            ResponseEntity.ok(body = APIResult.OK("Saved new file $srcKey, $length bytes"))
        }
    }
}