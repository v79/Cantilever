package org.liamjd.cantilever.models

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.liamjd.cantilever.openapi.APISchema

/**
 * Project definition file. *Will change a lot*. Should be stored in a file called 'cantilever.yaml'. Although Cantilever does not yet support multiple projects, this metadata object is useful for page rendering.
 *
 * @property projectName user-provided project name. Required.
 * @property author user-provided project author. Required.
 * @property dateFormat the default format for dates in rendered output; can be overridden with the 'localDate' handlebars helper
 * @property dateTimeFormat the default format for rendering dates and times. Not currently used.
 * @property imageResolutions a map of image resolutions; if empty uploaded images will not be scaled. Otherwise, images will be scaled and named according to this map. Written in the format "name: 320x260". Eg. if the map contains the key "square" and an [ImgRes] of x=320,y=320, then when an image is uploaded a scaled copy will be created with name '<original-name>-square.jpg`.
 * @property attributes a map of additional custom values which will be passed when rendering pages, posts etc.
 * @property domain the domain name of the site. Required.
 */
@APISchema
@Serializable
data class CantileverProject @OptIn(ExperimentalSerializationApi::class) constructor(
    val projectName: String,
    val author: String,
    val dateFormat: String = "dd/MM/yyyy",
    val dateTimeFormat: String = "HH:mm dd/MM/yyyy",
    val imageResolutions: Map<String, ImgRes> = emptyMap(),
    val domain: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    var attributes: Map<String,String>? = null
) {
    // Generation domain needs to end in /
    @Transient
    val domainKey: String = if(domain.endsWith("/")) domain else "$domain/"
}

/**
 * Represents an image resolution in pixels.
 * If one of the dimensions is null, then aspect ratio should be maintained.
 * If both are null, it's a bit broken. *Don't do this* but I can't prevent you.
 * @property w the width of the resolution, in pixels. If null, the width will not be changed.
 * @property h the height of the resolution, in pixels. If null, the height will not be changed.
 */
@Serializable(with = ImgResSerializer::class)
data class ImgRes(val w: Int?, val h: Int?)

/**
 * Converts the width and height values into a string of the format "WxH" (where that middle 'x' would be read as 'by').
 */
object ImgResSerializer : KSerializer<ImgRes> {
    override val descriptor = PrimitiveSerialDescriptor("ImgRes", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ImgRes) {
        val wString = value.w ?: ""
        val hString = value.h ?: ""
        val string = "${wString}x${hString}"
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): ImgRes {
        val string = decoder.decodeString()
        val w: Int? = string.substringBefore('x').toIntOrNull()
        val h: Int? = string.substringAfter('x').toIntOrNull()
        return ImgRes(w, h)
    }
}
