package org.liamjd.cantilever.models.rest

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.openapi.APISchema

/**
 * Front end needs a list of posts, but we don't want to send the entire [ContentTree] over the wire.
 */
@APISchema
@Serializable
class PostListDTO(val count: Int = 0, val lastUpdated: Instant, val posts: List<ContentNode.PostNode>)
