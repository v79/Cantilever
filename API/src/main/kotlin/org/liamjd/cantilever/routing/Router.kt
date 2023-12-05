package org.liamjd.cantilever.routing

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.liamjd.cantilever.auth.Authorizer
import org.liamjd.cantilever.common.toSlug
import kotlin.reflect.typeOf

/**
 * The Router class is the core of the routing mechanism
 * It provides the functions responding to the HTTP request methods (GET, POST) etc. and matches them to
 * a [HandlerFunction] provided by the library user
 * Use the function [lambdaRouter] to create a new router, rather than the direct constructor
 * @property routes a map of all the routes which have been declared
 */
class Router internal constructor() {

    val routes: MutableMap<RequestPredicate, RouterFunction<*, *>> = mutableMapOf()
    private val groups: MutableSet<Group> = mutableSetOf()

    val consumeByDefault = setOf(MimeType.json)
    val produceByDefault = setOf(MimeType.json)

    private val schemaParser: APISchemaParser = OpenAPISchemaParser()

    /**
     * A group is a collection of routes which share a common parent path.
     */
    class Group(
        val pathPattern: String, val spec: Spec.Tag? = null
    ) {
        constructor(pathPattern: String) : this(pathPattern, Spec.Tag(pathPattern, ""))
    }

    /**
     * HTTP GET
     * This only matches the embedded response Request<T> -> ResponseEntity.ok(T) route definition
     */
    inline fun <reified I, T : Any> get(
        pattern: String, noinline handlerFunction: HandlerFunction<I, T>,
    ): RequestPredicate {
        val requestPredicate = defaultRequestPredicate(
            pattern = pattern, method = "GET", consuming = emptySet(), handlerFunction = handlerFunction
        ).also {
            routes[it] = RouterFunction(it, handlerFunction)
        }
        return requestPredicate
    }

    /**
     * HTTP GET
     * This allows the definition of the route in an external function, with an optional [Spec.PathItem] object to provide OpenAPI documentation
     */
    inline fun <reified I, T : Any> get(
        pattern: String, noinline handlerFunction: HandlerFunction<I, T>, spec: Spec.PathItem? = null
    ): RequestPredicate {
        val requestPredicate = defaultRequestPredicate(
            pattern = pattern, method = "GET", consuming = emptySet(), spec = spec, handlerFunction = handlerFunction
        ).also {
            routes[it] = RouterFunction(it, handlerFunction)
        }
        return requestPredicate
    }

    /**
     * HTTP POST, used to create new data
     */
    inline fun <reified I, T : Any> post(
        pattern: String, noinline handlerFunction: HandlerFunction<I, T>
    ): RequestPredicate {
        val requestPredicate = defaultRequestPredicate(
            pattern = pattern, method = "POST", consuming = consumeByDefault, handlerFunction = handlerFunction
        ).also {
            it.kType = typeOf<I>()
            routes[it] = RouterFunction(it, handlerFunction)
        }
        return requestPredicate
    }

    /**
     * HTTP POST, used to create new data
     * This allows the definition of the route in an external function, with an optional [Spec.PathItem] object to provide OpenAPI documentation
     */
    inline fun <reified I, T : Any> post(
        pattern: String,
        noinline handlerFunction: HandlerFunction<I, T>,
        spec: Spec.PathItem? = null,
    ): RequestPredicate {
        val requestPredicate = defaultRequestPredicate(
            pattern = pattern,
            method = "POST",
            consuming = consumeByDefault,
            spec = spec,
            handlerFunction = handlerFunction
        ).also {
            it.kType = typeOf<I>()
            routes[it] = RouterFunction(it, handlerFunction)
        }
        return requestPredicate
    }

    /**
     * HTTP PUT, to update or replace existing data
     */
    inline fun <reified I, T : Any> put(
        pattern: String, noinline handlerFunction: HandlerFunction<I, T>,
    ): RequestPredicate {
        val requestPredicate = defaultRequestPredicate(
            pattern = pattern, method = "PUT", consuming = consumeByDefault, handlerFunction = handlerFunction
        ).also {
            it.kType = typeOf<I>()
            routes[it] = RouterFunction(it, handlerFunction)
        }
        return requestPredicate
    }

