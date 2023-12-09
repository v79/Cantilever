package org.liamjd.cantilever.models.rest

import kotlinx.serialization.Serializable
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.openapi.APISchema

/**
 * REST API model wrapping a [ContentNode.PageNode] metadata object and the sources of each named section. This what is edited/saved.
 */
@APISchema
@Serializable
class MarkdownPageDTO(val metadata: ContentNode.PageNode, val body: String = "", val type: String? = null) {
    override fun toString(): String {
        val sBuilder = StringBuilder()
        sBuilder.apply {
            appendLine(SEPARATOR)
            appendLine("title: ${metadata.title}")
            appendLine("templateKey: ${metadata.templateKey}")
            metadata.attributes.forEach {
                appendLine("#${it.key}: ${it.value}")
            }
            metadata.sections.forEach {
                appendLine("$SEPARATOR #${it.key}")
                appendLine(it.value)
            }
        }
        return sBuilder.toString().trim()
    }

    companion object {
        const val SEPARATOR = "---"
    }
}
