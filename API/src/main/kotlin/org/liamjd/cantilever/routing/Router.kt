package org.liamjd.cantilever.routing

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.liamjd.cantilever.auth.Authorizer

/**
 * The Router class is the core of the routing mechanism
 * It provides the functions responding to the HTTP request methods (GET, POST) etc and matches them to
 * a [HandlerFunction] provided by the library user
 * Use the function [lambdaRouter] to create a new router, rather than the direct constructor
 */
class Router internal constructor() {

    val routes: MutableMap<RequestPredicate, RouterFunction<*, *>> =
        mutableMapOf<RequestPredicate, RouterFunction<*, *>>()

    val consumeByDefault = setOf(MimeType.json)
    val produceByDefault = setOf(MimeType.json)

    inline fun <reified I, T : Any> get(
        pattern: String, noinline handlerFunction: HandlerFunction<I, T>
    ): RequestPredicate {
        val requestPredicate = defaultRequestPredicate(
            pattern = pattern, method = "GET", consuming = emptySet(), handlerFunction = handlerFunction
        ).also {
            routes[it] = RouterFunction(it, handlerFunction)
        }
        return requestPredicate
    }

    inline fun <reified I, T : Any> post(
        pattern: String, noinline handlerFunction: HandlerFunction<I, T>
    ): RequestPredicate {
        val requestPredicate = defaultRequestPredicate(
            pattern = pattern, method = "POST", consuming = emptySet(), handlerFunction = handlerFunction
        ).also {
            routes[it] = RouterFunction(it, handlerFunction)
        }
        return requestPredicate
    }

    inline fun <reified I, T : Any> put(
        pattern: String, noinline handlerFunction: HandlerFunction<I, T>
    ): RequestPredicate {
        val requestPredicate = defaultRequestPredicate(
            pattern = pattern, method = "PUT", consuming = emptySet(), handlerFunction = handlerFunction
        ).also {
            routes[it] = RouterFunction(it, handlerFunction)
        }
        return requestPredicate
    }

    inline fun <reified I, T : Any> patch(
        pattern: String, noinline handlerFunction: HandlerFunction<I, T>
    ): RequestPredicate {
        val requestPredicate = defaultRequestPredicate(
            pattern = pattern, method = "PATCH", consuming = emptySet(), handlerFunction = handlerFunction
        ).also {
            routes[it] = RouterFunction(it, handlerFunction)
        }
        return requestPredicate
    }

    inline fun <reified I, T : Any> delete(
        pattern: String, noinline handlerFunction: HandlerFunction<I, T>
    ): RequestPredicate {
        val requestPredicate = defaultRequestPredicate(
            pattern = pattern, method = "DELETE", consuming = emptySet(), handlerFunction = handlerFunction
        ).also {
            routes[it] = RouterFunction(it, handlerFunction)
        }
        return requestPredicate
    }

    /*inline fun <reified I, T : Any> authorizedRequestPredicate(
        permissionString: String,
        noinline handlerFunction: HandlerFunction<I, T>,
        function: () -> RequestPredicate
    ): RequestPredicate {

        val rp = function.invoke()

        rp.also { predicate ->
            routes[path] = RouterFunction(predicate, handlerFunction, permissionString)
        }
        return rp
    }*/

    inline fun <reified I, T : Any> defaultRequestPredicate(
        pattern: String,
        method: String,
        consuming: Set<MimeType> = consumeByDefault,
        noinline handlerFunction: HandlerFunction<I, T>
    ) = RequestPredicate(
        method = method, pathPattern = pattern, consumes = consuming, produces = produceByDefault
    ).also { predicate ->
        routes[predicate] = RouterFunction(predicate, handlerFunction)
    }

    /**
     * Utility function to list all the routes which have been declared. Useful for debugging.
     */
    fun listRoutes() {
        routes.forEach { route ->
            println("${route.value.requestPredicate.method} ${route.value.requestPredicate.pathPattern} <consumes: ${route.value.requestPredicate.accepts} -> produces: ${route.value.requestPredicate.supplies}>")
        }
    }

}

/**
 * Entry point for creating an AWS Lambda routing function
 */
fun lambdaRouter(block: Router.() -> Unit): Router {
    val router = Router()
    router.block()
    return router
}

/**
 * Create a grouping of routes which share a common parent path
 * It does this by creating a new instance of the Router, then copying its routes into the parent router, modifying the pathParameter of each
 */
fun Router.group(parentPath: String, block: Router.() -> Unit) {
    val childRouter = Router()
    childRouter.block()
    childRouter.routes.forEach {
        val routeCopy = it.key.copy(pathPattern = parentPath + it.key.pathPattern)
        routes[routeCopy] =
            it.value.copy().apply { it.value.requestPredicate.pathPattern = parentPath + it.key.pathPattern }
        println("Added $routeCopy to $this routes")
    }
}

/**
 * Create a grouping of routes which require authentication by the given authenticator or permission or role thingy
 * It does this by creating a new instance of the Router, then copying its routes into the parent router, adding the role/permission requirement to each
 */
fun Router.auth(authorizer: Authorizer, block: Router.() -> Unit) {
    val childRouter = Router()
    childRouter.block()
    childRouter.routes.forEach {
        val routeCopy = it.value.copy(authorizer = authorizer)
        routes[it.key] = routeCopy
    }
}

/**
 * Shorthand for a function which responds to a [Request] and returns a [ResponseEntity]
 */
typealias HandlerFunction<I, T> = (request: Request<I>) -> ResponseEntity<T>

/**
 * A RouterFunction combines a [RequestPredicate] (the HTTP method, the path, and the accept/return types, with the [HandlerFunction] which responds to the [Request] and returns a [ResponseEntity]
 */
data class RouterFunction<I, T : Any>(
    val requestPredicate: RequestPredicate, val handler: HandlerFunction<I, T>, var authorizer: Authorizer? = null
)

/**
 * Simple wrapper around the AWS [APIGatewayProxyRequestEvent] class, created after a matching route has been found
 * @property apiRequest the full event from API Gateway
 * @property body the body, which will be empty for a GET but should have a value for PUT, POST etc
 * @property pathPattern the path pattern from the predicate, with the {parameters} etc
 * @property pathParameters a map of matching path parameters and their values, i.e path /get/{id} with id = 3 becomes `map[id] = 3`
 */
data class Request<I>(
    val apiRequest: APIGatewayProxyRequestEvent,
    val body: I,
    val pathPattern: String = apiRequest.path
) {
    val pathParameters: Map<String, String> by lazy {
        val tmpMap = mutableMapOf<String, String>()
        val inputParts = apiRequest.path.split("/")
        val routeParts = pathPattern.split("/")
        for (i in routeParts.indices) {
            if (routeParts[i].startsWith("{") && routeParts[i].endsWith("}")) {
                val param = routeParts[i].removeSurrounding("{", "}")
                tmpMap[param] = inputParts[i]
            }
        }
        tmpMap.toMap()
    }
}

/**
 * Collection of useful HTTP codes I'm likely to need
 */
enum class HttpCodes(val code: Int, val message: String) {
    OK(200, "OK"), CREATED(201, "Created"),
    ACCEPTED(202, "Accepted"),
    NO_CONTENT(204, "No Content"),
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    TEAPOT(418, "I'm a teapot"),
    SERVER_ERROR(500, "Internal Server Error"),
    NOT_IMPLEMENTED(501, "Not Implemented"),
}