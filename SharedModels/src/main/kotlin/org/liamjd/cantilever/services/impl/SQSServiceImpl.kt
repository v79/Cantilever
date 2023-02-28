package org.liamjd.cantilever.services.impl

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.liamjd.cantilever.models.sqs.SqsMsgBody
import org.liamjd.cantilever.services.SQSService
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageResponse

class SQSServiceImpl(override val region: Region): SQSService {

    private val client = SqsClient.builder().region(region).build()

    override fun sendMessage(toQueue: String, body: SqsMsgBody, messageAttributes: Map<String, MessageAttributeValue>): SendMessageResponse? {
       val request = SendMessageRequest.builder()
           .queueUrl(toQueue)
           .messageAttributes(messageAttributes)
           .messageBody(Json.encodeToString(body))
           .build()

        return client.sendMessage(request)
    }
}