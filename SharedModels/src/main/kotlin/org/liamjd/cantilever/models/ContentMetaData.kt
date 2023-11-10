package org.liamjd.cantilever.models

import com.charleskorn.kaml.Yaml
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.*
import org.liamjd.cantilever.common.FILE_TYPE
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.common.now
import org.liamjd.cantilever.common.toSlug

sealed interface ContentMetaData {


    @OptIn(ExperimentalSerializationApi::class) // required for @EncodeDefault
    @Serializable
    class PostContentMeta(
        val title: String,
        @EncodeDefault
        val slug: String = title.toSlug(),
        val date: LocalDate,
        @EncodeDefault
        val lastModified: Instant = Clock.System.now(),
        val templateKey: String
    ) : ContentMetaData{

        fun toYamlString(): String {
            return Yaml.default.encodeToString(serializer(), this)
        }
    }
}

sealed interface ContentMetaDataBuilder {
    fun buildFromYamlString(yamlString: String): ContentMetaData

    object PostBuilder : ContentMetaDataBuilder {
        override fun buildFromYamlString(yamlString: String): ContentMetaData.PostContentMeta {
            return Yaml.default.decodeFromString(serializer(), yamlString)
        }
        fun buildWithoutYaml(filename: String): ContentMetaData.PostContentMeta {
            return ContentMetaData.PostContentMeta(
                title = filename.removeSuffix(FILE_TYPE.MD).removeSuffix("."),
                templateKey = S3_KEY.defaultPostTemplateKey,
                slug = filename.removeSuffix(FILE_TYPE.MD).removeSuffix(".").removePrefix(S3_KEY.sources).toSlug(),
                date = LocalDate.now(),
                lastModified = Clock.System.now()
            )
        }
    }
}