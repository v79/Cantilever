package org.liamjd.cantilever.routing

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.charleskorn.kaml.Yaml
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.liamjd.cantilever.common.MimeType
import org.liamjd.cantilever.routing.Router.Companion.CONTENT_TYPE

/**
 * Implementing the AWS API Gateway [RequestHandler] interface, this class looks for a route which matches the incoming request.
 * If a route exists, it generates a response by calling the [HandlerFunction] declared in the route, parsing and deserializing the body
 * if it exists.
 * @param corsDomain The website domain name, required for CORS
 */
abstract class RequestHandlerWrapper(open val corsDomain: String = "https://www.cantilevers.org/") :
    RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    abstract val router: Router

    /**
     * Clean up the received headers then pass request to the internal [handleRequest] function
     */
    override fun handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent =
        input.apply {
            headers = headers.mapKeys { it.key.lowercase() }
        }
            .let { handleRequest(input) }


    /**
     * Look for a declared route which matches the input method, path, accept headers and response type headers.
     * If there is a matching route, check for authorization requirements (implementing [org.liamjd.cantilever.auth.Authorizer]), and then
     * parse and deserialize the input body if it exists, then pass the request to the [HandlerFunction].
     * Finally, convert the [ResponseEntity] into the required [APIGatewayProxyResponseEvent].
     * If there is an error at any point, return an appropriate error response code and text body.
     */
    internal fun handleRequest(input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
        println(
            "RequestHandlerWrapper: handleRequest(): looking for route which matches request  ${input.httpMethod} ${input.path} <${
                input.getHeader(
                    CONTENT_TYPE
                )
            }->${input.acceptedMediaTypes()}>"
        )

        router.routes.entries.map { route: MutableMap.MutableEntry<RequestPredicate, RouterFunction<*, *>> ->
            val matchResult = route.key.match(input)
            if (matchResult.matches) {
                val matchedAcceptType = route.key.matchedAcceptType(input.acceptedMediaTypes())
                    ?: router.produceByDefault.first()
                // check the requirements and fail very early, even before authorizing
                // I think this should be in processRoute, really
                route.key.requirements.forEach { requirement ->
                    // TODO: how might I make this more user-friendly?
                    println("Checking requirement: $requirement")
                    if (!requirement.invoke(input)) {
                        println("Requirement failed")
                        return createResponse(
                            ResponseEntity.badRequest("Request did not meet requirements"),
                            MimeType.plainText
                        )
                    }
                }

                val entity: ResponseEntity<out Any> = processRoute(input, route.value)
                return createResponse(entity, matchedAcceptType, route.key.headerOverrides)
            }
            matchResult
        }
        return createNoMatchingRouteResponse(input.httpMethod, input.path, input.acceptedMediaTypes())
    }

    /**
     * Process the matching route by extracting the request body (if any), and calling the handler function
     * @param input the raw request event from AWS API Gateway
     * @param routerFunction the request predicate, handler and ???
     * @return a [ResponseEntity] object ready to be serialized and returned to the requester
     */
    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalSerializationApi::class)
    private fun processRoute(
        input: APIGatewayProxyRequestEvent,
        routerFunction: RouterFunction<*, *>
    ): ResponseEntity<out Any> {
        println("Processing route ${routerFunction.requestPredicate.method} ${routerFunction.requestPredicate.pathPattern}, supplying ${routerFunction.requestPredicate.supplies}")
        routerFunction.authorizer?.let { auth ->
            println("Checking authentication/authorization for ${auth.simpleName}")
            val authResult = auth.authorize(input)
            if (!authResult.authorized) {
                return ResponseEntity.unauthorized("Authorization check failed: ${authResult.message}")
            }
        }

        val handler: (Nothing) -> ResponseEntity<out Any> = routerFunction.handler

        val entity: ResponseEntity<out Any> = if (routerFunction.requestPredicate.kType == null) {
            // this must be a GET request
            val request = Request(input, null, routerFunction.requestPredicate.pathPattern)
            (handler as HandlerFunction<*, *>)(request)
        } else {
            // TODO: I should probably stop using this custom abuse of the HTTP standards
            if (input.xContentLengthHeader() == "0") {
                // body can be null in this case
                val request = Request(input, null, routerFunction.requestPredicate.pathPattern)
                (handler as HandlerFunction<*, *>)(request)
            } else {
                val kType = routerFunction.requestPredicate.kType!!
                if (input.body == null) {
                    ResponseEntity.badRequest(body = "No body received but $kType was expected. If there is legitimately no body, add a X-Content-Length header with value '0'.")
                } else {
                    try {
                        val contentType = input.getHeader("Content-Type")
                        println("Processing route: input content type was $contentType. Deserializing...")
                        // Deserialize the input string with the serializer declared for the kType specified in the API definition
                        // and based on the Content-Type header

                        val bodyObject = if (contentType != null) when (MimeType.parse(contentType)) {
                            MimeType.json -> {
                                Json.decodeFromString(serializer(kType), input.body)
                            }

                            MimeType.yaml -> {
                                Yaml.default.decodeFromString(serializer(kType), input.body)
                            }

                            else -> {
                                input.body
                            }
                        } else input.body
                        val request = Request(input, bodyObject, routerFunction.requestPredicate.pathPattern)
                        // call the handler function with the request object; this will return a ResponseEntity; if it catches an error then that's the fault of the handler function
                        try {
                            (handler as HandlerFunction<*, *>)(request)
                        } catch (e: Exception) {
                            println("Error calling handler function: ${e.message}")
                            ResponseEntity.serverError(body = "Server error in processing request for ${handler}: ${e.message}")
                        }
                    } catch (mfe: MissingFieldException) {
                        println("Invalid request. Error is: ${mfe.message}")
                        ResponseEntity.badRequest(body = "Invalid request. Error is: ${mfe.message}")
                    } catch (se: SerializationException) {
                        println("Could not deserialize body. Error is: ${se.message}")
                        ResponseEntity.badRequest(body = "Could not deserialize body. Error is: ${se.message}")
                    } catch (iae: IllegalArgumentException) {
                        println("Illegal argument exception. Error is: ${iae.message}")
                        ResponseEntity.badRequest(body = "Could not deserialize body; IllegalArgumentException. Error is: ${iae.message}")
                    }
                }
            }
        }
        return entity
    }

    /**
     * Return a 404 message with some useful details
     */
    private fun createNoMatchingRouteResponse(
        httpMethod: String?,
        path: String?,
        acceptedMediaTypes: List<MimeType>
    ): APIGatewayProxyResponseEvent {
        println("No route match found for $httpMethod $path")
        val possibleAlts = router.routes.filterKeys { it.pathPattern == path }
        if (possibleAlts.isNotEmpty()) {
            println("Possible alternatives: ${possibleAlts.keys}")
        }
        return APIGatewayProxyResponseEvent()
            .withStatusCode(404)
            .withHeaders(mapOf("Content-Type" to "text/plain"))
            .withBody("No match found for route '$httpMethod' '$path' which accepts $acceptedMediaTypes")
    }

    /**
     * Create a response to return to the client. All fields will be serialized, even those with default values.
     * @param responseEntity the object being returned
     * @param mimeType the mime type of the response, typically application/json
     * @param headerOverrides additional or replacement headers as specified by the route
     * @return an AWS [APIGatewayProxyResponseEvent] with the body of the response entity serialized in some way
     */
    private fun <T : Any> createResponse(
        responseEntity: ResponseEntity<T>,
        mimeType: MimeType,
        headerOverrides: Map<String, String> = emptyMap()
    ): APIGatewayProxyResponseEvent {

        var contentType = ""
        val jsonFormat = Json { prettyPrint = false; encodeDefaults = true }
        val body: String = when (mimeType) {
            MimeType.json -> {
                responseEntity.kType?.let { kType ->
                    val kSerializer = serializer(kType)
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

        val responseHeaders = mutableMapOf(CONTENT_TYPE to contentType, "Access-Control-Allow-Origin" to corsDomain)
        // A route may supply additional or overriding headers. There are two mechanisms to add headers
        responseHeaders.putAll(headerOverrides)
        responseHeaders.putAll(responseEntity.headers)
        return APIGatewayProxyResponseEvent().withStatusCode(responseEntity.statusCode)
            .withHeaders(responseHeaders)
            .withBody(body)
    }
}

