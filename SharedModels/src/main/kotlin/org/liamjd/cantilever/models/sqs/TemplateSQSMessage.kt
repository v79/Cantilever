package org.liamjd.cantilever.models.sqs

import kotlinx.serialization.Serializable
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.SrcKey

/**
 * Represents a message sent to the AWS SQS queue when a file is ready to be rendered to HTML
 */
@Serializable
sealed class TemplateSQSMessage {

    /**
     * Message to send to trigger the handlebars renderer for the Post
     */
    data class RenderPostMsg(val fragmentSrcKey: SrcKey, val metadata: ContentNode.PostNode) : TemplateSQSMessage()

    /**
     * Message to send to trigger the handlebars renderer for the Page
     */
    data class RenderPageMsg(val fragmentSrcKey: SrcKey, val metadata: ContentNode.PageNode) : TemplateSQSMessage()

    /**
     * Message to send to trigger the handlebars renderer for any other (text) file
     */
    data class StaticRenderMsg(val srcKey: SrcKey, val destinationKey: SrcKey) : TemplateSQSMessage()
}