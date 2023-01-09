package org.liamjd.cantilever.models

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.liamjd.cantilever.common.now
import org.liamjd.cantilever.common.toSlug

/**
 * This class represents the metadata provided in the frontmatter of the markdown file
 * The [slug] field should be provided, but if not it will be calculated from the title
 * in the format "The Title Is Hello" becomes "the-title-is-hello"
 */
@OptIn(ExperimentalSerializationApi::class) // required for @EncodeDefault
@Serializable
data class PostMetadata(
    val title: String,
    @EncodeDefault val template: String = "post",
    @EncodeDefault val slug: String = title.toSlug(),
    val date: LocalDate,
    @EncodeDefault val lastModified: LocalDateTime = LocalDateTime.now()
)

/**
 * Experimental inline value class for Slug?
 */
@JvmInline
value class Slug(private val slug: String) {
    init {
        require(slug.matches(legitimate))
    }

    companion object {

        private val legitimate = "INSERT VALID CHARACTERS HERE".toRegex()
        private val reserved = "[;/?:@&=+\$, ]".toRegex()

        fun String.toSlug(): Slug {
            return Slug(this.replace(reserved, "-").lowercase())
        }
    }
}