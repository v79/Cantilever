package org.liamjd.cantilever.routing.simple

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.liamjd.cantilever.common.MimeType
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
@OptIn(ExperimentalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
fun SimpleRouter.handleRequest(input: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
    val inputMethod = input.httpMethod
    var matchingRoute: Pair<SimpleRoutePredicate, SimpleRouterFunction<*, *>>? = null
    routes.forEach { route ->
        if ((route.key.method == inputMethod) && (pathMatches(input, route.key.pathPattern))) {
            matchingRoute = Pair(route.key, route.value)
            return@forEach
        }
    }

    val response = matchingRoute?.let { route ->
        val handler = route.second.handler
        // check the type specified in the request definition
        val response: SimpleResponse<*> = if (route.first.kType != null) {
            val kType = route.first.kType!!
            if (input.body != null && input.body.isNotEmpty()) {
                // attempt to deserialize the body
                try {
                    val bodyObject = Json.decodeFromString(serializer(kType), input.body)
                    val nRequest = SimpleRequest(input, bodyObject, matchingRoute!!.first.pathPattern)
                    (handler as SimpleHandlerFunction<*, *>)(nRequest)
                } catch (mfe: MissingFieldException) {
                    println("Could not deserialize as the body was incomplete.")
                    SimpleResponse(400, "Invalid request. Error is ${mfe.message}.")
                }
            } else {
                SimpleResponse(400, "No body supplied for request.")
            }
        } else {
            println("No request type was specified, likely Unit. Request has no body.")
            val nRequest = SimpleRequest(input, null, matchingRoute!!.first.pathPattern)
            (handler as SimpleHandlerFunction<*, *>)(nRequest)
        }
        response
    } ?: SimpleResponse(404, "Route not found")

    return APIGatewayProxyResponseEvent().withStatusCode(response.statusCode).withBody(response.body.toString())
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




