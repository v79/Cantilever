package org.liamjd.cantilever.routing

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable

class OpenAPISchemaParser {

    /**
     * Attempts to load the OpenAPI schema file and parse it into a Kotlin class
     */

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val parser = OpenAPISchemaParser()
            parser.loadSchemaFile("APISchema.yaml")
        }

        val VALID_TYPES = listOf("string", "number", "boolean", "array", "object", "null")
    }


    fun loadSchemaFile(schemaFile: String): OpenAPISchema? {
        val yaml = this.javaClass.classLoader.getResource(schemaFile)?.readText()
        if (yaml == null) {
            println("No schema file found")
            return null
        }
        println(yaml)

        return Yaml.default.decodeFromString(OpenAPISchema.serializer(), yaml)
    }
}

/**
 * https://spec.openapis.org/oas/v3.0.3#schemaObject
 */
@Serializable
data class OpenAPISchema(
    val classes: List<OpenAPIClass>
)

@Serializable
data class OpenAPIClass(
    val className: String,
    val properties: List<Property>
)

/**
 * https://spec.openapis.org/oas/v3.0.3#schemaObject
 * Properties are:
 * - title
 * - multipleOf
 * - maximum
 * - exclusiveMaximum
 * - minimum
 * - exclusiveMinimum
 * - maxLength
 * - minLength
 * - pattern
 * - maxItems
 * - minItems
 * - uniqueItems
 * - maxProperties
 * - minProperties
 * - required
 * - enum
 * Fixed fields are:
 * - nullable
 * - discriminator
 * - readOnly
 * - writeOnly
 * - xml
 * - externalDocs
 * - example
 * - deprecated
 */
@Serializable
data class Property(
    val name: String,
    val type: String
)