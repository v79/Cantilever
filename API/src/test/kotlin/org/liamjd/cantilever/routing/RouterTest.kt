package org.liamjd.cantilever.routing

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
    fun `matches route when consumes is overridden with img jpg`() {
        val testR = TestRouter()

        val event = APIGatewayProxyRequestEvent().withPath("/postImage").withHttpMethod("GET").withHeaders(acceptJson)
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

}

class TestRouter : RequestHandlerWrapper() {

    override val router: Router = Router.router {
        // supplies JSON by default. Expects nothing.
        get("/") { _: Request<Unit> -> ResponseEntity.ok(null) }

        get("/returnHtml") { _: Request<Unit> -> ResponseEntity.ok("<html></html>") }.supplies(setOf(MimeType.html))
            .expects(
                emptySet()
            )

        // TODO this would normally be a POST
        get("/postImage") { _: Request<Unit> -> ResponseEntity.ok("image bytes here") }.expects(setOf(MimeType.parse("img/jpeg")))
            .supplies(
                emptySet()
            )
    }
}