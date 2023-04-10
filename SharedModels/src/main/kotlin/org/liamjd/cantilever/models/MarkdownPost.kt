package org.liamjd.cantilever.models

import kotlinx.serialization.Serializable

@Serializable
class MarkdownPost(val metadata: Post) {
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