package org.liamjd.cantilever.models.sqs

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.liamjd.cantilever.models.PostMetadata

typealias MarkdownSection = String

/**
 * An SqsMsgBody must be serializable to Json
 */
@Serializable
sealed class SqsMsgBody {
    /**
     * Data class representing a message sent whenever a markdown file is uploaded to the source bucket
     */
    @Serializable
    data class MarkdownPostUploadMsg(val metadata: PostMetadata, val markdownText: String) : SqsMsgBody()

    @Serializable
    data class PageHandlebarsModelMsg(
        val key: String,
        val template: String,
        val attributes: Map<String, String>,
        val sectionKeys: Map<String, String>,
        val url: String
    ) : SqsMsgBody()

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class PageModelMsg(
        val title: String,
        val srcKey: String,
        val templateKey: String,
        val url: String,
        @EncodeDefault val lastModified: Instant = Clock.System.now(),
        val attributes: Map<String, String>,
        val sections: Map<String, MarkdownSection>
    ) : SqsMsgBody()

    @Serializable
    data class CssMsg(val srcKey: String, val destinationKey: String) : SqsMsgBody()

    /**
     * Once markdown processing is complete, it sends this message to the handlebars template engine
     * so that the complete web page can be generated
     */
    @Serializable
    data class HTMLFragmentReadyMsg(val fragmentKey: String, val metadata: PostMetadata) : SqsMsgBody()
}