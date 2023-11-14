@file:Suppress("ConvertObjectToDataObject")

package org.liamjd.cantilever.models

import com.charleskorn.kaml.Yaml
import kotlinx.datetime.LocalDate
import org.liamjd.cantilever.common.FILE_TYPE
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.common.now
import org.liamjd.cantilever.common.toSlug

/**
 * Parses the YAML front matter of a markdown file and returns a [ContentNode] object
 */
sealed interface ContentMetaDataBuilder {
    fun buildFromSourceString(sourceString: String, srcKey: SrcKey): ContentNode

    object PostBuilder : ContentMetaDataBuilder {
        /**
         * We use the temporary [PostYaml] class to decode the YAML front matter, then build a [ContentNode.PostNode] from it
         */
        override fun buildFromSourceString(sourceString: String, srcKey: SrcKey): ContentNode.PostNode {
            val postYaml = Yaml.default.decodeFromString(PostYaml.serializer(), sourceString)
            return ContentNode.PostNode(postYaml).apply { this.srcKey = srcKey }
        }

        fun buildWithoutYaml(srcKey: SrcKey): ContentNode.PostNode {
            val post = ContentNode.PostNode(
                title = srcKey.removeSuffix(FILE_TYPE.MD).removeSuffix("."),
                templateKey = S3_KEY.defaultPostTemplateKey,
                slug = srcKey.removeSuffix(FILE_TYPE.MD).removeSuffix(".").removePrefix(S3_KEY.sources).toSlug(),
                date = LocalDate.now()
            )
            post.srcKey = srcKey
            return post
        }
    }

    object PageBuilder : ContentMetaDataBuilder {
        /**
         * I can't use the Yaml.decodeFromString() extension function here, as page metadata is not, technically, valid YAML
         * It's a series of key:value pairs, followed by named sections, each of which is a markdown block
         */
        override fun buildFromSourceString(sourceString: String, srcKey: SrcKey): ContentNode.PageNode {

            val sectionRegex = Regex("(\n-{3} #)")
            val customSections = if (sectionRegex.containsMatchIn(sourceString)) {
                sourceString.split(sectionRegex).drop(1).map { it.trim() }
                    .associate {
                        val sectionName = it.substringBefore("\n").trim()
                        val sectionContent = it.substringAfter(sectionName).trim()
                        sectionName to ""
                    }.filter { it.key.isNotEmpty() && !it.key.startsWith("=") }
            } else {
                emptyMap()
            }

            val customAttributes = sourceString.lines().associate {
                val key = it.substringBefore(":").trim()
                val value = it.substringAfter(":").trim()
                key to value
            }.filter { it.key.startsWith("#") }
                .map { it.key.removePrefix("#") to it.value }

            val template = if (sourceString.contains("templateKey:")) {
                sourceString.substringAfter("templateKey:").substringBefore("\n").trim()
            } else {
                ""
            }

            val pageFile = srcKey.substringAfter(S3_KEY.pagesPrefix) // strip /sources/pages from the filename
            val url = if (sourceString.contains("slug:")) {
                pageFile + sourceString.substringAfter("slug:").substringBefore("\n").trim()
            } else {
                pageFile.substringBefore(".${FILE_TYPE.MD}")
            }

            val title = if (sourceString.contains("title:")) {
                sourceString.substringAfter("title:").substringBefore("\n").trim()
            } else {
                srcKey
            }

            val isRoot = if (sourceString.contains("isRoot:")) {
                sourceString.substringAfter("isRoot:").substringBefore("\n").trim().toBoolean()
            } else {
                false
            }

            return ContentNode.PageNode(
                title = title,
                srcKey = srcKey,
                templateKey = template,
                isRoot = isRoot,
                slug = url,
                attributes = customAttributes.toMap(),
                sections = customSections
            )
        }
    }
}