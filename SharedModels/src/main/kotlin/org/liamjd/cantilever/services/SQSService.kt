package org.liamjd.cantilever.services

import org.liamjd.cantilever.models.sqs.SqsMsgBody
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
}
