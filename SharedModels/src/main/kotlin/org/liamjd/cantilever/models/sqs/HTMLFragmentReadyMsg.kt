package org.liamjd.cantilever.models.sqs

import kotlinx.serialization.Serializable
import org.liamjd.cantilever.models.PostMetadata

/**
 * Once markdown processing is complete, it sends this message to the handlebars template engine
 * so that the complete web page can be generated
 */
@Serializable
data class HTMLFragmentReadyMsg(val fragmentKey: String, val metadata: PostMetadata)
