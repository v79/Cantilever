package org.liamjd.cantilever.routing

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent

/**
 * A RequestPredicate is descriptor of a route set up in the router
 * @param method the HTTP method (GET, PUT etc)
 * @param pathPattern the string representing the route, e.g. /customers/get/{id}
 * @param consumes a set of Mime Types it accepts
 * @param produces a set of Mime Types it replies with
 */
data class RequestPredicate(
    val method: String,
    var pathPattern: String,
    private var consumes: Set<MimeType>,
    private var produces: Set<MimeType>
) {
    val accepts
        get() = consumes
    val supplies
        get() = produces

    fun match(request: APIGatewayProxyRequestEvent) = RequestMatchResult(
        matchPath = pathMatches(request.path,pathPattern),
        matchMethod = methodMatches(request),
        matchAcceptType = acceptMatches(request, produces),
        matchContentType = when {
            consumes.isEmpty() -> true
            request.contentType() == null -> false
            else -> {
                val requestsType = request.contentType()
                requestsType?.let {
                    consumes.contains(MimeType.parse(it))
                } ?: false
            }
        }
    )

    // I need to remove the UriTemplate stuff because I don't understand it
    private fun pathMatches(inputPath: String, routePath: String): Boolean {
        // WAS request.path?.let { UriTemplate.from(pathPattern).matches(it) } ?: false,
        val routeParts = routePath.split("/")
        val inputParts = inputPath.split("/")

        if (routeParts.size != inputParts.size) {
            return false
        }
        for (i in routeParts.indices) {
            if (routeParts[i].startsWith("{") && routeParts[i].endsWith("}")) {
               // OK, nothing to do here
            } else {
                if (inputParts[i] != routeParts[i]) {
                   return false
                }
            }
        }
        return true
    }

    private fun acceptMatches(request: APIGatewayProxyRequestEvent, produces: Set<MimeType>): Boolean {
        return when {
            produces.isEmpty() && request.acceptedMediaTypes().isEmpty() -> true
            else -> produces
                .firstOrNull { request.acceptedMediaTypes().any { acceptedType -> it == acceptedType } } != null
        }
    }

    private fun methodMatches(request: APIGatewayProxyRequestEvent) = method.equals(request.httpMethod, true)

    fun matchedAcceptType(acceptedMediaTypes: List<MimeType>): MimeType? =
        produces
            .firstOrNull { acceptedMediaTypes.any { acceptedType -> it.isCompatibleWith(acceptedType) } }

    fun expects(mimeTypes: Set<MimeType>?): RequestPredicate {
        mimeTypes?.let {
            consumes = mimeTypes
        }
        return this
    }

    fun supplies(mimeTypes: Set<MimeType>?): RequestPredicate {
        mimeTypes?.let {
            produces = mimeTypes
        }
        return this
    }

}

data class RequestMatchResult(
    val matchPath: Boolean = false,
    val matchMethod: Boolean = false,
    val matchAcceptType: Boolean = false,
    val matchContentType: Boolean = false
) {
    val matches
        get() = matchPath && matchMethod && matchAcceptType && matchContentType
}

