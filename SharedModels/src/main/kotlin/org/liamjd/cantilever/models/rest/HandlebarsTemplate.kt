package org.liamjd.cantilever.models.rest

import kotlinx.serialization.Serializable
import org.liamjd.cantilever.models.Template
import org.liamjd.cantilever.openapi.APISchema

@APISchema
@Serializable
class HandlebarsTemplate(val template: Template, val body: String)