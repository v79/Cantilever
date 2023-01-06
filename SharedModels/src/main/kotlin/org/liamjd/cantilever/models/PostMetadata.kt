package org.liamjd.cantilever.models

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * This class represents the metadata provided in the frontmatter of the markdown file
 */
@Serializable
data class PostMetadata(val title: String, val template: String = "post", val date: LocalDate)
