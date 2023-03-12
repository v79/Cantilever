package org.liamjd.cantilever.routing

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.liamjd.cantilever.routing.Router.Companion.CONTENT_TYPE

/**
 * Implementing the AWS API Gateway [RequestHandler] interface, this class looks for a route which matches the incoming request
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
    @Suppress("UNCHECKED_CAST")
    internal fun handleRequest(input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
        println(
            "RequestHandlerWrapper: handleRequest(): looking for route which matches request  ${input.httpMethod} ${input.path} <${
                input.getHeader(
                    CONTENT_TYPE
                )
            }->${input.acceptedMediaTypes()}>"
        )
        // find matching route
        val routes: List<RouterFunction<*, *>> = router.routes.values.toList()
        routes.map { routerFunction: RouterFunction<*, *> ->
            val matchResult = routerFunction.requestPredicate.match(input)
            if (matchResult.matches) {
                val matchedAcceptType = routerFunction.requestPredicate.matchedAcceptType(input.acceptedMediaTypes())
                    ?: router.produceByDefault.first()

                val entity: ResponseEntity<out Any> = processRoute(input, routerFunction)
                return createResponse(entity, matchedAcceptType)
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
    @OptIn(ExperimentalSerializationApi::class)
    private fun processRoute(
        input: APIGatewayProxyRequestEvent,
        routerFunction: RouterFunction<*, *>
    ): ResponseEntity<out Any> {
        println("Processing route ${routerFunction.requestPredicate.method} ${routerFunction.requestPredicate.pathPattern}, supplying ${routerFunction.requestPredicate.supplies}")
        routerFunction.authorizer?.let { auth ->
            println("Checking authentication/authorization for ${auth.simpleName}")

            println("BAD - BYPASSING AUTH!")

            if (corsDomain != "http://localhost:5173") {
                val authResult = auth.authorize(input)
                if (!authResult.authorized) {
                    return ResponseEntity.unauthorized("Authorization check failed: ${authResult.message}")
                }
            }

            println("END BAD!")
        }

        val handler: (Nothing) -> ResponseEntity<out Any> = routerFunction.handler

        val entity = if (routerFunction.requestPredicate.kType == null) {
            // this must be a GET request
            val request = Request(input, null, routerFunction.requestPredicate.pathPattern)
            (handler as HandlerFunction<*, *>)(request)
        } else {
            println("Headers: ${input.headers}")
            println("Content-Length: ${input.contentLengthHeader()}")
            if (input.contentLengthHeader() == "0") {
                // body can be null in this case
                val request = Request(input, null, routerFunction.requestPredicate.pathPattern)
                (handler as HandlerFunction<*, *>)(request)
            } else {
                val kType = routerFunction.requestPredicate.kType!!
                if (input.body == null) {
                    ResponseEntity.badRequest(body = "No body received but $kType was expected. If there is legitimately no body, add a Content-Length header with value '0'.")
                } else {
                    try {
                        val bodyObject = Json.decodeFromString(serializer(kType), input.body)
                        val request = Request(input, bodyObject, routerFunction.requestPredicate.pathPattern)
                        (handler as HandlerFunction<*, *>)(request)
                    } catch (mfe: MissingFieldException) {
                        ResponseEntity.badRequest(body = "Invalid request. Error is ${mfe.message}")
                    } catch (se: SerializationException) {
                        ResponseEntity.badRequest(body = "Could not deserialize body. Error is ${se.message}")
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
        return APIGatewayProxyResponseEvent()
            .withStatusCode(404)
            .withHeaders(mapOf("Content-Type" to "text/plain"))
            .withBody("No match found for route '$httpMethod' '$path' which accepts $acceptedMediaTypes")
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
        // though this may only be allowed for non-authenticated requests
        return APIGatewayProxyResponseEvent().withStatusCode(responseEntity.statusCode)
            .withHeaders(mapOf(CONTENT_TYPE to contentType, "Access-Control-Allow-Origin" to corsDomain))
            .withBody(body)
    }
}

