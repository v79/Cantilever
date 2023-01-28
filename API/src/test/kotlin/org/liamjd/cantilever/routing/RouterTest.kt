package org.liamjd.cantilever.routing

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.liamjd.cantilever.api.models.RawJsonString

class RouterTest {

    private val acceptJson = mapOf("accept" to "application/json")

    @Test
    fun `can get a basic route`() {
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/").withHttpMethod("GET").withHeaders(acceptJson)
        val response = testR.handleRequest(event)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `returns 404 when no matching route found`() {
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/this-route-doesnt-exist").withHttpMethod("GET")
            .withHeaders(emptyMap())
        val response = testR.handleRequest(event)

        assertNotNull(response)
        assertEquals(404, response.statusCode)
    }

    @Test
    fun `matches route when produces is overridden with html`() {
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/returnHtml").withHttpMethod("GET")
            .withHeaders(mapOf("accept" to "text/html"))
        val response = testR.handleRequest(event)

        assertEquals(200, response.statusCode)
    }

    @Test
    fun `matches POST route when consumes is overridden with img jpg`() {
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/postImage").withHttpMethod("POST").withHeaders(acceptJson)
            .withHeaders(mapOf("Content-Type" to "img/jpeg"))
        val response = testR.handleRequest(event)

        assertEquals(200, response.statusCode)
    }

    @Test
    fun `matches when many mime types are supplied in the accepts header`() {
//        text/plain,application/json,text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/").withHttpMethod("GET")
            .withHeaders(mapOf("accept" to "text/plain,application/json,text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"))
        val response = testR.handleRequest(event)

        assertEquals(200, response.statusCode)

    }

    @Test
    fun `responds with a serialized version of SimpleClass`() {
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/getSimple").withHttpMethod("GET").withHeaders(acceptJson)
        val response = testR.handleRequest(event)

        assertEquals(200, response.statusCode)
        assertNotNull(response.body)

        response.body.let {
            println(it)
            assertTrue(it.startsWith("{"))
            assertTrue(it.endsWith("}"))
        }
    }

    @Test
    fun `request response function can be defined in another class`() {
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/controller").withHttpMethod("GET").withHeaders(acceptJson)
        val response = testR.handleRequest(event)

        assertEquals(200, response.statusCode)
        assertNotNull(response.body)

        response.body.let {
            println(it)
            assertTrue(it.startsWith("{"))
            assertTrue(it.endsWith("}"))
        }
    }

    @Test
    fun `can serialize a error sealed class with a message`() {
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/sealedNo").withHttpMethod("GET").withHeaders(acceptJson)
        val response = testR.handleRequest(event)

        assertEquals(200, response.statusCode)
        assertNotNull(response.body)
        response.body.let {
            println(it)
            assertTrue(it.startsWith("{"))
            assertTrue(it.endsWith("}"))
        }
    }

    @Test
    fun `can serialize a sealed class containing a generic`() {
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/sealedYes").withHttpMethod("GET").withHeaders(acceptJson)
        val response = testR.handleRequest(event)

        assertEquals(200, response.statusCode)
        assertNotNull(response.body)
        response.body.let {
            println(it)
            assertTrue(it.startsWith("{"))
            assertTrue(it.endsWith("}"))
        }
    }

    @Test
    fun `router correctly serializes an API Response containing a raw json part`() {
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/getJsonString").withHttpMethod("GET").withHeaders(acceptJson)
        val response = testR.handleRequest(event)

        assertEquals(200, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body.contains(""""colour""""))
        assertTrue(response.body.contains(""""red""""))
    }
}

class TestRouter : RequestHandlerWrapper() {

    private val testController = TestController()
    override val router: Router = Router.router {
        // supplies JSON by default. Expects nothing.
        get("/") { _: Request<Unit> -> ResponseEntity(statusCode = 200, body = null) }
        get("/returnHtml") { _: Request<Unit> -> ResponseEntity.ok("<html></html>") }.supplies(setOf(MimeType.html))
            .expects(
                emptySet()
            )
        post("/postImage") { _: Request<Unit> -> ResponseEntity.ok("image bytes here") }.expects(setOf(MimeType.parse("img/jpeg")))
            .supplies(
                emptySet()
            )
        get("/getSimple") { _: Request<Unit> -> ResponseEntity.ok(body = SimpleClass("hello from simpleClass")) }
        get("/controller", testController::doSomething)
        get("/sealedNo") { _: Request<Unit> -> ResponseEntity.ok(ServiceResult.Error(exceptionMessage = "No here")) }
        get("/sealedYes") { _: Request<Unit> -> ResponseEntity.ok(ServiceResult.Success(data = SimpleClass("Ok from SimpleClass"))) }
        get("/getJsonString", testController::returnJsonString)
    }
}

class TestController {

    fun doSomething(request: Request<Unit>): ResponseEntity<SimpleClass> {
        println("TestController doSomething()")
        return ResponseEntity.ok(body = SimpleClass(message = "TestController has done stuff"))
    }

    fun returnJsonString(request: Request<Unit>): ResponseEntity<RawJsonString> {
        println("TestController returnJsonString")
        return ResponseEntity.ok(body = RawJsonString("""{ "colour": "red", "age": 23}"""))
    }
}

@Serializable
data class SimpleClass(val message: String)

@Serializable
data class ClassContainingRawJson(val name: String, val raw: RawJsonString)

@Serializable(with = ServiceResultSerializer::class)
sealed class ServiceResult<out T : Any> {
    @Serializable
    data class Success<out T : Any>(val data: T) : ServiceResult<T>()
    @Serializable
    data class Error(val exceptionMessage: String?) : ServiceResult<Nothing>()
}


@OptIn(ExperimentalSerializationApi::class)
class ServiceResultSerializer<T : Any>(
    tSerializer: KSerializer<T>
) : KSerializer<ServiceResult<T>> {
    @Serializable
    @SerialName("ServiceResult")

    data class ServiceResultSurrogate<T : Any>  constructor(
        val type: Type,
        // The annotation is not necessary, but it avoids serializing "data = null"
        // for "Error" results.
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val data: T? = null,
        @EncodeDefault(EncodeDefault.Mode.NEVER)
        val exceptionMessage: String? = null
    ) {
        enum class Type { SUCCESS, ERROR }
    }

    private val surrogateSerializer = ServiceResultSurrogate.serializer(tSerializer)

    override val descriptor: SerialDescriptor = surrogateSerializer.descriptor

    override fun deserialize(decoder: Decoder): ServiceResult<T> {
        val surrogate = surrogateSerializer.deserialize(decoder)
        return when (surrogate.type) {
            ServiceResultSurrogate.Type.SUCCESS ->
                if (surrogate.data != null)
                    ServiceResult.Success(surrogate.data)
                else
                    throw SerializationException("Missing data for successful result")

            ServiceResultSurrogate.Type.ERROR ->
                ServiceResult.Error(surrogate.exceptionMessage)
        }
    }

    override fun serialize(encoder: Encoder, value: ServiceResult<T>) {
        val surrogate = when (value) {
            is ServiceResult.Error -> ServiceResultSurrogate(
                ServiceResultSurrogate.Type.ERROR,
                exceptionMessage = value.exceptionMessage
            )

            is ServiceResult.Success -> ServiceResultSurrogate(
                ServiceResultSurrogate.Type.SUCCESS,
                data = value.data
            )
        }
        surrogateSerializer.serialize(encoder, surrogate)
    }
}