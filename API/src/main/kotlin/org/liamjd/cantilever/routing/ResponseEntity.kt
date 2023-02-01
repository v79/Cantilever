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