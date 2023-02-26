package org.liamjd.cantilever.models.sqs

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

typealias MarkdownSection = String

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PageModelMsg(
    val key: String,
    val template: String,
    @EncodeDefault val lastModified: Instant = Clock.System.now(),
    val attributes: Map<String, String>,
    val sections: Map<String, MarkdownSection>
)