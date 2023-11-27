package org.liamjd.cantilever.openapi

import kotlinx.serialization.Serializable

@Serializable
data class APISchemaProperty(val name: String, val type: String)
@Serializable
data class APISchemaClassModel(val className: String, val properties: List<APISchemaProperty> = emptyList()) {
}