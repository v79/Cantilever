package org.liamjd.cantilever.models.sqs

import kotlinx.serialization.Serializable
import org.liamjd.cantilever.common.MimeType
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.SrcKey

/**
 * Represents a message sent to the AWS SQS queue when a file is ready to be rendered to HTML
 */
@Serializable
sealed class TemplateSQSMessage {

    /**
     * Message to send to trigger the Handlebars renderer for the Post
     */
    @Serializable
    data class RenderPostMsg(
        val projectDomain: String,
        val fragmentSrcKey: SrcKey,
        val metadata: ContentNode.PostNode
    ) : TemplateSQSMessage()

    /**
     * Message to send to trigger the Handlebars renderer for the Page
     */
    @Serializable
    data class RenderPageMsg(
        val projectDomain: String,
        val fragmentSrcKey: SrcKey,
        val metadata: ContentNode.PageNode
    ) : TemplateSQSMessage()

    /**
     * Message to send to trigger the Handlebars renderer for any other (text) file
     */
    @Serializable
    data class StaticRenderMsg(
        val projectDomain: String,
        val srcKey: SrcKey,
        val metadata: ContentNode.StaticNode,
        val mimeType: MimeType
    ) : TemplateSQSMessage()
}