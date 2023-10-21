package org.liamjd.cantilever.models.rest

import kotlinx.serialization.Serializable
import org.liamjd.cantilever.models.PostMeta

/**
 * REST API model wrapping a [PostMeta] metadata object and the source string. This is what is edited/saved.
 */
@Serializable
class MarkdownPost(val metadata: PostMeta) {
    var body: String = ""

    override fun toString(): String {
        val sBuilder = StringBuilder()
        sBuilder.appendLine(separator)
        sBuilder.appendLine("title: ${metadata.title}")
        sBuilder.appendLine("template: ${metadata.templateKey}")
        sBuilder.appendLine("date: ${metadata.date}") // TODO: format date
        sBuilder.appendLine("slug: ${metadata.url}")
        sBuilder.appendLine(separator)
        sBuilder.appendLine(body)
        return sBuilder.toString()
    }

    companion object {
        const val separator = "---"
    }
}