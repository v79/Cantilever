package org.liamjd.cantilever.routing

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.liamjd.cantilever.common.MimeType
import java.util.*
import kotlin.reflect.KType

/**
 * A RequestPredicate is descriptor of a route set up in the router
 * @param method the HTTP method (GET, PUT, etc.)
 * @param pathPattern the string representing the route, e.g. `/customers/get/{id}`
 * @param consumes a set of Mime Types it accepts
 * @param produces a set of Mime Types it replies with
 * @property accepts is an alias for consumes
 * @property supplies is an alias for produces
 * @property kType is the Kotlin type of the request body, or null
 * @property specs a set of OpenAPI specifications for this route
 */
data class RequestPredicate(
    val method: String,
    var pathPattern: String,
    internal var consumes: Set<MimeType>,
    internal var produces: Set<MimeType>,
) {
    var kType: KType? = null
    val accepts
        get() = consumes
    val supplies
        get() = produces

    private val routeParts
        get() = pathPattern.split("/")

    val pathVariables: List<String>
        get() = routeParts.filter { it.startsWith("{") && it.endsWith("}") }.map { it.removeSurrounding("{", "}") }

    var headerOverrides = mutableMapOf<String, String>()
        private set

    var requirements: MutableSet<(APIGatewayProxyRequestEvent) -> Boolean> = mutableSetOf()
        private set

    // OpenAPI specifications
    var specs: MutableSet<Spec> = mutableSetOf()

    fun match(request: APIGatewayProxyRequestEvent) =
        RequestMatchResult(matchPath = pathMatches(request.path),
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
            })

    private fun pathMatches(inputPath: String): Boolean {
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
            else -> produces.firstOrNull {
                request.acceptedMediaTypes().any { acceptedType -> it == acceptedType }
            } != null
        }
    }

    private fun methodMatches(request: APIGatewayProxyRequestEvent) = method.equals(request.httpMethod, true)

    fun matchedAcceptType(acceptedMediaTypes: List<MimeType>): MimeType? =
        produces.firstOrNull { acceptedMediaTypes.any { acceptedType -> it.isCompatibleWith(acceptedType) } }


    /**
     * Add an OpenAPI specification to this route
     */
    fun addSpec(spec: Spec) {
        specs.add(spec)
    }

    /**
     * Add a set of OpenAPI specifications to this route
     */
    fun addSpecs(newSpecs: Set<Spec>) {
        specs.addAll(newSpecs)
    }

    /**
     * Add a requirement to this route, which must be satisfied for the route to be processed
     */
    fun addRequirement(newRequirement: (APIGatewayProxyRequestEvent) -> Boolean) {
        requirements.add(newRequirement)
    }

    override fun toString(): String {
        val sB = StringBuilder()
        sB.append(method.uppercase(Locale.getDefault())).append(" ")
        sB.append(pathPattern).append("")
        sB.append("[" + { consumes.joinToString(", ") } + "]->[" + produces.joinToString(", ") + "]")
        sB.append(" requirements: $requirements")
        return sB.toString()
    }
}

/**
 * Override the default consumes mime type
 */
fun RequestPredicate.expects(mimeTypes: Set<MimeType>?): RequestPredicate {
    mimeTypes?.let {
        this.consumes = mimeTypes
    }
    return this
}

/**
 * Override the default produces mime type
 */
fun RequestPredicate.supplies(mimeTypes: Set<MimeType>?): RequestPredicate {
    mimeTypes?.let {
        produces = mimeTypes
    }
    return this
}

/**
 * Add additional or overriding headers to this particular route
 */
fun RequestPredicate.addHeaders(headers: Map<String, String>): RequestPredicate {
    headerOverrides.putAll(headers)
    return this
}

/**
 * Add an OpenAPI specification to this route, it may be a little cleaner than using the constructor
 */
fun RequestPredicate.spec(pathItem: Spec.PathItem): RequestPredicate {
    this.addSpec(pathItem)
    return this
}

/**
 * Stores the results of the matching operation across path, method, and mime types
 * @property matches is true if all components are true
 */
data class RequestMatchResult(
    val matchPath: Boolean = false,
    val matchMethod: Boolean = false,
    val matchAcceptType: Boolean = false,
    val matchContentType: Boolean = false
) {
    val matches
        get() = matchPath && matchMethod && matchAcceptType && matchContentType
}

