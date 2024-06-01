package org.liamjd.cantilever.models.rest

import kotlinx.serialization.Serializable
import org.liamjd.apiviaduct.schema.OpenAPISchema
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.openapi.APISchema

/**
 * Front end needs a list of folders, but we don't want to send the entire [ContentTree] over the wire.
 */
@APISchema
@OpenAPISchema
@Serializable
class FolderListDTO(val count: Int = 0, val folders: List<ContentNode.FolderNode>)
