@file:Suppress("ConvertObjectToDataObject")

package org.liamjd.cantilever.models

import com.charleskorn.kaml.Yaml
import kotlinx.datetime.LocalDate
import org.liamjd.cantilever.common.FILE_TYPE
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.common.now
import org.liamjd.cantilever.common.toSlug


sealed interface ContentMetaDataBuilder {
    fun buildFromYamlString(yamlString: String, srcKey: SrcKey): ContentNode

    object PostBuilder : ContentMetaDataBuilder {
        override fun buildFromYamlString(yamlString: String, srcKey: SrcKey): ContentNode.PostNode {
            val postYaml = Yaml.default.decodeFromString(PostYaml.serializer(), yamlString)
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
        override fun buildFromYamlString(yamlString: String, srcKey: SrcKey): ContentNode.PageNode {
            // I can't use the Yaml.decodeFromString() extension function here, as page metadata is not, technically, valid YAML

            val sectionRegex = Regex("(\n-{3} #)")
            val customSections = if (sectionRegex.containsMatchIn(yamlString)) {
                yamlString.split(sectionRegex).drop(1).map { it.trim() }
                    .associate {
                        val sectionName = it.substringBefore("\n").trim()
                        val sectionContent = it.substringAfter(sectionName).trim()
                        sectionName to sectionContent
                    }.filter { it.key.isNotEmpty() && !it.key.startsWith("=") }
            } else {
                emptyMap()
            }

            val customAttributes = yamlString.lines().associate {
                val key = it.substringBefore(":").trim()
                val value = it.substringAfter(":").trim()
                key to value
            }.filter { it.key.startsWith("#") }
                .map { it.key.removePrefix("#") to it.value }

            val template = if (yamlString.contains("templateKey:")) {
                yamlString.substringAfter("templateKey:").substringBefore("\n").trim()
            } else {
                ""
            }

            val pageFile = srcKey.substringAfter(S3_KEY.pagesPrefix) // strip /sources/pages from the filename
            val url = if (yamlString.contains("slug:")) {
                pageFile + yamlString.substringAfter("slug:").substringBefore("\n").trim()
            } else {
                pageFile.substringBefore(".${FILE_TYPE.MD}")
            }

            val title = if (yamlString.contains("title:")) {
                yamlString.substringAfter("title:").substringBefore("\n").trim()
            } else {
                srcKey
            }

            val isRoot = if (yamlString.contains("isRoot:")) {
                yamlString.substringAfter("isRoot:").substringBefore("\n").trim().toBoolean()
            } else {
                false
            }

            return ContentNode.PageNode(
                title = title,
                srcKey = srcKey,
                templateKey = template,
                isRoot = isRoot,
                url = url,
                attributes = customAttributes.toMap(),
                sections = customSections
            )
        }
    }
}