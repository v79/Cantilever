package org.liamjd.cantilever.services.impl

import org.liamjd.cantilever.common.*
import org.liamjd.cantilever.models.sqs.SqsMsgBody


/**
 * A page model is different from a Post, in that it can have multiple named markdown sections.
 * @param key full S3 key for the object
 * @param source full contents of the file
 * It will start with basic metadata though, and the 'template' and 'title' properties are required.
 * Sections must be named, and the name must start with '#' // TODO the hash symbol is a comment in Yaml, so maybe change this to '@' or '$' or '§'
 * The basic format is:
 * ```
 * ---
 * title: <title>
 * template: <templateName>
 * #customProperty: customValue
 * --- #namedSection
 * Section content
 * --- #namedSection2
 * More section content
 * ---
 */
fun extractPageModel(key: String, source: String): SqsMsgBody.PageModelMsg {
    val metadata = source.getFrontMatter().trim()
    val sectionRegex = Regex("(\n-{3} #)")
    val customSections = if (sectionRegex.containsMatchIn(source)) {
        source.split(sectionRegex).drop(1).map { it.trim() }
            .associate {
                val sectionName = it.substringBefore("\n").trim()
                val sectionContent = it.substringAfter(sectionName).trim()
                sectionName to sectionContent
            }.filter { it.key.isNotEmpty() && !it.key.startsWith("=") }
    } else {
        emptyMap()
    }

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

    val pageFile = key.substringAfter(S3_KEY.pagesPrefix) // strip /sources/pages from the filename
    val url = if (metadata.contains("slug:")) {
        pageFile + metadata.substringAfter("slug:").substringBefore("\n").trim()
    } else {
        pageFile.substringBefore(".${FILE_TYPE.MD}")
    }

    val title = if (metadata.contains("title:")) {
        metadata.substringAfter("title:").substringBefore("\n").trim()
    } else {
        key
    }

    return SqsMsgBody.PageModelMsg(
        title = title,
        srcKey = key,
        templateKey = template,
        url = url,
        attributes = customAttributes.toMap(),
        sections = customSections
    )
}
