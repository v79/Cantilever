package org.liamjd.cantilever.models.rest

import kotlinx.serialization.Serializable
import org.liamjd.cantilever.models.SrcKey
import org.liamjd.cantilever.openapi.APISchema

@Serializable
@APISchema
class TemplateUseDTO(val count: Int, val pageKeys: List<SrcKey>, val postKeys: List<SrcKey>) {
}