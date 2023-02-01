package org.liamjd.cantilever.routing.simple

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import org.liamjd.cantilever.routing.MimeType
import kotlin.reflect.KType

/**
 * This is a playground for a better router class
 */
class SimpleRouter {
    val routes = mutableMapOf<SimpleRoutePredicate, SimpleRouterFunction<*, *>>()

    inline fun <reified I, T : Any> get(path: String, noinline handlerFunction: SimpleHandlerFunction<I, T>) {
        val r = SimpleRoutePredicate("GET", path, emptySet(), emptySet())
        val rP = SimpleRouterFunction(handlerFunction)
        routes[r] = rP
    }

    fun listRoutes() {
        routes.forEach {
            println("${it.key.method} ${it.key.pathPattern} -> ${it.value}")
        }
    }

}

fun SimpleRouter.group(parentPath: String, block: SimpleRouter.() -> Unit) {
    val childRoute = SimpleRouter()
    childRoute.block()
    childRoute.routes.forEach {
        val routeCopy = it.key.copy(pathPattern = parentPath + it.key.pathPattern)
        routes[routeCopy] = it.value
    }
}

@Suppress("UNCHECKED_CAST")
fun SimpleRouter.handleRequest(input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
    val inputMethod = input.httpMethod
    val inputPath = input.path

    var matchingRoute: SimpleRouterFunction<*, *>? = null
    routes.forEach { route ->
        if ((route.key.method == inputMethod) && (route.key.pathPattern == inputPath)) {
            matchingRoute = route.value
            return@forEach
        }
    }

    val resp = matchingRoute?.let {
        val handler = matchingRoute!!.handler
        val nRequest = SimpleRequest(input, null, inputPath)
        (handler as SimpleHandlerFunction<*, *>)(nRequest)
    } ?: SimpleResponse(404, "Route not found")

    val apiGWResponse = APIGatewayProxyResponseEvent().withStatusCode(resp.statusCode).withBody(resp.body.toString())

    return apiGWResponse
}

fun simpleRouter(block: SimpleRouter.() -> Unit): SimpleRouter {
    val router = SimpleRouter()
    router.block()
    return router
}

typealias SimpleHandlerFunction<I, T> = (request: SimpleRequest<I>) -> SimpleResponse<T>

data class SimpleRouterFunction<I, T : Any>(val handler: SimpleHandlerFunction<I, T>)

data class SimpleRoutePredicate(
    val method: String, var pathPattern: String,
    private var consumes: Set<MimeType>, private var produces: Set<MimeType>
)

data class SimpleRequest<I>(
    val apiRequest: APIGatewayProxyRequestEvent,
    val body: I,
    val pathPattern: String = apiRequest.path
)

data class SimpleResponse<T : Any>(
    val statusCode: Int,
    val body: T? = null,
    val headers: Map<String, String> = emptyMap()
) {
    var kType: KType? = null
}