    /**
     * HTTP PUT, to update or replace existing data
     * This allows the definition of the route in an external function, with an optional [Spec.PathItem] object to provide OpenAPI documentation
     */
    inline fun <reified I, T : Any> put(
        pattern: String,
        noinline handlerFunction: HandlerFunction<I, T>,
        spec: Spec.PathItem? = null,
    ): RequestPredicate {
        val requestPredicate = defaultRequestPredicate(
            pattern = pattern,
            method = "PUT",
            consuming = consumeByDefault,
            spec = spec,
            handlerFunction = handlerFunction
        ).also {
            it.kType = typeOf<I>()
            routes[it] = RouterFunction(it, handlerFunction)
        }
        return requestPredicate
    }

    /**
     * HTTP PATCH, untested
     */
    inline fun <reified I, T : Any> patch(
        pattern: String, noinline handlerFunction: HandlerFunction<I, T>
    ): RequestPredicate {
        val requestPredicate = defaultRequestPredicate(
            pattern = pattern, method = "PATCH", consuming = emptySet(), handlerFunction = handlerFunction
        ).also {
            it.kType = typeOf<I>()
            routes[it] = RouterFunction(it, handlerFunction)
        }
        return requestPredicate
    }

    /**
     * HTTP DELETE
     */
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

    /**
     * HTTP DELETE
     * This allows the definition of the route in an external function, with an optional [Spec.PathItem] object to provide OpenAPI documentation
     */
    inline fun <reified I, T : Any> delete(
        pattern: String,
        noinline handlerFunction: HandlerFunction<I, T>,
        spec: Spec.PathItem? = null,
    ): RequestPredicate {
        val requestPredicate = defaultRequestPredicate(
            pattern = pattern,
            method = "DELETE",
            consuming = consumeByDefault,
            spec = spec,
            handlerFunction = handlerFunction
        ).also {
            it.kType = typeOf<I>()
            routes[it] = RouterFunction(it, handlerFunction)
        }
        return requestPredicate
    }

    /**
     * The default request predicate forms the basis of all the HTTP methods.
     * It sets a default set of [MimeType]s for consuming and producing, and creates a [RequestPredicate] object.
     * The defaults are `application/json` for both consuming and producing but these can be overridden with the `supplies` and `consumes` modifiers.
     */
    inline fun <reified I, T : Any> defaultRequestPredicate(
        pattern: String,
        method: String,
        consuming: Set<MimeType> = consumeByDefault,
        spec: Spec.PathItem? = null,
        noinline handlerFunction: HandlerFunction<I, T>
    ) = RequestPredicate(
        method = method, pathPattern = pattern, consumes = consuming, produces = produceByDefault
    ).also { predicate ->
        routes[predicate] = RouterFunction(predicate, handlerFunction)
        if (spec != null) {
            predicate.addSpec(spec)
        }
    }

    /**
     * Create a grouping of routes which share a common parent path.
     * It does this by creating a new instance of the Router, then copying its routes into the parent router, modifying the pathParameter of each
     */
    fun group(parentPath: String, spec: Spec.Tag? = null, block: Router.() -> Unit) {
        val childRouter = Router()
        childRouter.block()
        if (spec != null) {
            groups.add(Group(parentPath, spec))
        } else {
            groups.add(Group(parentPath))
        }
        childRouter.routes.forEach {
            val routeCopy = it.key.copy(pathPattern = parentPath + it.key.pathPattern).apply { addSpecs(it.key.specs) }
            routes[routeCopy] =
                it.value.copy().apply { it.value.requestPredicate.pathPattern = parentPath + it.key.pathPattern }
        }
    }

    /**
     * Create a grouping of routes which require authentication by the given authenticator or permission or role thingy
     * It does this by creating a new instance of the Router, then copying its routes into the parent router, adding the role/permission requirement to each
     */
    fun auth(authorizer: Authorizer, block: Router.() -> Unit) {
        val childRouter = Router()
        childRouter.block()
        childRouter.routes.forEach {
            val routeCopy = it.value.copy(authorizer = authorizer)
            routes[it.key] = routeCopy
        }
        groups.addAll(childRouter.groups)
    }

    /**
     * Utility function to list all the routes which have been declared. Useful for debugging.
     */
    fun listRoutes(): String {
        return routes.values.joinToString(separator = " ;") { "${it.requestPredicate.method} ${it.requestPredicate.pathPattern}  <${it.requestPredicate.accepts} (${it.requestPredicate.kType}) -> ${it.requestPredicate.supplies}>" }
    }

