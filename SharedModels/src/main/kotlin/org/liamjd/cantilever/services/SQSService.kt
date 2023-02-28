package org.liamjd.cantilever.services

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.liamjd.cantilever.models.PostMetadata
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageResponse

typealias MarkdownSection = String

/**
 * General interface for sending messages to SQS
 */
interface SQSService {
    val region: Region

    /**
     * Send a message to the given queue
     * @param toQueue the name of the SQS queue
     * @param body the message body, which will be serialized to Json
     * @param messageAttributes a map of [MessageAttributeValue], or an empty map
     * @return AWS [SendMessageResponse] or null
     */
    fun sendMessage(
        toQueue: String,
        body: SqsMsgBody,
        messageAttributes: Map<String, MessageAttributeValue> = emptyMap()
    ): SendMessageResponse?
}

@Serializable
sealed class SqsMsgBody {
    @Serializable
    data class MarkdownPostUploadMsg(val metadata: PostMetadata, val markdownText: String) : SqsMsgBody()

    @Serializable
    data class PageHandlebarsModelMsg(
        val key: String,
        val template: String,
        val attributes: Map<String, String>,
        val sectionKeys: Map<String, String>
    ) : SqsMsgBody()

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    data class PageModelMsg(
        val key: String,
        val template: String,
        @EncodeDefault val lastModified: Instant = Clock.System.now(),
        val attributes: Map<String, String>,
        val sections: Map<String, MarkdownSection>
    ) : SqsMsgBody()
}