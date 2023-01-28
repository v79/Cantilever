import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

/**
 * Experimental function to store an existing json string as part of a wider serialized object
 * Requires kotlinx.serialization >= 1.5
 */
fun main() {
//    val packet = ContainsJsonString("name", InlineJsonString("""{"some":"json"}"""))
    val packet = InlineJsonString("""{"some":"json"}""")

    val encodedPacket = Json.encodeToString(packet)
    println("\t$encodedPacket")

//    require("""{"name":"name","jsonString":{"some":"json"}}""" == encodedPacket)
    require("""{"some":"json"}""" == encodedPacket)
}

@Serializable
data class ContainsJsonString constructor(
    val name: String,
    @Serializable(with = RawJsonStringSerializer::class)
    val jsonString: InlineJsonString
//    val jsonString: String,
//    val jsonString: RawJsonString, // doesn't work?
)

typealias RawJsonString = @Serializable(with = RawJsonStringSerializer::class) String

@JvmInline
@Serializable(with = RawJsonStringSerializer::class)
value class InlineJsonString(val content: String) : CharSequence by content

@OptIn(ExperimentalSerializationApi::class)
private object RawJsonStringSerializer : KSerializer<InlineJsonString> {

    override val descriptor =
        PrimitiveSerialDescriptor("my.project.RawJsonString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): InlineJsonString = InlineJsonString(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: InlineJsonString) {
        return when (encoder) {
            is JsonEncoder -> {
                println("raw encoding $value")
                encoder.encodeJsonElement(JsonUnquotedLiteral(value.content))
            }

            else -> {
                println("plain encoding $value")
                encoder.encodeString(value.content)
            }
        }
    }
}