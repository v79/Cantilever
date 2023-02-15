package org.liamjd.cantilever.routing.simple

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.liamjd.cantilever.routing.MimeType
import kotlin.reflect.KType

typealias SimpleHandlerFunction<I, T> = (request: SimpleRequest<I>) -> SimpleResponse<T>

data class SimpleRouterFunction<I, T : Any>(val handler: SimpleHandlerFunction<I, T>)

data class SimpleRoutePredicate(
    val method: String, var pathPattern: String,
    private var consumes: Set<MimeType>, private var produces: Set<MimeType>
) {
    var kType: KType? = null
}

/**
 * A SimpleRequest is created after a matching route has been found
 * @property apiRequest the full event from API Gateway
 * @property body the body, which will be empty for a GET but should have a value for PUT, POST etc
 * @property pathPattern the path pattern from the predicate, with the {parameters} etc
 */
data class SimpleRequest<I>(
    val apiRequest: APIGatewayProxyRequestEvent,
    val body: I,
    val pathPattern: String
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

data class SimpleResponse<T : Any>(
    val statusCode: Int,
    val body: T? = null,
    val headers: Map<String, String> = emptyMap()
) {
    var kType: KType? = null
}