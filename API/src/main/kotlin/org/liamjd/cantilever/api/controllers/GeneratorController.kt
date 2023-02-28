package org.liamjd.cantilever.api.controllers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import org.liamjd.cantilever.services.S3Service

/**
 * Handle routes relating to document generation. Mostly this will be done by sending messages to the appropriate queues.
 */
class GeneratorController(val sourceBucket: String, val destinationBucket: String) : KoinComponent {

    companion object {
        const val PAGES_DIR = "/sources/pages/"
    }
    private val s3Service: S3Service by inject()

    fun generatePage(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val srcKey = PAGES_DIR + request.pathParameters["srcKey"]
        println("GeneratorController: Received request to regenerate page $srcKey")
        val getPageSourceResponse = s3Service.getObject(srcKey,sourceBucket)

        return ResponseEntity.ok(body = APIResult.Success(value =  "Default response"))
    }
}