package org.liamjd.cantilever.routing

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

abstract class RequestHandlerWrapper : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

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
        val routes: List<RouterFunction<*, *>> = router.routes.values.toList()
        val matchResults: List<RequestMatchResult> = routes.map { routerFunction: RouterFunction<*, *> ->
            val matchResult = routerFunction.requestPredicate.match(input)
            if (matchResult.matches) {
                val handler: (Nothing) -> ResponseEntity<out Any> = routerFunction.handler
                val matchedAcceptType = routerFunction.requestPredicate.matchedAcceptType(input.acceptedMediaTypes())
                    ?: router.produceByDefault.first()

                val entity: ResponseEntity<out Any> = try {
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
            .withBody("No match found for route '$httpMethod' '$path';  accepts $acceptedMediaTypes")
    }

    /**
     * Create a response to return to the client
     * @param responseEntity the object being returned
     * @param mimeType the mime type of the response, typically application/json
     * @return an AWS [APIGatewayProxyResponseEvent] with the body of the response entity serialized in some way
     */
    private fun <T : Any> createResponse(
        responseEntity: ResponseEntity<T>,
        mimeType: MimeType
    ): APIGatewayProxyResponseEvent {

        var contentType = ""
        val jsonFormat = Json { prettyPrint = false }
        val body: String = when (mimeType) {
            MimeType.json -> {
                responseEntity.kType?.let { ktype ->
                    val kSerializer = serializer(ktype)
                    contentType = mimeType.toString()
                    kSerializer.let {
                        jsonFormat.encodeToString(kSerializer, responseEntity.body as T)
                    }
                } ?: "could not ---- could not get serializer for $responseEntity"

            }

            MimeType.html -> {
                "html"
            }

            MimeType.plainText -> {
                responseEntity.body.toString()
            }

            else -> {
                "error"
            }
        }
        // with CORS enabled, I have to include Access-Control-Allow-Origin header to *
        // according to https://docs.aws.amazon.com/apigateway/latest/developerguide/how-to-cors-console.html
        return APIGatewayProxyResponseEvent().withStatusCode(200)
            .withHeaders(mapOf("Content-Type" to contentType, "Access-Control-Allow-Origin" to "*"))
            .withBody(body)
    }

    private fun <T> createErrorResponse(): APIGatewayProxyResponseEvent =
        APIGatewayProxyResponseEvent().withStatusCode(500)

//    abstract fun authorize(permissionName: String, function: () -> RequestPredicate)
}

