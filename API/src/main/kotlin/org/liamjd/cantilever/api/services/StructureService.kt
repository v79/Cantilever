package org.liamjd.cantilever.api.services

import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.charleskorn.kaml.Yaml
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerializationException
import org.liamjd.cantilever.common.now
import org.liamjd.cantilever.common.toSlug
import org.liamjd.cantilever.models.PostMetadata

class StructureService {

    /**
     * Extract the PostMetadata from the given markdown source file
     * It will attempt to construct missing data based on the filename if it cannot parse the frontmatter in the .md file
     * @param filename the name (S3 key) of the file
     * @param markdownSource the entire source of the markdown file
     * @return A [PostMetadata] object containing the title, template, slug, date and last modified date
     */
    fun extractPostMetadata(filename: String, markdownSource: String): PostMetadata {
        /**
         * Extract [PostMetadata] object from the markdown file.
         * It should be deliminated between a pair of '---' lines.
         * If this couldn't be extracted, it will attempt to construct a generic but valid object based on some assumptions.
         * This is using experimental Context Receivers to inject the [LambdaLogger].
         */
        val metadataString = markdownSource.substringAfter("---").substringBeforeLast("---")
        if (metadataString.isNotEmpty()) {
            try {
                return Yaml.default.decodeFromString(PostMetadata.serializer(), metadataString)
            } catch (se: SerializationException) {
                println("ERROR: Yaml exception: ${se.message}")
            }

        }
        return PostMetadata(
            title = filename.removeSuffix(".md"),
            template = "post",
            slug = filename.removeSuffix(".md").removePrefix("sources").toSlug(),
            date = LocalDate.now(),
            lastModified = LocalDateTime.now()
        )
    }
}