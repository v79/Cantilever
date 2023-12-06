package org.liamjd.cantilever.models.sqs

import kotlinx.serialization.Serializable
import org.liamjd.cantilever.models.SrcKey

/**
 * Represents a message sent to the AWS SQS queue when an image file is ready to be processed and resized
 */
@Serializable
sealed class ImageSQSMessage {

    @Serializable
    data class ResizeImageMsg(val srcKey: SrcKey, val mimeType: String) : ImageSQSMessage()
}