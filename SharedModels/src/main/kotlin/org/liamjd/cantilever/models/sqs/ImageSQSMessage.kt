package org.liamjd.cantilever.models.sqs

import kotlinx.serialization.Serializable
import org.liamjd.cantilever.models.ContentNode

/**
 * Represents a message sent to the AWS SQS queue when an image file is ready to be processed and resized
 */
@Serializable
sealed class ImageSQSMessage {

    @Serializable
    data class ResizeImageMsg(val projectDomain: String, val metadata: ContentNode.ImageNode) : ImageSQSMessage()

    @Serializable
    data class CopyImagesMsg(val projectDomain: String, val imageList: List<String>) : ImageSQSMessage()
}