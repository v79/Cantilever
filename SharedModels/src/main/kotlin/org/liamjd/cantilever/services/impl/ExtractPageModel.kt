package org.liamjd.cantilever.services.impl

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * A page model is different from a Post, in that it can have multiple named markdown sections.
 * It will start with basic metadata though, and the 'template' property is required.
 * Sections must be named, and the name must start with '#'
 * The basic format is:
 * ```
 * ---
 * template: <templateName>
 * #customProperty: customValue
 * --- #namedSection
 * Section content
 * --- #namedSection2
 * More section content
 * ---
 */
fun extractPageModel(source: String): PageModel {

    val metadata = source.substringAfter("---").substringBefore("---").trim()

    val customSections =
        source.substringAfter("---").substringAfter("---").split("---").filter { it.isNotEmpty() }.map { it.trim() }
            .associate {
                val sectionName = if (it.startsWith("#")) {
                    it.substringAfter("#").substringBefore("\n")
                } else {
                    ""
                }
                val sectionContent = it.substringAfter("#$sectionName").substringBefore("---").trim()
                sectionName to sectionContent
            }.filter { it.key.isNotEmpty() }

    val customAttributes = metadata.lines().associate {
        val key = it.substringBefore(":").trim()
        val value = it.substringAfter(":").trim()
        key to value
    }.filter { it.key.startsWith("#") }
        .map { it.key.removePrefix("#") to it.value }

    val template = if (metadata.contains("template:")) {
        metadata.substringAfter("template:").substringBefore("\n").trim()
    } else {
        ""
    }

    return PageModel(template = template, attributes = customAttributes.toMap(), sections = customSections)
}
typealias MarkdownSection = String

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PageModel(
    val template: String,
    @EncodeDefault val lastModified: Instant = Clock.System.now(),
    val attributes: Map<String, String>,
    val sections: Map<String, MarkdownSection>
)