    /**
     * Function to build an OpenAPI 3.0.1 specification from the routes which have been declared.
     * This really needs to be broken up and rationalize.
     */
    fun openAPI(): String {

        // local schema definitions
        val schemas = schemaParser.loadSchemaFile("openapi/schema/api-schema.yaml")

        // build reference objects
        val authorizers = routes.mapNotNull { it.value.authorizer }.distinct()

        val sb = StringBuilder()
        sb.appendLine("openapi: 3.0.3")
        sb.appendLine("info:")
        sb.appendLine("  title: Cantilever API")
        sb.appendLine("  description: API for Cantilever, providing methods for managing the content of a static website")
        sb.appendLine("  version: 0.0.8")
        sb.appendLine("servers:")
        sb.appendLine("  - url: https://api.cantilevers.org")
        // add tags as defined by the route groups
        if (groups.isNotEmpty()) {
            sb.appendLine("tags:")
            groups.forEach { group ->
                group.spec?.also { spec ->
                    sb.appendLine("  - name: ${spec.name}")
                    if (spec.description.isNotEmpty()) {
                        sb.appendLine("    description: ${spec.description}")
                    }
                } ?: sb.appendLine("  - name: ${group.pathPattern}")
            }
        } else {
            sb.appendLine("tags: []")
        }
        sb.appendLine("paths:")
        routes.entries.groupBy { it.key.pathPattern }.forEach { path ->
            sb.appendLine("  ${path.key}:")
            path.value.forEach { route ->
                sb.appendLine("    ${route.key.method.lowercase()}:")
                // find matching Tags based on the containing group, if any
                if (groups.isNotEmpty()) {
                    groups.findLast { group ->
                        route.key.pathPattern.substringBeforeLast("/").startsWith(group.pathPattern)
                    }?.let {
                        sb.appendLine("      tags:")
                        it.spec?.let { spec ->
                            sb.appendLine("        - ${spec.name}")
                        } ?: {
                            sb.appendLine("        - ${it.pathPattern}")
                        }
                    }
                }
                // display summary and description, if any
                route.key.specs.forEach { spec ->
                    if (spec is Spec.PathItem) {
                        sb.appendLine("      summary: ${spec.summary}")
                        sb.appendLine("      description: ${spec.description}")
                    }
                }
                // display authorization, if any
                route.value.authorizer?.let { authorizer ->
                    sb.appendLine("      security:")
                    sb.appendLine("        - ${authorizer.simpleName.toSlug()}: []")
                }
                // display path variables, if any
                if (route.key.pathVariables.isNotEmpty()) {
                    sb.appendLine("      parameters:")
                    route.key.pathVariables.forEach { variable ->
                        sb.appendLine("        - name: $variable")
                        sb.appendLine("          in: path")
                        sb.appendLine("          required: true")
                        sb.appendLine("          schema:")
                        // TODO: this should the type and the format but my Request class doesn't have that information
                        sb.appendLine("            type: string")
                    }
                }
                // display schema to consume, if any
                when (route.key.method) {
                    "PUT", "POST" -> {
                        // in my API, PUT may not have a body if the X-Content-Length header is zero
                        sb.appendLine("      requestBody:")
                        sb.appendLine("        content:")
                        route.key.accepts.forEach { accepts ->
                            sb.appendLine("          ${accepts.toString().lowercase()}:")
                            sb.appendLine("            schema:")
                            // TODO: I need to figure out how to get the type of the body
                            // This should be schema: object, and then use reflection to interrogate the class?
                            // but for now, all I have is a kType, which I can't use to get the class
                            // perhaps using a reference like  $ref: '#/components/schemas/Order'   ?
                            // val first = (kType.classifier as KClass<*>).constructors.first().parameters.first().type.toString()
                            route.value.requestPredicate.kType?.let { kType ->
                                val typeString =
                                    kType.toString().removeSuffix("(Kotlin reflection is not available)").trim()
                                if (typeString != "kotlin.Unit") {
                                    // here we insert a reference IF we have a schema for this type defined
                                    if (schemas != null) {
                                        val schema = schemas.classes.find { it.className == typeString }
                                        if (schema != null) {
                                            sb.appendLine("              ${'$'}ref: '#/components/schemas/$typeString'")
                                        } else {
                                            sb.appendLine("              type: object")
                                            sb.appendLine("              description: $typeString")
                                        }
                                    } else {
                                        sb.appendLine("              type: object")
                                        sb.appendLine("              description: $typeString")
                                    }
                                } else {
                                    sb.appendLine("              type: string") // fallback to string
                                }
                            }
                        }
                    }
                }
                // display response options
                sb.appendLine("      responses:")
                sb.appendLine("        200:")
                sb.appendLine("          description: OK")
                sb.appendLine("          content:")
                route.key.supplies.forEach { supplies ->
                    sb.appendLine("            ${supplies.toString().lowercase()}:")
                    sb.appendLine("              schema:")
                    sb.appendLine("                type: string")
                }
            }
        }
        // components:
        sb.appendLine("components:")
        // display security schemes, if any
        if (authorizers.isNotEmpty()) {
            sb.appendLine("  securitySchemes:")
            authorizers.forEach { authorizer ->
                if (authorizer != null) {
                    sb.appendLine("    ${authorizer.simpleName.toSlug()}:")
                    sb.appendLine("      type: ${authorizer.type}")
                    sb.appendLine("      scheme: bearer")
                    sb.appendLine("      bearerFormat: JWT")
                }
            }
        }
        //  schemas:
        if (schemas != null) {
            sb.appendLine("  schemas:")
            schemas.classes.forEach { clazz ->
                sb.appendLine("    ${clazz.className}:")
                sb.appendLine("      type: object")
                sb.appendLine("      properties:")
                clazz.properties.forEach { property ->
                    sb.appendLine("        ${property.name}:")
                    if (OpenAPISchemaParser.VALID_TYPES.contains(property.type)) {
                        sb.appendLine("          type: ${property.type}")
                    } else {
                        sb.appendLine("          type: object")
                        sb.appendLine("          description: ${property.type}")
                    }
                }
            }
        }
        //  requestBodies:
        return sb.toString()
    }

