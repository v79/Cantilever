package org.liamjd.cantilever.routing

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent

abstract class RequestHandlerWrapper : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    abstract val router: Router

    override fun handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent =
        input.apply {
            headers = headers.mapKeys { it.key.lowercase() }
        }
            .let { handleRequest(input) }


    internal fun handleRequest(input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
        println("RequestHandlerWrapper: handleRequest(): looking for route which matches request  ${input.httpMethod} ${input.path} <${input.getHeader("Content-Type")}->${input.acceptedMediaTypes()}>")
        // find matching route
        val routes = router.routes as List<RouterFunction<Any, Any>>
        println("Valid routes:")
        router.listRoutes()
        println()
        val matchResults: List<RequestMatchResult> = routes.map { routerFunction: RouterFunction<Any, Any> ->
            val matchResult = routerFunction.requestPredicate.match(input)
            println("RequestHandlerWrapper: HandleRequest(): ${routerFunction.requestPredicate} matchResult=$matchResult")
            if (matchResult.matches) {
                val handler: HandlerFunction<Any, Any> = routerFunction.handler
                val response: ResponseEntity<out Any?> = try {
                    val requestBody = "faked"
                    val request = Request(input, requestBody, routerFunction.requestPredicate.pathPattern)
                    (handler as HandlerFunction<*, *>)(request)
                } catch (e: Exception) {
                    println("RequestHandlerWrapper: handleRequest() Error! ${e.message}")
                    ResponseEntity(500)
                }
                return createResponse(response)
            }
            matchResult

        }
        return createNoMatchingRouteResponse(input.httpMethod, input.path, input.acceptedMediaTypes())
    }

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


    private fun <T> createResponse(responseEntity: ResponseEntity<T>): APIGatewayProxyResponseEvent =
        APIGatewayProxyResponseEvent().withStatusCode(200)
            .withHeaders(mapOf("Content-Type" to "application/json"))
            .withBody("createResponse() function responding with T=$responseEntity")

    private fun <T> createErrorResponse(): APIGatewayProxyResponseEvent =
        APIGatewayProxyResponseEvent().withStatusCode(500)
}