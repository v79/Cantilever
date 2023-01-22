package org.liamjd.cantilever.routing

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent

/**
 * A RequestPredicate is a combination of an http method, a path
 * a set of Mime Types it can produce (supplies)
 * a set of Mime Times it accepts (consumes or expects)
 */
data class RequestPredicate(
    val method: String,
    val pathPattern: String,
    private var consumes: Set<MimeType>,
    private var produces: Set<MimeType>,

) {
    val accepts
        get() = consumes
    val supplies
        get() = produces

    fun match(request: APIGatewayProxyRequestEvent) = RequestMatchResult(
        matchPath = request.path?.let { UriTemplate.from(pathPattern).matches(it) } ?: false,
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

