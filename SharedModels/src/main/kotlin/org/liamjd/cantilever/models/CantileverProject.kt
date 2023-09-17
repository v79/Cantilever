package org.liamjd.cantilever.models

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Project definition file. Will change a lot.
 * Should be stored in a file called 'cantilever.yaml'
 */
@Serializable
data class CantileverProject @OptIn(ExperimentalSerializationApi::class) constructor(
    val projectName: String,
    val author: String,
    val dateFormat: String = "dd/MM/yyyy",
    val dateTimeFormat: String = "HH:mm dd/MM/yyyy",
    val imageResolutions: Map<String, ImgRes>,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    var attributes: Map<String,String>? = null
)

/**
 * Represents an image resolution in pixels.
 * If one of the dimensions is null, then aspect ratio should be maintained.
 * If both are null, it's a bit broken
 */
@Serializable(with = ImgResSerializer::class)
data class ImgRes(val x: Int?, val y: Int?)

object ImgResSerializer : KSerializer<ImgRes> {
    override val descriptor = PrimitiveSerialDescriptor("ImgRes", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ImgRes) {
        val xString = value.x ?: ""
        val yString = value.y ?: ""
        val string = "${xString}x${yString}"
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): ImgRes {
        val string = decoder.decodeString()
        val x: Int? = string.substringBefore('x').toIntOrNull()
        val y: Int? = string.substringAfter('x').toIntOrNull()
        return ImgRes(x, y)
    }
}
