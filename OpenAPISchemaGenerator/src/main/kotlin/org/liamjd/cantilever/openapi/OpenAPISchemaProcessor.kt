package org.liamjd.cantilever.openapi

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias

/**
 * Searches for classes with the [APISchema] annotation, and generates a yaml file listing all matching classes, their properties and types.
 * This isn't a comprehensive data model, as it only lists the simple primitive types. But it is a start.
 */
class OpenAPISchemaProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {

    private val logger = environment.logger
    private val generator = environment.codeGenerator

    override fun process(resolver: Resolver): List<KSAnnotated> {

        val sBuilder = StringBuilder()
        sBuilder.appendLine("classes:")

        val schemaClasses =
            resolver.getSymbolsWithAnnotation(annotationName = APISchema::class.qualifiedName!!, inDepth = false)
                .filterIsInstance<KSClassDeclaration>()

        if (!schemaClasses.iterator().hasNext()) {
            logger.warn("No classes found with @APISchema annotation")
            return emptyList()
        } else {
            schemaClasses.forEach { kClassDeclaration ->
                logger.warn("Found class with @APISchema annotation: ${kClassDeclaration.simpleName.asString()}")
                sBuilder.appendLine(" - className: ${kClassDeclaration.qualifiedName?.asString()}")
                sBuilder.appendLine("   properties:")
                kClassDeclaration.getAllProperties().forEach { property ->
//                    logger.warn("\tProperty: ${property.simpleName.asString()}: ${property.type}")
                    sBuilder.appendLine("    - name: ${property.simpleName.asString()}")
                    val type = lookupType(property.type.resolve())
                    sBuilder.appendLine("      type: $type")
                }

            }

            val output =
                generator.createNewFile(Dependencies.ALL_FILES, "openapi.schema", "api-schema", "yaml")
            output.write(sBuilder.toString().toByteArray(Charsets.UTF_8))
            logger.warn("Written output file to build/generated/ksp/kotlin/main/openapi/schema/api-schema.yaml")
        }

        return emptyList()
    }

    /**
     * Attempts to convert a Kotlin KSType into one of the valid Javascript primitives
     * Possible values are:
     * - null
     * - string
     * - boolean
     * - number
     * - array - I could attempt to map List, Set, Map and Array to this type
     * - object
     * I won't be able to do a comprehensive mapping, so most things will default to 'object' or 'string'
     */
    private fun lookupType(propertyType: KSType): String {
        val type = if (propertyType.declaration is KSTypeAlias) {
            (propertyType.declaration as KSTypeAlias).type.resolve().toString()
        } else {
            propertyType.toString()
        }
        return when (type) {
            "String" -> "string"
            "Int", "Float", "Long" -> "number"
            "Boolean" -> "boolean"
            else -> "object($propertyType)"
        }
    }
}

class OpenAPISchemaProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return OpenAPISchemaProcessor(environment)
    }
}