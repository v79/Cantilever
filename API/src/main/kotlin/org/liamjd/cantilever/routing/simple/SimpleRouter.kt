package org.liamjd.cantilever.routing.simple

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.liamjd.cantilever.routing.MimeType
import kotlin.reflect.typeOf

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

    inline fun <reified I, T : Any> post(path: String, noinline handlerFunction: SimpleHandlerFunction<I, T>) {
        val r = SimpleRoutePredicate("POST", path, setOf(MimeType.json), emptySet())
        r.kType = typeOf<I>()
        val rP = SimpleRouterFunction(handlerFunction)
        routes[r] = rP
    }

    fun listRoutes() {
        routes.forEach {
            println("${it.key.method} ${it.key.pathPattern} -> ${it.value}")
        }
    }

}

/**
 * Route grouping
 */
fun SimpleRouter.group(parentPath: String, block: SimpleRouter.() -> Unit) {
    val childRouter = SimpleRouter()
    childRouter.block()
    childRouter.routes.forEach {
        val routeCopy = it.key.copy(pathPattern = parentPath + it.key.pathPattern)
        routes[routeCopy] = it.value
    }
}

/**
 * Route authentication
 */
fun SimpleRouter.auth(thingy: String, block: SimpleRouter.() -> Unit) {
    val childRouter = SimpleRouter()
    childRouter.block()
    childRouter.routes.forEach {
        // add some authentication property/requirement to the child routes based on the thingy
        routes[it.key] = it.value
    }
}

/**
 * Find a matching route and then execute its [SimpleHandlerFunction]
 */
@Suppress("UNCHECKED_CAST")
fun SimpleRouter.handleRequest(input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
    val inputMethod = input.httpMethod
    val inputPath = input.path

    var matchingRoute: Pair<SimpleRoutePredicate, SimpleRouterFunction<*, *>>? = null
    routes.forEach { route ->
        if ((route.key.method == inputMethod) && (pathMatches(input, route.key.pathPattern))) {
            matchingRoute = Pair(route.key, route.value)
            return@forEach
        }
    }

    val resp = matchingRoute?.let { route ->
        val handler = matchingRoute!!.second.handler
        println("Now to deserialize the body")
        val bodyObject: Any? = if (input.body != null && input.body.isNotEmpty()) {
            println("Body: ${input.body}")
            val hf = handler as SimpleHandlerFunction<*, *>
            val bodyType = matchingRoute!!.first.kType
            bodyType?.let { kt ->
                Json.decodeFromString(serializer(kt), input.body)
            }
        } else {
            null
        }
        println("BodyObject is : $bodyObject")
        val nRequest = SimpleRequest(input, bodyObject, matchingRoute!!.first.pathPattern)
        (handler as SimpleHandlerFunction<*, *>)(nRequest)
    } ?: SimpleResponse(404, "Route not found")

    return APIGatewayProxyResponseEvent().withStatusCode(resp.statusCode).withBody(resp.body.toString())
}

/**
 * Look if the input.path matches the routePath, handling {parameters}
 */
fun pathMatches(input: APIGatewayProxyRequestEvent, routePath: String): Boolean {
    println("Comparing input: ${input.path} to route: $routePath")
    val routeParts = routePath.split('/')
    val inputParts = input.path.split('/')

    println("RouteParts: $routeParts")
    println("InputParts: $inputParts")
    val pathParams = mutableMapOf<Int, String>()
    for (i in routeParts.indices) {
        if (routeParts[i].startsWith("{") && routeParts[i].endsWith("}")) {
            pathParams[i] = routeParts[i].removeSurrounding("{", "}")
        }
    }
    println("Found pathParams: $pathParams")


    var matching = true
    if (routeParts.size != inputParts.size) {
        return false
    }
    for (i in routeParts.indices) {
        if (pathParams[i] != null) {
            matching = true
        } else {
            if (inputParts[i] != routeParts[i]) {
                return false
            }
        }
    }
    return matching
}

/**
 * Builder function to create the router
 */
fun simpleRouter(block: SimpleRouter.() -> Unit): SimpleRouter {
    val router = SimpleRouter()
    router.block()
    return router
}




