package org.liamjd.cantilever.models.sqs

import kotlinx.serialization.Serializable
import org.liamjd.cantilever.models.ContentNode

/**
 * Represents a message sent to the AWS SQS queue when a markdown file is uploaded to the source bucket
 */
@Serializable
sealed class MarkdownSQSMessage {

    /**
     * Data class representing a message sent whenever a markdown [ContentNode.PostNode] file is uploaded to the source bucket
     */
    @Serializable
    data class PostUploadMsg(val metadata: ContentNode.PostNode, val markdownText: String) : MarkdownSQSMessage()

    /**
     * Data class representing a message sent whenever a markdown [ContentNode.PageNode] file is uploaded to the source bucket
     */
    @Serializable
    data class PageUploadMsg(val metadata: ContentNode.PageNode, val markdownText: String) : MarkdownSQSMessage()
}