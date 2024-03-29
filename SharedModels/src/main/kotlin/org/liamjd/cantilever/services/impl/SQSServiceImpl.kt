package org.liamjd.cantilever.services.impl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.liamjd.cantilever.models.sqs.ImageSQSMessage
import org.liamjd.cantilever.models.sqs.MarkdownSQSMessage
import org.liamjd.cantilever.models.sqs.TemplateSQSMessage
import org.liamjd.cantilever.services.SQSService
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageResponse

class SQSServiceImpl(override val region: Region) : SQSService {

    private val client = SqsClient.builder().region(region).build()

    override fun sendMarkdownMessage(
        toQueue: String,
        body: MarkdownSQSMessage,
        messageAttributes: Map<String, MessageAttributeValue>
    ): SendMessageResponse? {
        val request = SendMessageRequest.builder()
            .queueUrl(toQueue)
            .messageAttributes(messageAttributes)
            .messageBody(Json.encodeToString(body))
            .build()
        return client.sendMessage(request)
    }

    override fun sendTemplateMessage(
        toQueue: String,
        body: TemplateSQSMessage,
        messageAttributes: Map<String, MessageAttributeValue>
    ): SendMessageResponse? {
        val request = SendMessageRequest.builder()
            .queueUrl(toQueue)
            .messageAttributes(messageAttributes)
            .messageBody(Json.encodeToString(body))
            .build()
        return client.sendMessage(request)
    }

    override fun sendImageMessage(
        toQueue: String,
        body: ImageSQSMessage,
        messageAttributes: Map<String, MessageAttributeValue>
    ): SendMessageResponse? {
        val request = SendMessageRequest.builder()
            .queueUrl(toQueue)
            .messageAttributes(messageAttributes)
            .messageBody(Json.encodeToString(body))
            .build()
        return client.sendMessage(request)
    }

}