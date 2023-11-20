package org.liamjd.cantilever.models.rest

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.liamjd.cantilever.models.ContentNode

/**
 * Front end needs a list of templates, but we don't want to send the entire [ContentTree] over the wire. */
@Serializable
data class TemplateListDTO(val count: Int = 0, val lastUpdated: Instant, val templates: List<ContentNode.TemplateNode>)