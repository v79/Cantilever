package org.liamjd.cantilever.services

import org.liamjd.cantilever.models.sqs.MarkdownSQSMessage
import org.liamjd.cantilever.models.sqs.SqsMsgBody
import org.liamjd.cantilever.models.sqs.TemplateSQSMessage
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageResponse

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

    /**
     * Send a message to the given queue which converts Markdown to HTML fragments
     * @param toQueue the name of the SQS queue
     * @param body the message body, which will be serialized to Json
     * @param messageAttributes a map of [MessageAttributeValue], or an empty map
     * @return AWS [SendMessageResponse] or null
     */
    fun sendMarkdownMessage(
        toQueue: String,
        body: MarkdownSQSMessage,
        messageAttributes: Map<String, MessageAttributeValue> = emptyMap()
    ): SendMessageResponse?

    /**
     * Send a message to the given queue which renders a Handlebars template
     * @param toQueue the name of the SQS queue
     * @param body the message body, which will be serialized to Json
     * @param messageAttributes a map of [MessageAttributeValue], or an empty map
     * @return AWS [SendMessageResponse] or null
     */
    fun sendTemplateMessage(
        toQueue: String,
        body: TemplateSQSMessage,
        messageAttributes: Map<String, MessageAttributeValue> = emptyMap()
    ): SendMessageResponse?
}
