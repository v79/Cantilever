package org.liamjd.cantilever.openapi

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import kotlin.reflect.KClass

class OpenAPISchemaProcessor(environment: SymbolProcessorEnvironment) : SymbolProcessor {

    private val logger = environment.logger

    override fun process(resolver: Resolver): List<KSAnnotated> {

        val processedList = mutableListOf<KSAnnotated>()

        val schemaClasses =
            resolver.getSymbolsWithAnnotation(annotationName = APISchema::class.qualifiedName!!, inDepth = false)
                .filterIsInstance<KSClassDeclaration>()

        if (!schemaClasses.iterator().hasNext()) {
            logger.warn("No classes found with @APISchema annotation")
            return emptyList()
        } else {
            schemaClasses.forEach {
                val kClassDeclaration = it
                logger.warn("Found class with @APISchema annotation: ${kClassDeclaration.simpleName.asString()}")
                kClassDeclaration.getAllProperties().forEach { property ->
                    logger.warn("\tProperty: ${property.simpleName.asString()}: ${property.type.toString()}")
                }
//                processedList.add(kClassDeclaration)
            }
        }

        return emptyList()
    }

    private fun Resolver.findAnnotations(kClass: KClass<*>) =
        getSymbolsWithAnnotation(kClass.qualifiedName.toString()).filterIsInstance<KSClassDeclaration>()
}

class OpenAPISchemaProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return OpenAPISchemaProcessor(environment)
    }
}