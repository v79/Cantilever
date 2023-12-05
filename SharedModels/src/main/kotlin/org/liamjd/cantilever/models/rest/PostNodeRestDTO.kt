package org.liamjd.cantilever.models.rest

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.SrcKey
import org.liamjd.cantilever.openapi.APISchema

/**
 * Front end is unable to provide us with a [ContentNode.PostNode] object, so we need to create a DTO to pass back and forth
 */
@Serializable
@APISchema
class PostNodeRestDTO(
    val srcKey: SrcKey,
    val title: String,
    val templateKey: String,
    val date: LocalDate,
    val slug: String,
    val body: String,
    val attributes: Map<String, String> = emptyMap()
) {
    override fun toString(): String {
        val sBuilder = StringBuilder()
        sBuilder.appendLine(SEPARATOR)
        sBuilder.appendLine("title: $title")
        sBuilder.appendLine("templateKey: $templateKey")
        sBuilder.appendLine("date: $date") // TODO: format date
        sBuilder.appendLine("slug: $slug")
        sBuilder.appendLine(SEPARATOR)
        sBuilder.appendLine(body)
        return sBuilder.toString()
    }

    companion object {
        const val SEPARATOR = "---"
    }

    /**
     * Convert this DTO object into a real [ContentNode.PostNode]
     */
    fun toPostNode(): ContentNode.PostNode {
        val post = ContentNode.PostNode(
            title = title,
            templateKey = templateKey,
            slug = slug,
            date = date,
            attributes = attributes
        )
        post.srcKey = srcKey
        post.body = body
        return post
    }
}
