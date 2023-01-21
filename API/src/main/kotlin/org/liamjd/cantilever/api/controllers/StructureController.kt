package org.liamjd.cantilever.api.controllers

import io.moia.router.Request
import io.moia.router.ResponseEntity
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.models.Structure
import org.liamjd.cantilever.services.S3Service

class StructureController(val sourceBucket: String) : KoinComponent {

    private val s3Service: S3Service by inject()
    private val structureKey = "generated/structure.json"


    fun getStructureFile(request: Request<Unit>): ResponseEntity<Result> {
        println("Searching for file '$structureKey'")
        return if(s3Service.objectExists(structureKey, sourceBucket)) {
            val structureJson = s3Service.getObjectAsString(structureKey,sourceBucket)
            val structure = Json.decodeFromString<Structure>(structureJson)
            println("Returning structure object with ${structure.items.posts.size} posts")
            ResponseEntity(statusCode = 200, body = Result.Success(value = structure))
        } else {
            println("Returning not found")
            ResponseEntity(statusCode = 404, body = Result.Error("Fake not found structure file $structureKey for bucket $sourceBucket"))
        }

        /*println("Returning empty")
        return ResponseEntity(statusCode = 200, body = Result.Empty)*/
    }
}

sealed class Result {
    data class Success<out R>(val value: R) : Result()
    data class Error(val message: String) : Result()
    data class OK(val message: String) : Result()
    object Empty : Result()
}