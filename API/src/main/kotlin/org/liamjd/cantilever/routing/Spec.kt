package org.liamjd.cantilever.routing

/**
 * OpenAPI specification for route elements, used to generate documentation
 */
sealed class Spec private constructor() {

    /**
     * https://spec.openapis.org/oas/v3.0.1#tag-object
     */
    data class Tag(val name: String, val description: String, val externalDocs: ExternalDocumentation? = null): Spec()

    /**
     * https://spec.openapis.org/oas/v3.0.1#external-documentation-object
     */
    data class ExternalDocumentation(val description: String, val url: String): Spec()

    /**
     * https://spec.openapis.org/oas/v3.0.1#path-item-object
     */
    data class PathItem(val summary: String = "", val description: String = ""): Spec()
}