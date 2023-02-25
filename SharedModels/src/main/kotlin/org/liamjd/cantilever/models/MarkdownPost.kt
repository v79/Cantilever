package org.liamjd.cantilever.models

import kotlinx.serialization.Serializable

@Serializable
class MarkdownPost(val post: Post) {
    var body: String = ""

    override fun toString(): String {
        val sBuilder = StringBuilder()
        sBuilder.appendLine(separator)
        sBuilder.appendLine("title: ${post.title}")
        sBuilder.appendLine("template: ${post.templateKey}")
        sBuilder.appendLine("date: ${post.date}") // TODO: format date
        sBuilder.appendLine("slug: ${post.url}")
        sBuilder.appendLine(separator)
        sBuilder.appendLine(body)
        return sBuilder.toString()
    }

    companion object {
        const val separator = "---"
    }
}