package org.liamjd.cantilever.routing

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.liamjd.cantilever.api.models.RawJsonString
import org.liamjd.cantilever.auth.AuthResult
import org.liamjd.cantilever.auth.Authorizer
import java.net.URLEncoder
import java.nio.charset.Charset

class RouterTest {

    private val acceptJson = mapOf("accept" to "application/json")
    private val acceptText = mapOf("accept" to "text/plain")
    private val acceptYaml = mapOf("accept" to "application/yaml")
    private val acceptHtml = mapOf("accept" to "text/html")
    private val contentJson = mapOf("Content-Type" to "application/json")
    private val contentText = mapOf("Content-Type" to "text/plain")
    private val xContentZero = mapOf("X-Content-Length" to "0")

    @Test
    fun `can get a basic route`() {
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/").withHttpMethod("GET").withHeaders(acceptJson)
        val response = testR.handleRequest(event)
        println(testR.router.listRoutes())
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
    fun `defaults to accepting and consuming application_json content when not specified`() {
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/postSimple").withHttpMethod("POST")
            .withBody(Json.encodeToString(SimpleClass("This is a simple class"))).withHeaders(contentJson + acceptJson)
        val response = testR.handleRequest(event)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `can override the default content type`() {
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/postText").withHttpMethod("POST")
            .withBody("This is a simple class").withHeaders(contentText + acceptJson)
        val response = testR.handleRequest(event)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `matches route when produces is overridden with html`() {
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/returnHtml").withHttpMethod("GET")
            .withHeaders(acceptHtml)
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
    fun `does not match when accept header contains wrong values`() {
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/").withHttpMethod("GET")
            .withHeaders(mapOf("accept" to "image/webp"))
        val response = testR.handleRequest(event)

        assertEquals(404, response.statusCode)
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
        val event =
            APIGatewayProxyRequestEvent().withPath("/getJsonString").withHttpMethod("GET").withHeaders(acceptJson)
        val response = testR.handleRequest(event)

        assertEquals(200, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body.contains(""""colour""""))
        assertTrue(response.body.contains(""""red""""))
    }

    // not implemented yet
    fun `requires auth header matching permission for authorize routes`() {
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/requiresTestPermission").withHttpMethod("GET")
            .withHeaders(acceptJson)
        val response = testR.handleRequest(event)

        testR.router.listRoutes()

        assertEquals(200, response.statusCode)
        assertNotNull(response.body)
        assertEquals("\"Permission granted\"", response.body)
    }

    @Test
    fun `can correctly match a nested route`() {
        val testR = TestRouter()
        val event =
            APIGatewayProxyRequestEvent().withPath("/group/route").withHttpMethod("GET").withHeaders(acceptJson)
        val response = testR.handleRequest(event)
        testR.router.listRoutes()
        assertEquals(200, response.statusCode)
        assertNotNull(response.body)
        assertEquals("\"Matching the nested route /route/\"", response.body)
    }

    @Test
    fun `can correctly match a deeply nested route`() {
        val testR = TestRouter()
        val event =
            APIGatewayProxyRequestEvent().withPath("/group/nested/wow").withHttpMethod("GET").withHeaders(acceptJson)
        val response = testR.handleRequest(event)

        assertEquals(200, response.statusCode)
        assertNotNull(response.body)
        assertEquals("\"This is deeply nested route /group/nested/wow\"", response.body)
    }

    @Test
    fun `secure route fails with 401 when but no credentials supplied`() {
        val testR = TestRouter()
        val event =
            APIGatewayProxyRequestEvent().withPath("/auth/hello").withHttpMethod("GET").withHeaders(acceptJson)
        val response = testR.handleRequest(event)

        assertEquals(401, response.statusCode)
    }

    @Test
    fun `secure route succeeds when no credentials supplied`() {
        val testR = TestRouter()
        val event =
            APIGatewayProxyRequestEvent().withPath("/auth/hello").withHttpMethod("GET")
                .withHeaders(mapOf("Authorization" to "Bearer 123123123") + acceptJson)
        val response = testR.handleRequest(event)

        assertEquals(200, response.statusCode)
    }

    @Test
    fun `can match a route with a path parameter and extract its value`() {
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/getParam/special").withHttpMethod("GET")
            .withHeaders(acceptText)
        val response = testR.handleRequest(event)

        assertEquals(200, response.statusCode)
        assertEquals("SPECIAL", response.body)
    }

    @Test
    fun `can match a route with a path parameter containing forward slashes that has been encoded`() {
        val testR = TestRouter()
        val encodedPath = URLEncoder.encode("special/path", Charset.defaultCharset())
        val event = APIGatewayProxyRequestEvent().withPath("/getParam/$encodedPath").withHttpMethod("GET")
            .withHeaders(acceptText)
        val response = testR.handleRequest(event)

        assertEquals(200, response.statusCode)
        assertEquals("SPECIAL%2FPATH", response.body)
    }

    @Test
    fun `can match a route with a multiple path parameters`() {
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/customer/xy123/purchaseOrder/2523").withHttpMethod("GET")
            .withHeaders(acceptText)
        val response = testR.handleRequest(event)

        assertEquals(200, response.statusCode)
        assertEquals("Customer 'xy123' made order #2523", response.body)
    }

    @Test
    fun `can match a nested path parameter route`() {
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/posts/load/123").withHttpMethod("GET")
            .withHeaders(acceptJson)
        val response = testR.handleRequest(event)

        assertEquals(200, response.statusCode)
        assertEquals(""""Request for /posts/load/123 received"""", response.body)
    }

    @Test
    fun `match a post route and deserialize a valid object`() {
        val testR = TestRouter()
        val postThing = PostThis(name = "Grapefruit", count = 23)
        val event = APIGatewayProxyRequestEvent().withPath("/postThing").withHttpMethod("POST")
            .withBody(Json.encodeToString(postThing))
            .withHeaders(acceptText + contentJson)
        val response = testR.handleRequest(event)

        assertEquals(200, response.statusCode)
        assertTrue(response.body.contains("Grapefruit"))
        assertTrue(response.body.contains("23"))
    }

    @Test
    fun `bad request when supplied body does not contain all required fields`() {
        val testR = TestRouter()
        val postThing = """{ "name": "Grapefruit" }"""
        val event = APIGatewayProxyRequestEvent().withPath("/postThing").withHttpMethod("POST").withBody(postThing)
            .withHeaders(acceptText + contentJson)
        val response = testR.handleRequest(event)

        assertEquals(400, response.statusCode)
        assertFalse(response.body.contains("Grapefruit"))
        assertTrue(response.body.startsWith("Invalid request."))
    }

    @Test
    fun `bad request when no body supplied but was expected for POST`() {
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/postThing").withHttpMethod("POST")
            .withHeaders(acceptText + contentJson)
        val response = testR.handleRequest(event)

        assertEquals(400, response.statusCode)
        assertEquals(
            "No body received but org.liamjd.cantilever.routing.PostThis was expected. If there is legitimately no body, add a X-Content-Length header with value '0'.",
            response.body
        )
    }

    @Test
    fun `throw appropriate error when entirely wrong object type is supplied`() {
        val testR = TestRouter()
        val postThing = DontPostThis(year = 1923L, truth = false)
        val event = APIGatewayProxyRequestEvent().withPath("/postThing").withHttpMethod("POST")
            .withBody(Json.encodeToString(postThing))
            .withHeaders(acceptText + contentJson)
        val response = testR.handleRequest(event)

        assertEquals(400, response.statusCode)
        assertTrue(response.body.startsWith("Could not deserialize body."))
    }

    @Test
    fun `can match a route with an asterix path parameter`() {
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/posts/save/*").withHttpMethod("PUT")
            .withHeaders(acceptJson + contentJson + xContentZero)
        val response = testR.handleRequest(event)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `can match a route with multiple valid methods`() {
        val testR = TestRouter()
        val getEvent = APIGatewayProxyRequestEvent().withPath("/multiple").withHttpMethod("GET")
            .withHeaders(acceptJson)
        val postEvent = APIGatewayProxyRequestEvent().withPath("/multiple").withHttpMethod("POST")
            .withHeaders(acceptJson + xContentZero)
        val getResponse = testR.handleRequest(getEvent)
        assertEquals(200, getResponse.statusCode)
        val postResponse = testR.handleRequest(postEvent)
        assertEquals(200, postResponse.statusCode)
    }

    @Test
    fun `can return an OpenAPI 3_0_1 spec`() {
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/openAPI").withHttpMethod("GET")
            .withHeaders(acceptText)
        val response = testR.handleRequest(event)
        assertEquals(200, response.statusCode)
        println(response.body)
        assertTrue(response.body.contains("openapi: 3.0.3"))
        assertTrue(response.body.contains("paths:"))
        assertTrue(response.body.contains("tags:"))
    }

    @Test
    fun `can override response headers for a particular route`() {
        val testR = TestRouter()
        val event = APIGatewayProxyRequestEvent().withPath("/overrideHeaders").withHttpMethod("GET").withHeaders(acceptJson)
        val response = testR.handleRequest(event)
        assertEquals(200, response.statusCode)
        assertEquals("*", response.headers["Access-Control-Allow-Origin"])
    }
}

/**
 * This class contains all the test routes
 */
class TestRouter : RequestHandlerWrapper() {

    private val testController = TestController()
    override val router: Router = lambdaRouter {
        /**
         * Test basic routing, return types, calling external controllers and so on
         */
        get("/") { _: Request<Unit> -> ResponseEntity(statusCode = 200, body = null) }
        get("/returnHtml") { _: Request<Unit> -> ResponseEntity.ok("<html></html>") }.supplies(setOf(MimeType.html))
            .expects(
                emptySet()
            )

        get("/getSimple") { _: Request<Unit> -> ResponseEntity.ok(body = SimpleClass("hello from simpleClass")) }
        get("/controller", testController::doSomething)
        get("/sealedNo") { _: Request<Unit> -> ResponseEntity.ok(ServiceResult.Error(exceptionMessage = "No here")) }
        get("/sealedYes") { _: Request<Unit> -> ResponseEntity.ok(ServiceResult.Success(data = SimpleClass("Ok from SimpleClass"))) }
        get("/getJsonString", testController::returnJsonString)

        /**
         * Test grouping
         */
        group("/group") {
            post("/new") { req: Request<String> -> ResponseEntity.ok(body = "Created a new ${req.body}") }
            get("/route") { _: Request<Any> -> ResponseEntity.ok(body = "Matching the nested route /route/") }
            get("/route/{thingy}") { _: Request<Any> -> ResponseEntity.ok(body = "Matching the nested route /route2/{thingy}") }
            group("/nested") {
                get("/wow") { _: Request<Any> -> ResponseEntity.ok(body = "This is deeply nested route /group/nested/wow") }
            }
        }

        /**
         * Test authorization
         */
        auth(FakeAuthorizer) {
            get("/auth/hello") { _: Request<Unit> -> ResponseEntity.ok(body = SimpleClass("Authenticated route says hello")) }
        }

        /**
         * Test path parameter matching
         */
        get("/getParam/{key}") { request: Request<Unit> -> ResponseEntity.ok(body = request.pathParameters["key"]?.uppercase()) }.supplies(
            setOf(MimeType.parse("text/plain"))
        )
        get("/customer/{id}/purchaseOrder/{po}") { request: Request<Unit> ->
            ResponseEntity.ok(body = "Customer '${request.pathParameters["id"]}' made order #${request.pathParameters["po"]}")
        }.supplies(setOf(MimeType("text", "plain")))

        group("/posts") {
            get("/load/{key}") { request: Request<Unit> -> ResponseEntity.ok("Request for /posts/load/${request.pathParameters["key"]} received") }
            put("/save/{key}") { request: Request<Unit> -> ResponseEntity.ok("Request for /posts/save/${request.pathParameters["key"]} received") }
        }

        get("/openAPI") { _: Request<Unit> ->
            ResponseEntity.ok(body = this.openAPI())
        }.supplies(setOf(MimeType.plainText))

        /**
         * Post and serialization of input body
         */
        post("/postSimple") { request: Request<SimpleClass> ->
            val obj = request.body
            ResponseEntity.ok("Received post for object message=${obj.message}")
        }

        post("/postThing") { request: Request<PostThis> ->
            val obj = request.body
            ResponseEntity.ok("Received post for object name=${obj.name}, count=${obj.count}")
        }.supplies(
            setOf(
                MimeType.plainText
            )
        )

        post("/postText") { request: Request<String> ->
            val obj = request.body
            ResponseEntity.ok("Received post for object message=${obj}")
        }.expects(
            setOf(
                MimeType.plainText
            )
        )

        // multiple methods, same path
        get("/multiple") { _: Request<Unit> -> ResponseEntity.ok(body = "GET /multiple") }
        post("/multiple") { _: Request<Unit> -> ResponseEntity.ok(body = "POST /multiple") }.expects(emptySet())

        // overriding heaeders for a route
        get("/overrideHeaders") { _: Request<Unit> -> ResponseEntity.ok(body = "GET /overrideHeaders") }.addHeaders(
            mapOf(
                "Access-Control-Allow-Origin" to "*"
            )
        )

        // contains OpenAPI specifications
        group("/specGroup") {
        get("/spec", testController::hasSpecification, Spec.PathItem(summary = "This is a summary", description = "This is a description"))
    }}
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

    fun hasSpecification(request: Request<Unit>): ResponseEntity<SimpleClass> {
        println("TestController hasSpecification()")
        return ResponseEntity.ok(body = SimpleClass(message = "TestController has a specification"))
    }
}

/**
 * Test classes below to simplify testing without bringing in other dependencies
 */
@Serializable
data class SimpleClass(val message: String)

@Serializable(with = ServiceResultSerializer::class)
sealed class ServiceResult<out T : Any> {
    @Serializable
    data class Success<out T : Any>(val data: T) : ServiceResult<T>()

    @Serializable
    data class Error(val exceptionMessage: String?) : ServiceResult<Nothing>()
}

@Serializable
data class PostThis(val name: String, val count: Int)

@Serializable
data class DontPostThis(val year: Long, val truth: Boolean)

object FakeAuthorizer : Authorizer {
    override val simpleName: String
        get() = "Looks for an Authorize Header which starts with 'Bearer '"
    override val type: String
        get() = "http"

    override fun authorize(request: APIGatewayProxyRequestEvent): AuthResult {
        val authHead = request.getHeader("Authorization")
        if (authHead != null) {
            return AuthResult(authHead.startsWith("Bearer "), "AuthResultMessage")
        }
        return AuthResult(false, "Invalid")
    }
}


@OptIn(ExperimentalSerializationApi::class)
class ServiceResultSerializer<T : Any>(
    tSerializer: KSerializer<T>
) : KSerializer<ServiceResult<T>> {
    @Serializable
    @SerialName("ServiceResult")

    data class ServiceResultSurrogate<T : Any> constructor(
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