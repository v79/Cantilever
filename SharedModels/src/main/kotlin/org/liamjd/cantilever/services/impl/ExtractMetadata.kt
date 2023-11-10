package org.liamjd.cantilever.services.impl

import com.charleskorn.kaml.Yaml
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerializationException
import org.liamjd.cantilever.common.*
import org.liamjd.cantilever.models.PostFrontmatter

/**
 * Extract [PostFrontmatter] object from the markdown file.
 * It should be deliminated between a pair of '---' lines.
 * If this couldn't be extracted, it will attempt to construct a generic but valid object based on some assumptions.
 * @param filename the leaf file name, which should end in '.md'
 * @param source the entire contents of the markdown file
 */
@Deprecated("Use MetaData::PostMeta::buildFromYamlString instead")
fun extractPostMetadata(filename: String, source: String): PostFrontmatter {
    val metadataString = source.getFrontMatter()
    if (metadataString.isNotEmpty()) {
        try {
            return Yaml.default.decodeFromString(PostFrontmatter.serializer(), metadataString)
        } catch (se: SerializationException) {
            println("ERROR: Yaml exception: ${se.message}")
        }

    }
    return PostFrontmatter(
        title = filename.removeSuffix(FILE_TYPE.MD).removeSuffix("."),
        template = "post",
        slug = filename.removeSuffix(FILE_TYPE.MD).removeSuffix(".").removePrefix(S3_KEY.sources).toSlug(),
        date = LocalDate.now(),
        lastModified = Clock.System.now()
    )
}