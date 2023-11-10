package org.liamjd.cantilever.models.rest

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.liamjd.cantilever.models.ContentNode

/**
 * Front end needs a list of posts, but we don't want to send the entire [ContentTree] over the wire.
 */
@Serializable
class PostListDTO(val count: Int = 0, val lastUpdated: Instant, val posts: List<ContentNode.PostNode>)
