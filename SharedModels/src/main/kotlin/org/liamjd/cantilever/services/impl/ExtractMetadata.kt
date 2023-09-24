package org.liamjd.cantilever.services.impl

import com.charleskorn.kaml.Yaml
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerializationException
import org.liamjd.cantilever.common.*
import org.liamjd.cantilever.models.PostMetadata

/**
 * Extract [PostMetadata] object from the markdown file.
 * It should be deliminated between a pair of '---' lines.
 * If this couldn't be extracted, it will attempt to construct a generic but valid object based on some assumptions.
 * @param filename the leaf file name, which should end in '.md'
 * @param source the entire contents of the markdown file
 */
fun extractPostMetadata(filename: String, source: String): PostMetadata {
    val metadataString = source.getFrontMatter()
    if (metadataString.isNotEmpty()) {
        try {
            return Yaml.default.decodeFromString(PostMetadata.serializer(), metadataString)
        } catch (se: SerializationException) {
            println("ERROR: Yaml exception: ${se.message}")
        }

    }
    return PostMetadata(
        title = filename.removeSuffix(FILE_TYPE.MD).removeSuffix("."),
        template = "post",
        slug = filename.removeSuffix(FILE_TYPE.MD).removeSuffix(".").removePrefix(S3_KEY.sources).toSlug(),
        date = LocalDate.now(),
        lastModified = Clock.System.now()
    )
}