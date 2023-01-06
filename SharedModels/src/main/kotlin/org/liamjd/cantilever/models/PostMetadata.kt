package org.liamjd.cantilever.models

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import org.liamjd.cantilever.common.toSlug

/**
 * This class represents the metadata provided in the frontmatter of the markdown file
 * The [slug] field should be provided, but if not it will be calculated from the title
 * in the format "The Title Is Hello" becomes "the-title-is-hello"
 */
@Serializable
data class PostMetadata(val title: String, val template: String = "post", val slug: String = title.toSlug(), val date: LocalDate)

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

        fun String.toSlug() : Slug {
            return Slug(this.replace(reserved, "-").lowercase())
        }
    }
}