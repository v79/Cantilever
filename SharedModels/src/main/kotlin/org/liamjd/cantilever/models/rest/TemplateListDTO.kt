package org.liamjd.cantilever.models.rest

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.openapi.APISchema

/**
 * Data Transfer Object (DTO) representing a list of templates.
 *
 * @property count The total number of templates in the list. Default is 0.
 * @property lastUpdated The timestamp of the last update to the template list.
 * @property templates A list of [ContentNode.TemplateNode] objects representing the templates.
 */
@APISchema
@Serializable
data class TemplateListDTO(val count: Int = 0, val lastUpdated: Instant, val templates: List<ContentNode.TemplateNode>)