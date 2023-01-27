package org.liamjd.cantilever.api.controllers

import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.api.models.RawJsonString
import org.liamjd.cantilever.models.Structure
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import org.liamjd.cantilever.services.S3Service

class StructureController(val sourceBucket: String) : KoinComponent {

    private val s3Service: S3Service by inject()
    private val structureKey = "generated/simplerStructure.json"

    /**
     * Return the Json file 'structure.json' from the generated file bucket
     * Or an error if the file is not found
     */
    fun getStructureFile(request: Request<Unit>): ResponseEntity<APIResult<RawJsonString>> {
        println("StructureController: Searching for file '$structureKey'")
        return if (s3Service.objectExists(structureKey, sourceBucket)) {
            try {
                val structureJson = s3Service.getObjectAsString(structureKey, sourceBucket)
                val structure = Json.decodeFromString<Structure>(structureJson)
                println("StructureController: Returning structure object with ${structure.items.posts.size} posts")
                ResponseEntity.ok(body = APIResult.JsonSuccess(RawJsonString(structureJson)))
            } catch (se: SerializationException) {
                ResponseEntity.serverError(body = APIResult.Error(se.message ?: "Serialization Exception"))
            }
        } else {
            println("StructureController: Returning not found")
            ResponseEntity.notFound(body = APIResult.Error("Structure file $structureKey not found in bucket $sourceBucket"))
        }
    }
}
