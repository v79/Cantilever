package org.liamjd.cantilever.models.rest

import kotlinx.serialization.Serializable
import org.liamjd.cantilever.models.SrcKey
import org.liamjd.cantilever.openapi.APISchema

@APISchema
@Serializable
data class ReassignIndexRequestDTO(val from: SrcKey, val to: SrcKey, val folder: SrcKey)
