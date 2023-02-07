package org.liamjd.cantilever.api.controllers

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.api.services.StructureService
import org.liamjd.cantilever.common.toLocalDateTime
import org.liamjd.cantilever.models.Layouts
import org.liamjd.cantilever.models.Post
import org.liamjd.cantilever.models.Structure
import org.liamjd.cantilever.models.Template
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import org.liamjd.cantilever.services.S3Service
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

/**
 * Handle functions relating to the structure.json file
 */
class StructureController(val sourceBucket: String, val corsDomain: String = "https://www.cantilevers.org") : KoinComponent {

    private val s3Service: S3Service by inject()
    private val structureService: StructureService by inject()
    private val structureKey = "generated/structure.json"
    private val templatesKey = "templates/"

    /**
     * Return the Json file 'structure.json' from the generated file bucket
     * Or an error if the file is not found
     */
    fun getStructureFile(request: Request<Unit>): ResponseEntity<APIResult<Structure>> {
        println("StructureController: Searching for file '$structureKey'")
        return if (s3Service.objectExists(structureKey, sourceBucket)) {
            try {
                val structureJson = s3Service.getObjectAsString(structureKey, sourceBucket)
                val structure = Json.decodeFromString<Structure>(structureJson)
                println("StructureController: Returning structure object with ${structure.posts.size} posts")
                ResponseEntity.ok(body = APIResult.Success(structure))
            } catch (se: SerializationException) {
                ResponseEntity.serverError(body = APIResult.Error(se.message ?: "Serialization Exception"))
            }
        } else {
            println("StructureController: Returning not found")
            ResponseEntity.notFound(body = APIResult.Error("Structure file $structureKey not found in bucket $sourceBucket"))
        }
    }

    /**
     * Rebuild the entire structure file from scratch, processing each markdown file in the source bucket
     */
    fun rebuildStructureFile(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        println("StructureController: Rebuilding structure file")
        val listResponse = s3Service.listObjects(prefix = "sources/", bucket = sourceBucket)
        println("Sources exist in '${sourceBucket}': ${listResponse.hasContents()} (up to ${listResponse.keyCount()} files)")
        var filesProcessed = 0
        if (listResponse.hasContents()) {
            val layouts = Layouts(mutableMapOf())
            val structure = Structure(layouts, mutableListOf(), 0)

            listResponse.contents().forEach { obj ->
                if (obj.key().endsWith(".md")) {
                    println("Extracting metadata from file ${obj.key()} (${obj.size()} bytes)")
                    val markdownSource = s3Service.getObjectAsString(obj.key(), sourceBucket)
                    val postMetadata = structureService.extractPostMetadata(obj.key(), markdownSource)
                    println(postMetadata)
                    val templateKey = templatesKey + postMetadata.template + ".html.hbs"
                    val template = try {
                        val lastModified = obj.lastModified().toLocalDateTime()
                        Template(templateKey, lastModified)
                    } catch (nske: NoSuchKeyException) {
                        println("Cannot find template file '$templateKey'; aborting for file '${obj.key()}'")
                        return@forEach
                    }

                    structure.posts.add(Post(
                        title = postMetadata.title,
                        srcKey = obj.key(),
                        url = postMetadata.slug,
                        template = template,
                        date = postMetadata.date,
                        lastUpdated = postMetadata.lastModified
                    ))
                    structure.layouts.templates[templateKey] = template
                    filesProcessed++
                } else {
                    println("Skipping non-markdown file '${obj.key()}'")
                }
            }
            structure.postCount = filesProcessed
            val structureToSave = Json.encodeToString(Structure.serializer(),structure)
            println("Saving new structure json file (${structureToSave.length} bytes)")
            s3Service.putObject(structureKey,sourceBucket,structureToSave,"application/json")

        } else {
            return ResponseEntity.serverError(body = APIResult.Error(message = "No source files found in $sourceBucket which match the requirements to build a project structure file."))
        }

        return ResponseEntity.ok(body = APIResult.Success("Written new '$structureKey' with $filesProcessed markdown files processed"))
    }

    fun addFileToStructure(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        return ResponseEntity.ok(body = APIResult.Success("Haven't actually added the file to the structure"))
    }

    fun removeFileFromStructure(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        return ResponseEntity.ok(body = APIResult.Success("Haven't actually removed the file from the structure"))
    }
}
