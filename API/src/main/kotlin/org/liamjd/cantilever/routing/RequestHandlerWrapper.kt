package org.liamjd.cantilever.routing

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

abstract class RequestHandlerWrapper : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    val serializersModule = SerializersModule { }

    abstract val router: Router

    override fun handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent =
        input.apply {
            headers = headers.mapKeys { it.key.lowercase() }
        }
            .let { handleRequest(input) }


    @Suppress("UNCHECKED_CAST")
    internal fun handleRequest(input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
        println(
            "RequestHandlerWrapper: handleRequest(): looking for route which matches request  ${input.httpMethod} ${input.path} <${
                input.getHeader(
                    "Content-Type"
                )
            }->${input.acceptedMediaTypes()}>"
        )
        // find matching route
        val routes = router.routes as List<RouterFunction<Any, Any>>
        println("Valid routes:")
        router.listRoutes()
        println()
        val matchResults: List<RequestMatchResult> = routes.map { routerFunction: RouterFunction<Any, Any> ->
            val matchResult = routerFunction.requestPredicate.match(input)
            if (matchResult.matches) {
                val handler: HandlerFunction<Any, Any> = routerFunction.handler
                val matchedAcceptType = routerFunction.requestPredicate.matchedAcceptType(input.acceptedMediaTypes())
                    ?: router.produceByDefault.first()

                val entity: ResponseEntity<out Any> = try {
                    println("Calling the handler function $handler")
                    // this is where we'd add authorization checks, which may throw exceptions
                    val requestBody = "TODO: deserialize the input request body"
                    val request = Request(input, requestBody, routerFunction.requestPredicate.pathPattern)
                    (handler as HandlerFunction<*, *>)(request)
                } catch (e: Exception) {
                    ResponseEntity.serverError(e.message)
                    // TODO return createErrorResponse(errorEntity)
                }

                return createResponse(entity, matchedAcceptType)
            }
            matchResult

        }
        return createNoMatchingRouteResponse(input.httpMethod, input.path, input.acceptedMediaTypes())
    }

    /**
     * Return a 404 message with some useful details
     */
    private fun createNoMatchingRouteResponse(
        httpMethod: String?,
        path: String?,
        acceptedMediaTypes: List<MimeType>
    ): APIGatewayProxyResponseEvent {
        return APIGatewayProxyResponseEvent()
            .withStatusCode(404)
            .withHeaders(mapOf("Content-Type" to "text/plain"))
            .withBody("No match found for route '$httpMethod' '$path' which only accepts $acceptedMediaTypes")
    }

    /**
     * Create a response to return to the client
     * @param responseEntity the object being returned
     * @param mimeType the mime type of the response, typically application/json
     * @return an AWS [APIGatewayProxyResponseEvent] with the body of the response entity serialized in some way
     */
    @OptIn(InternalSerializationApi::class)
    private fun <T : Any> createResponse(
        responseEntity: ResponseEntity<T>,
        mimeType: MimeType
    ): APIGatewayProxyResponseEvent {
        println("Attempting to serialize $responseEntity (class ${responseEntity.kType})")

        var contentType: String = ""

        val body: String = when (mimeType) {
            MimeType.json -> {
                responseEntity.kType?.let { ktype ->
                    val kSerializer = serializer(ktype)
                    println("Serializer is $kSerializer")
                    kSerializer.let {
                        contentType = mimeType.toString()
                        Json.encodeToString(kSerializer, responseEntity.body as T)
                    }
                } ?: "could not ---- could not get serializer for $responseEntity"

            }

            MimeType.html -> {
                "html"
            }

            MimeType.plainText -> {
                "text"
            }

            else -> {
                "error"
            }
        }

        /**could I serialize the body object to a string
        serialize a Result.Success() object to a string
        then stitch in the body into the result json?

         * { "result":
         *   { "statusCode": "200",
         *     "body" : "!!!!" <-- leave blank
         *   }
         *  }
         *
         *  then string.replace("!!!!",bodyJson)
         *
         *  I'd have to escape bodyJson though to make it valid (I think)
         */


        val resp = APIGatewayProxyResponseEvent().withStatusCode(200)
            .withHeaders(mapOf("Content-Type" to contentType))
            .withBody(body)

        return resp
    }

    private fun <T> createErrorResponse(): APIGatewayProxyResponseEvent =
        APIGatewayProxyResponseEvent().withStatusCode(500)
}

