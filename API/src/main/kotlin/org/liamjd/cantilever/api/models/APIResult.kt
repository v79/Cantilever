package org.liamjd.cantilever.api.models

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.liamjd.cantilever.api.models.ResultSerializer.APIResultSurrogate

@JvmInline
@Serializable(with = RawJsonStringSerializer::class)
value class RawJsonString(val content: String) : CharSequence by content

/**
 * An APIResult may be either:
 *
 * - OK or Error, which have a simple message string property
 * - Success, which returns an object of type 'R'
 * - JsonSuccess, which returns a complete Json string
 */
@Serializable(with = ResultSerializer::class)
sealed interface APIResult<out R : Any> {
    @Serializable
    data class Success<out R : Any>(val value: R) : APIResult<R>

    @Serializable
    @JvmInline
    value class JsonSuccess(val jsonString: RawJsonString) : APIResult<RawJsonString>

    // FIXME: none of these should have a message or status text, sadly.
    @Serializable
    data class Error(val statusText: String) : APIResult<Nothing>

    // FIXME: OK should not have a message, see https://github.com/v79/Cantilever/issues/75
    @Serializable
    data class OK(val message: String) : APIResult<Nothing>
}

/**
 * A custom serializer for the [APIResult] class
 * It uses an internal "surrogate" class [APIResultSurrogate] to handle the different possible types of [APIResult],
 * necessary because of the generics used in the [APIResult.Success] class.
 */
@OptIn(ExperimentalSerializationApi::class)
class ResultSerializer<R : Any>(rSerializer: KSerializer<R>) : KSerializer<APIResult<R>> {

    @Serializable
    @SerialName("result")
    data class APIResultSurrogate<R : Any> constructor(
        @Transient
        val type: ResultType = ResultType.OK,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val data: R? = null,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val message: String? = null,
        @Serializable(with = RawJsonStringSerializer::class)
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val jsonString: RawJsonString = RawJsonString("")
    ) {
        enum class ResultType { Success, Error, OK, JSON }
    }

    private val surrogateSerializer = APIResultSurrogate.serializer(rSerializer)
    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun deserialize(decoder: Decoder): APIResult<R> {
        val surrogate = surrogateSerializer.deserialize(decoder)
        println("Deserializing via surrogate for ${surrogate.type}")
        return when (surrogate.type) {
            APIResultSurrogate.ResultType.Success -> {
                if (surrogate.data != null) {
                    APIResult.Success(surrogate.data)
                } else {
                    throw SerializationException("Unable to serialize object with null data for Result.Success")
                }
            }
            APIResultSurrogate.ResultType.JSON -> {
               TODO("NOT YET IMPLEMENTED DESERIALIZATION OF ARBITRARY JSON BODY AND I DON'T NEED TO ANYWAY")
            }
            APIResultSurrogate.ResultType.OK -> {
                APIResult.OK(surrogate.message ?: "")
            }
            APIResultSurrogate.ResultType.Error -> {
                APIResult.Error(surrogate.message ?: "")
            }
        }
    }

    override fun serialize(encoder: Encoder, value: APIResult<R>) {
        val surrogate = when (value) {
            is APIResult.Error -> APIResultSurrogate(type = APIResultSurrogate.ResultType.Error, message = value.statusText)
            is APIResult.OK -> APIResultSurrogate(type = APIResultSurrogate.ResultType.OK, message = value.message)
            is APIResult.Success -> APIResultSurrogate(type = APIResultSurrogate.ResultType.Success, data = value.value)
            is APIResult.JsonSuccess -> {
                APIResultSurrogate(type = APIResultSurrogate.ResultType.JSON, jsonString = value.jsonString)
            }
        }
        surrogateSerializer.serialize(encoder, surrogate)
    }
}

@OptIn(ExperimentalSerializationApi::class)
object RawJsonStringSerializer : KSerializer<RawJsonString> {
    override val descriptor = PrimitiveSerialDescriptor("org.liamjd.cantilever.api.models.RawJsonString", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): RawJsonString = RawJsonString(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: RawJsonString) {
        return when (encoder) {
            is JsonEncoder -> {
                println("RawJsonStringSerializer: raw encoding")
                encoder.encodeJsonElement(JsonUnquotedLiteral(value.content))
            }
            else -> {
                println("RawJsonStringSerializer: plain encoding $value")
                encoder.encodeString(value.content)
            }
        }
    }
}