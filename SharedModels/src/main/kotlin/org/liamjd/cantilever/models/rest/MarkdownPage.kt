package org.liamjd.cantilever.models.rest

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.liamjd.cantilever.models.Page

/**
 * REST API model wrapping a [Page] metadata object and the sources of each named section
 */
@Serializable
class MarkdownPage(
    val metadata: Page,
    val body: String = ""
) {
    // the front-end sends a body in its MarkdownContent class, but is not needed for a Page

    override fun toString(): String {
        val sBuilder = StringBuilder()
        sBuilder.apply {
            appendLine(separator)
            appendLine("title: ${metadata.title}")
            appendLine("template: ${metadata.templateKey}")
            metadata.attributes.forEach {
                appendLine("#${it.key}: ${it.value}")
            }
            metadata.sections.forEach {
                appendLine("$separator #${it.key}")
                appendLine(it.value)
            }
        }
        return sBuilder.toString().trim()
    }

    companion object {
        const val separator = "---"
    }
}