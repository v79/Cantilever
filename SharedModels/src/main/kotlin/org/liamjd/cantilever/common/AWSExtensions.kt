package org.liamjd.cantilever.common

import software.amazon.awssdk.services.sqs.model.MessageAttributeValue

/**
 * Utility function to create an SQS String Message Attribute.
 */
fun createStringAttribute(name: String, value: String): Map<String, MessageAttributeValue> {
    return mapOf(name to MessageAttributeValue.builder().dataType("String").stringValue(value).build())
}
