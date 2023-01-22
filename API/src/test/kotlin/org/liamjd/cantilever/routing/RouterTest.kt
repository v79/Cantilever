package org.liamjd.cantilever.routing

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

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

        }
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

        get("/getSimple") { _: Request<Unit> -> ResponseEntity.ok(body = SimpleClass("hello from simpleClass"))}

        get("/controller", testController::doSomething)
    }
}

class TestController {

    fun doSomething(request: Request<Unit>): ResponseEntity<SimpleClass> {
        println("TestController doSomething()")
       return ResponseEntity.ok(body = SimpleClass(message = "TestController has done stuff"))
    }
}

@Serializable
data class SimpleClass(val message: String)