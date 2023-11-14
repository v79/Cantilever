package org.liamjd.cantilever.models.rest

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.liamjd.cantilever.models.ContentNode

/**
 * Front end needs a list of pages, but we don't want to send the entire [ContentTree] over the wire.
 */
@Serializable
class PageListDTO(val count: Int = 0, val lastUpdated: Instant, val pages: List<ContentNode.PageNode>, val folders: List<ContentNode.FolderNode>)