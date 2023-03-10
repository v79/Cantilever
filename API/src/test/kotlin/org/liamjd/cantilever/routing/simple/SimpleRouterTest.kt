package org.liamjd.cantilever.routing.simple

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * This is a playground for testing a better router class
 */
class SimpleRouterTest {

    private val acceptJson = mapOf("accept" to "application/json")

    @Test
    fun `can match a simple get route`() {
        val router = simpleRouter {
            get("/") { req: SimpleRequest<Unit> ->
                SimpleResponse(200, "OK")
            }
            get("/hello") { req: SimpleRequest<Unit> ->
                SimpleResponse(200, "Hello")
            }
        }
        router.listRoutes()

        val event = APIGatewayProxyRequestEvent().withPath("/").withHttpMethod("GET").withHeaders(acceptJson)
        val response = router.handleRequest(event)
        assertEquals(200, response.statusCode)
        assertEquals("OK", response.body)
    }

    @Test
    fun `returns 404 when route not found`() {
        val router = simpleRouter {
            get("/") { req: SimpleRequest<Unit> ->
                SimpleResponse(200, "OK")
            }
            get("/hello") { req: SimpleRequest<Unit> ->
                SimpleResponse(200, "Hello")
            }
        }
        router.listRoutes()

        val event = APIGatewayProxyRequestEvent().withPath("/notFound").withHttpMethod("GET").withHeaders(acceptJson)
        val response = router.handleRequest(event)
        assertEquals(404, response.statusCode)
        assertEquals("Route not found", response.body)
    }

    @Test
    fun `can match a nested route`() {
        val router = simpleRouter {
            group("/users") {
                get("/bob") { request: SimpleRequest<Unit> -> SimpleResponse(200, "Bob") }
            }
        }
        router.listRoutes()
        val event = APIGatewayProxyRequestEvent().withPath("/users/bob").withHttpMethod("GET").withHeaders(acceptJson)
        val response = router.handleRequest(event)
        assertEquals(200, response.statusCode)
        assertEquals("Bob", response.body)
    }

    @Test
    fun `does not match a nested route with wrong path`() {
        val router = simpleRouter {
            group("/users") {
                get("/bob") { request: SimpleRequest<Unit> -> SimpleResponse(200, "Bob") }
            }
        }
        router.listRoutes()
        val event = APIGatewayProxyRequestEvent().withPath("/bob").withHttpMethod("GET").withHeaders(acceptJson)
        val response = router.handleRequest(event)
        assertEquals(404, response.statusCode)
        assertEquals("Route not found", response.body)
    }

    @Test
    fun `can wrap a route with an auth block but not do anything with it`() {
        val router = simpleRouter {
            auth("A_THING") {
                get("/secure") { _: SimpleRequest<Unit> -> SimpleResponse(200, "Nothing") }
            }
        }
        val event = APIGatewayProxyRequestEvent().withPath("/secure").withHttpMethod("GET").withHeaders(acceptJson)
        val response = router.handleRequest(event)
        assertEquals(200, response.statusCode)
        assertEquals("Nothing", response.body)
    }

    @Test
    fun `can match a route with a path parameter and extract its value`() {
        val router = simpleRouter {
            get("/get/{key}") { req: SimpleRequest<Unit> ->
                val keyVal = req.pathParameters["key"]
                SimpleResponse(200, body = keyVal?.uppercase())
            }
        }
        val event = APIGatewayProxyRequestEvent().withPath("/get/special").withHttpMethod("GET")
            .withHeaders(mapOf("accept" to "text/plain"))
        val response = router.handleRequest(event)
        assertEquals(200, response.statusCode)
        assertEquals("SPECIAL", response.body)
    }

    @Test
    fun `can deserialize a body on a post event`() {
        val router = simpleRouter {
            post("/add/person") { request: SimpleRequest<Person> ->
                val person = request.body
                SimpleResponse(200, body = "Created person $person")
            }
        }
        val newPerson = Person("Bob", 43)
        val event = APIGatewayProxyRequestEvent().withPath("/add/person").withBody(Json.encodeToString(newPerson))
            .withHttpMethod("POST").withHeaders(mapOf("accept" to "text/plain"))
        val response = router.handleRequest(event)
        assertEquals(200, response.statusCode)
        println("Response is: $response")
        assertEquals("Created person Person(name=Bob, age=43)", response.body)
    }

    @Test
    fun `returns an error if it cannot deserialize an invalid body string`() {
        val router = simpleRouter {
            post("/add/person") { request: SimpleRequest<Person> ->
                val person = request.body
                SimpleResponse(200, body = "Created person $person")
            }
        }
        val newPerson = Person("Bob", 43)
        val invalidPersonJson = """{"name":"Bob"}"""
        val event = APIGatewayProxyRequestEvent().withPath("/add/person").withBody(invalidPersonJson)
            .withHttpMethod("POST").withHeaders(mapOf("accept" to "text/plain"))
        val response = router.handleRequest(event)
        assertEquals(400, response.statusCode)
        println("Response is: $response")
    }

}

@Serializable
data class Person(val name: String, val age: Int)
