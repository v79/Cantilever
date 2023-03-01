package org.liamjd.cantilever.api.controllers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.*
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.routing.ResponseEntity
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.SQSService
import org.liamjd.cantilever.services.impl.extractPageModel
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

/**
 * Handle routes relating to document generation. Mostly this will be done by sending messages to the appropriate queues.
 */
class GeneratorController(val sourceBucket: String, val destinationBucket: String) : KoinComponent {

    companion object {
        const val PAGES_DIR = S3_KEY.sources + "pages/"
    }

    private val s3Service: S3Service by inject()
    private val sqsService: SQSService by inject()

    private val queueUrl: String = System.getenv(QUEUE.MARKDOWN)

    /**
     * Generate the HTML version of the page specified by the path parameter 'srcKey'.
     * The actual path searched for will be `/sources/pages/<srcKey>'.
     * This method will send a message to the markdown processing queue in SQS.
     * If <srcKey> is '*' it will trigger regeneration of all source markdown pages
     */
    fun generatePage(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val requestKey = request.pathParameters["srcKey"]
        if (requestKey == "*") {
            println("Wow, that's a big request")

            // get every page in the pages folder
            val pageListResponse = s3Service.listObjects(PAGES_DIR, sourceBucket)
            println("There are ${pageListResponse.keyCount()} potential pages to process")
            pageListResponse.contents().filter { it.key().endsWith(FILE_TYPE.MD) }.forEach { obj ->
                println(obj.key())
                val sourceString = s3Service.getObjectAsString(obj.key(), sourceBucket)
                val pageSrcKey =
                    obj.key().removePrefix("sources/${SOURCE_TYPE.PAGES}/") // just want the actual file name
                // extract page model
                val pageModel = extractPageModel(pageSrcKey, sourceString)
                val msgResponse = sqsService.sendMessage(
                    toQueue = queueUrl,
                    body = pageModel,
                    messageAttributes = createStringAttribute("sourceType", SOURCE_TYPE.PAGES)
                )

                if (msgResponse != null) {
                    println("Message '${obj.key()}' sent to '$queueUrl', message ID is ${msgResponse.messageId()}'")
                } else {
                    println("No response received for message")
                }
            }

            return ResponseEntity.notImplemented(body = APIResult.OK("Mass page generation not yet implemented."))
        } else {

            val srcKey = PAGES_DIR + request.pathParameters["srcKey"]
            println("GeneratorController: Received request to regenerate page $srcKey")
            try {
                println("Received page file $srcKey and sending it to Markdown processor queue")
                val sourceString = s3Service.getObjectAsString(srcKey, sourceBucket)
                val pageSrcKey = srcKey.removePrefix("sources/${SOURCE_TYPE.PAGES}/") // just want the actual file name
                // extract page model
                val pageModel = extractPageModel(pageSrcKey, sourceString)
                println("Built page model: $pageModel")

                val msgResponse = sqsService.sendMessage(
                    toQueue = queueUrl,
                    body = pageModel,
                    messageAttributes = createStringAttribute("sourceType", SOURCE_TYPE.PAGES)
                )

                if (msgResponse != null) {
                    println("Message '$srcKey' sent to '$queueUrl', message ID is ${msgResponse.messageId()}'")
                } else {
                    println("No response received for message")
                }

            } catch (nske: NoSuchKeyException) {
                println("${nske.message} for key $srcKey")
                return ResponseEntity.notFound(body = APIResult.Error(message = "Could not find page with key $srcKey"))
            }
        }
        return ResponseEntity.ok(body = APIResult.Success(value = "Regenerated file $requestKey"))
    }
}