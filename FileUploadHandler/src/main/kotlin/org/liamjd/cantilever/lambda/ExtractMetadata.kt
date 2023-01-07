package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.charleskorn.kaml.Yaml
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerializationException
import org.liamjd.cantilever.common.now
import org.liamjd.cantilever.common.toSlug
import org.liamjd.cantilever.models.PostMetadata

/**
 * Extract [PostMetadata] object from the markdown file.
 * It should be deliminated between a pair of '---' lines.
 * If this couldn't be extracted, it will attempt to construct a generic but valid object based on some assumptions.
 * This is using experimental Context Receivers to inject the [LambdaLogger].
 */
context (LambdaLogger)
fun extractPostMetadata(filename: String, source: String): PostMetadata {
    val metadataString = source.substringAfter("---").substringBeforeLast("---")
    if (metadataString.isNotEmpty()) {
        try {
            return Yaml.default.decodeFromString(PostMetadata.serializer(), metadataString)
        } catch (se: SerializationException) {
            println("Yaml exception: ${se.message}") // TODO logger
        }

    }
    return PostMetadata(
        title = filename.removeSuffix(".md"),
        template = "post",
        slug = filename.removeSuffix(".md").toSlug(),
        date = LocalDate.now()
    )
}