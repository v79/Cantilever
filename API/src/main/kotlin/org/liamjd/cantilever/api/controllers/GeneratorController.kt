package org.liamjd.cantilever.api.controllers

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.common.createStringAttribute
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
        const val PAGES_DIR = "sources/pages/"
    }
    private val s3Service: S3Service by inject()
    private val sqsService: SQSService by inject()

    private val queueUrl: String = System.getenv("markdown_processing_queue")

    /**
     * Generate the HTML version of the page specified by the path parameter 'srcKey'.
     * The actual path searched for will be `/sources/pages/<srcKey>'.
     * This method will send a message to the markdown processing queue in SQS.
     */
    fun generatePage(request: Request<Unit>): ResponseEntity<APIResult<String>> {
        val sourceType = "pages"

        val requestKey = request.pathParameters["srcKey"]
        if(requestKey == "*") {
            println("Wow, that's a big request")
        } else {

            val srcKey = PAGES_DIR + request.pathParameters["srcKey"]
            println("GeneratorController: Received request to regenerate page $srcKey")
            try {
                println("Received page file $srcKey and sending it to Markdown processor queue")
                val sourceString = s3Service.getObjectAsString(srcKey, sourceBucket)
                val pageSrcKey = srcKey.removePrefix("sources/$sourceType/") // just want the actual file name
                // extract page model
                val pageModel = extractPageModel(pageSrcKey, sourceString)
                println("Built page model: $pageModel")

                val msgResponse = sqsService.sendMessage(
                    toQueue = queueUrl,
                    body = pageModel,
                    messageAttributes = createStringAttribute("sourceType", sourceType)
                )

                if (msgResponse != null) {
                    println("Message '$srcKey' sent, message ID is ${msgResponse.messageId()}'")
                } else {
                    println("No response received for message")
                }

            } catch (nske: NoSuchKeyException) {
                println("${nske.message} for key $srcKey")
                return ResponseEntity.notFound(body = APIResult.Error(message = "Could not find page with key $srcKey"))
            }
        }
        return ResponseEntity.ok(body = APIResult.Success(value =  "Regenerated file $requestKey"))
    }
}