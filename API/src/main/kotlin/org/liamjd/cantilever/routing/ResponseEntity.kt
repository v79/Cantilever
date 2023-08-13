package org.liamjd.cantilever.routing

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.liamjd.cantilever.routing.ResponseEntity.Companion.ok
import kotlin.reflect.KType
import kotlin.reflect.typeOf

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
        // 200
        inline fun <reified T : Any> ok(body: T? = null, headers: Map<String, String> = emptyMap()): ResponseEntity<T> {
            val tt: KType = typeOf<T>()
            return ResponseEntity<T>(HttpCodes.OK.code, body, headers).apply { kType = tt }
        }

        // 202
        inline fun <reified T : Any> accepted(body: T? = null, headers: Map<String, String> = emptyMap()): ResponseEntity<T> {
            val tt: KType = typeOf<T>()
            return ResponseEntity<T>(HttpCodes.ACCEPTED.code, body, headers).apply { kType = tt }
        }

        // 204
        /**
         * @param body will be ignored as no content is sent with the HTTP 204 message
         */
        inline fun <reified T : Any> noContent(body: T? = null, headers: Map<String, String> = emptyMap()): ResponseEntity<T> {
            val tt: KType = typeOf<T>()
            return ResponseEntity<T>(HttpCodes.NO_CONTENT.code, body, headers).apply { kType = tt }
        }

        // 404
        inline fun <reified T : Any> notFound(body: T? = null, headers: Map<String, String> = emptyMap()): ResponseEntity<T> {
            val tt: KType = typeOf<T>()
            return ResponseEntity<T>(HttpCodes.NOT_FOUND.code, body, headers).apply { kType = tt }
        }

        // 500
        inline fun <reified T : Any> serverError(body: T? = null, headers: Map<String, String> = emptyMap()): ResponseEntity<T> {
            val tt: KType = typeOf<T>()
            return ResponseEntity<T>(HttpCodes.SERVER_ERROR.code, body, headers).apply { kType = tt }
        }

        // 401
        inline fun <reified T : Any> unauthorized(body: T? = null, headers: Map<String, String> = emptyMap()): ResponseEntity<T> {
            val tt: KType = typeOf<T>()
            return ResponseEntity<T>(HttpCodes.UNAUTHORIZED.code, body, headers).apply { kType = tt }
        }

        // 400
        inline fun <reified T : Any> badRequest(body: T? = null, headers: Map<String, String> = emptyMap()): ResponseEntity<T> {
            val tt: KType = typeOf<T>()
            return ResponseEntity<T>(HttpCodes.BAD_REQUEST.code, body, headers).apply { kType = tt }
        }

        // 501
        inline fun <reified T : Any> notImplemented(body: T? = null, headers: Map<String, String> = emptyMap()): ResponseEntity<T> {
            val tt: KType = typeOf<T>()
            return ResponseEntity<T>(HttpCodes.NOT_IMPLEMENTED.code, body, headers).apply { kType = tt }
        }
    }
}