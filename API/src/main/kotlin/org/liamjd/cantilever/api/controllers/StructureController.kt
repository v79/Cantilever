package org.liamjd.cantilever.api.controllers

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.models.Structure
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import org.liamjd.cantilever.services.S3Service

class StructureController(val sourceBucket: String) : KoinComponent {

    private val s3Service: S3Service by inject()
    private val structureKey = "generated/structure.json"

    fun getStructureFile(request: Request<Unit>): ResponseEntity<Result> {
        println("Searching for file '$structureKey'")
        return if(s3Service.objectExists(structureKey, sourceBucket)) {
            try {
                val structureJson = s3Service.getObjectAsString(structureKey, sourceBucket)
                val structure = Json.decodeFromString<Structure>(structureJson)
                println("Returning structure object with ${structure.items.posts.size} posts")
//                ResponseEntity.ok(body = Result.Success(value = structure))

                ResponseEntity.ok(body = Result.OK("Ok, found structure but not returning it yet"))
            } catch (se: SerializationException) {
                ResponseEntity.serverError(body = Result.Error(se.message?: "Serialization Exception"))
            }
        } else {
            println("Returning not found")
            ResponseEntity.notFound(body = Result.Error("Structure file $structureKey not found in bucket $sourceBucket"))
        }

        /*println("Returning empty")
        return ResponseEntity(statusCode = 200, body = Result.Empty)*/
    }
}

@Serializable
sealed class Result {
    @Serializable
    data class Success<out R>(val value: R) : Result()
    @Serializable
    data class Error(val message: String) : Result()
    @Serializable
    data class OK(val message: String) : Result()
}