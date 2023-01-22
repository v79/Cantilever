package org.liamjd.cantilever.api.models

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ResultSerializer::class)
sealed class APIResult<out R : Any> {
    @Serializable
    data class Success<out R : Any>(val value: R) : APIResult<R>()

    @Serializable
    data class Error(val message: String) : APIResult<Nothing>()

    @Serializable
    data class OK(val message: String) : APIResult<Nothing>()
}

@OptIn(ExperimentalSerializationApi::class)
class ResultSerializer<R : Any>(rSerializer: KSerializer<R>) : KSerializer<APIResult<R>> {

    @Serializable
    @SerialName("result")
    data class APIResultSurrogate<R : Any> constructor(
        val type: ResultType,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val data: R? = null,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val message: String? = null
    ) {
        enum class ResultType { Success, Error, OK }
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
            is APIResult.Error -> APIResultSurrogate(type = APIResultSurrogate.ResultType.Error, message = value.message)
            is APIResult.OK -> APIResultSurrogate(type = APIResultSurrogate.ResultType.OK, message = value.message)
            is APIResult.Success -> APIResultSurrogate(type = APIResultSurrogate.ResultType.Success, data = value.value)
        }
        surrogateSerializer.serialize(encoder, surrogate)
    }
}