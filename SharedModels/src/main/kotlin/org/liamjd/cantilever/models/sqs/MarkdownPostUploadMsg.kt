package org.liamjd.cantilever.models.sqs

import kotlinx.serialization.Serializable
import org.liamjd.cantilever.models.PostMetadata

/**
 * Data class representing a message sent whenever a markdown file is uploaded to the source bucket
 */
@Serializable
data class MarkdownPostUploadMsg(val metadata: PostMetadata, val markdownText: String)
