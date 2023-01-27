package org.liamjd.cantilever.routing

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import org.liamjd.cantilever.routing.ResponseEntity.Companion.ok
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class Router {

    val sModule = SerializersModule { }

    val routes = mutableListOf<RouterFunction<*, *>>()

    val consumeByDefault = setOf(MimeType.json)
    val produceByDefault = setOf(MimeType.json)

    inline fun <reified I, T : Any> get(pattern: String, noinline handlerFunction: HandlerFunction<I, T>) =
        defaultRequestPredicate(
            pattern = pattern,
            method = "GET",
            consuming = emptySet(),
            handlerFunction = handlerFunction
        )

    inline fun <reified I, T : Any> post(pattern: String, noinline handlerFunction: HandlerFunction<I, T>) =
        defaultRequestPredicate(
            pattern = pattern,
            method = "POST",
            handlerFunction = handlerFunction
        )

    /*
        fun <I, T> put(pattern: String, handlerFunction: HandlerFunction<I, T>) =
            defaultRequestPredicate(
                pattern = pattern,
                method = "PUT",
                handlerFunction = handlerFunction
            )

        fun <I, T> patch(pattern: String, handlerFunction: HandlerFunction<I, T>) =
            defaultRequestPredicate(
                pattern = pattern,
                method = "PATCH",
                handlerFunction = handlerFunction
            )

        fun <I, T> delete(pattern: String, handlerFunction: HandlerFunction<I, T>) =
            defaultRequestPredicate(
                pattern = pattern,
                method = "DELETE",
                consuming = emptySet(),
                handlerFunction = handlerFunction
            )
    */
    inline fun <reified I, T : Any> defaultRequestPredicate(
        pattern: String,
        method: String,
        consuming: Set<MimeType> = consumeByDefault,
        noinline handlerFunction: HandlerFunction<I, T>
    ) =
        RequestPredicate(
            method = method,
            pathPattern = pattern,
            consumes = consuming,
            produces = produceByDefault
        ).also { predicate ->
            routes += RouterFunction(predicate, handlerFunction)
        }

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

data class RouterFunction<I, T : Any>(
    val requestPredicate: RequestPredicate,
    val handler: HandlerFunction<I, T>,
) {
    val sModule = SerializersModule { }
    inline fun <reified I> getInputSerializer() = sModule.serializer<I>()
    inline fun <reified O> getOutputSerializer() = sModule.serializer<O>()
}

data class Request<I>(
    val apiRequest: APIGatewayProxyRequestEvent,
    val body: I,
    val pathPattern: String = apiRequest.path
)

/**
 * A ResponseEntity contents all the components needed to respond to any request
 * @param statusCode the HTTP Status Code
 * @param body the object returned from the server, which could be text, an object to be serialized later, or null
 * @param headers the HTTP response headers, which should always include a Content-Type header
 * @property kType internal property to keep track of the type of the body, for serialization
 * It is recommended that you use one of the companion functions, like [ok], to construct response entities.
 */
@Serializable
data class ResponseEntity<T : Any>(
    val statusCode: Int,
    val body: T? = null,
    val headers: Map<String, String> = emptyMap(),
) {
    @Transient
    var kType: KType? = null

    companion object {
        /**
         * Create a default successful response with the body and headers provided
         */
        inline fun <reified T : Any> ok(body: T? = null, headers: Map<String, String> = emptyMap()): ResponseEntity<T> {
            val tt: KType = typeOf<T>()
            return ResponseEntity<T>(HttpCodes.OK.code, body, headers).apply { kType = tt }
        }

        // TODO: Other typical responses, such as 'created', 'accepted', 'no content', 'bad request', 'not found' etc

        inline fun <reified T : Any> notFound(body: T? = null, headers: Map<String, String> = emptyMap()): ResponseEntity<T> {
            val tt: KType = typeOf<T>()
            return ResponseEntity<T>(HttpCodes.NOT_FOUND.code, body, headers).apply { kType = tt }
        }

        inline fun <reified T : Any> serverError(body: T? = null, headers: Map<String, String> = emptyMap()): ResponseEntity<T> {
            val tt: KType = typeOf<T>()
            return ResponseEntity<T>(HttpCodes.SERVER_ERROR.code, body, headers).apply { kType = tt }
        }
    }
}

/**
 * Collection of useful HTTP codes I'm likely to need
 */
enum class HttpCodes(val code: Int, val message: String) {
    OK(200,"OK"),
    CREATED(201,"Created"),
    ACCEPTED(202,"Accepted"),
    NO_CONTENT(204,"No Content"),
    BAD_REQUEST(400,"Bad Request"),
    UNAUTHORIZED(401,"Unauthorized"),
    FORBIDDEN(403,"Forbidden"),
    NOT_FOUND(404,"Not Found"),
    METHOD_NOT_ALLOWED(405,"Method Not Allowed"),
    TEAPOT(418,"I'm a teapot"),
    SERVER_ERROR(500,"Internal Server Error"),
    NOT_IMPLEMENTED(501,"Not Implemented"),
}