    companion object {
        const val CONTENT_TYPE = "Content-Type"
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
 * Shorthand for a function which responds to a [Request] and returns a [ResponseEntity]
 */
typealias HandlerFunction<I, T> = (request: Request<I>) -> ResponseEntity<T>

/**
 * A RouterFunction combines a [RequestPredicate] (the HTTP method, the path, and the accept/return types, with the [HandlerFunction] which responds to the [Request] and returns a [ResponseEntity]
 */
data class RouterFunction<I, T : Any>(
    val requestPredicate: RequestPredicate, val handler: HandlerFunction<I, T>, var authorizer: Authorizer? = null,
)

/**
 * Simple wrapper around the AWS [APIGatewayProxyRequestEvent] class, created after a matching route has been found
 * @property apiRequest the full event from API Gateway
 * @property body the body, which will be empty for a GET but should have a value for PUT, POST etc
 * @property pathPattern the path pattern from the predicate, with the {parameters} etc
 * @property pathParameters a map of matching path parameters and their values, i.e. path /get/{id} with id = 3 becomes `map[id] = 3`
 */
data class Request<I>(
    val apiRequest: APIGatewayProxyRequestEvent, val body: I, val pathPattern: String
) {
    // TODO: Ideally, this should be Map<String, Any> but I can't figure out how to do that
    val pathParameters: Map<String, String> by lazy {
        buildMap {
            val inputParts = apiRequest.path.split("/")
            val routeParts = pathPattern.split("/")
            for (i in routeParts.indices) {
                if (routeParts[i].startsWith("{") && routeParts[i].endsWith("}")) {
                    put(routeParts[i].removeSurrounding("{", "}"), inputParts[i])
                }
            }
        }
    }
}

/**
 * Collection of useful HTTP codes I'm likely to need
 */
enum class HttpCodes(val code: Int, val message: String) {
    OK(200, "OK"), CREATED(201, "Created"), ACCEPTED(202, "Accepted"), NO_CONTENT(204, "No Content"), BAD_REQUEST(
        400,
        "Bad Request"
    ),
    UNAUTHORIZED(401, "Unauthorized"), FORBIDDEN(403, "Forbidden"), NOT_FOUND(404, "Not Found"), METHOD_NOT_ALLOWED(
        405,
        "Method Not Allowed"
    ),
    TEAPOT(418, "I'm a teapot"), SERVER_ERROR(500, "Internal Server Error"), NOT_IMPLEMENTED(501, "Not Implemented"),
}