package org.liamjd.cantilever.models.sqs

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.liamjd.cantilever.models.PostFrontmatter

typealias MarkdownSection = String

/**
 * An SqsMsgBody must be serializable to Json. These classes represent messages sent to the AWS SQS queue. Not all messages are appropriate to all queues; I may be better to split them at some point.
 */
@Serializable
sealed class SqsMsgBody {
    /**
     * Data class representing a message sent whenever a markdown file is uploaded to the source bucket
     */
    @Serializable
    data class MarkdownPostUploadMsg(val metadata: PostFrontmatter, val markdownText: String) : SqsMsgBody()

    /**
     * A repeat of the [PageMeta] class
     */
    @Serializable
    data class PageHandlebarsModelMsg(
        val key: String,
        val title: String,
        val template: String,
        val attributes: Map<String, String>,
        val sectionKeys: Map<String, String>,
        val url: String,
        val lastModified: Instant
    ) : SqsMsgBody()

    /**
     * Another repeat of the [PageMeta] class? Rationalize these!
     */
    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class PageModelMsg(
        val title: String,
        val srcKey: String,
        val templateKey: String,
        val isRoot: Boolean = false,
        val url: String,
        @EncodeDefault val lastModified: Instant = Clock.System.now(),
        val attributes: Map<String, String>,
        val sections: Map<String, MarkdownSection>
    ) : SqsMsgBody()

    /**
     * Message to send to the handlebars template engine when a .css file is uploaded.
     */
    @Serializable
    data class CssMsg(val srcKey: String, val destinationKey: String) : SqsMsgBody()

    /**
     * Once markdown processing is complete, it sends this message to the handlebars template engine
     * so that the complete web page can be generated
     */
    @Serializable
    data class HTMLFragmentReadyMsg(val fragmentKey: String, val metadata: PostFrontmatter) : SqsMsgBody()
}