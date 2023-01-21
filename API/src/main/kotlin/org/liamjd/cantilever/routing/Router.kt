package org.liamjd.cantilever.routing

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent

class Router {

    val routes = mutableListOf<RouterFunction<*, *>>()

    private val consumeByDefault = setOf(MimeType.json)
    private val produceByDefault = setOf(MimeType.json)

    fun <I, T> get(pattern: String, handlerFunction: HandlerFunction<I, T>) =
        defaultRequestPredicate(
            pattern = pattern,
            method = "GET",
            consuming = emptySet(),
            handlerFunction = handlerFunction
        )


    private fun <I, T> defaultRequestPredicate(
        pattern: String,
        method: String,
        consuming: Set<MimeType> = consumeByDefault,
        handlerFunction: HandlerFunction<I, T>,
    ) =
        RequestPredicate(
            method = method,
            pathPattern = pattern,
            consumes = consuming,
            produces = produceByDefault
        ).also { routes += RouterFunction(it, handlerFunction) }

    companion object {
        fun router(routes: Router.() -> Unit) = Router().apply(routes)
    }

    fun listRoutes() {
        routes.forEach { route ->
            println("${route.requestPredicate.method} ${route.requestPredicate.pathPattern} <consumes: ${route.requestPredicate.accepts} -> produces: ${route.requestPredicate.supplies}>")
        }
    }

}

typealias HandlerFunction<I, T> = (request: Request<I>) -> ResponseEntity<T>

data class RouterFunction<I, T>(
    val requestPredicate: RequestPredicate,
    val handler: HandlerFunction<I, T>
)

data class Request<I>(
    val apiRequest: APIGatewayProxyRequestEvent,
    val body: I,
    val pathPattern: String = apiRequest.path
)

data class ResponseEntity<T>(
    val statusCode: Int,
    val body: T? = null,
    val headers: Map<String, String> = emptyMap()
) {
    companion object {
        fun <T> ok(body: T? = null, headers: Map<String, String> = emptyMap()) =
            ResponseEntity<T>(200, body, headers)
    }
}