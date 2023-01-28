package org.liamjd.cantilever.routing

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RequestPredicateTest {

    val defaultAcceptHeader = mapOf<String, String>("accept" to "application/json")
    val jsonMimeType = MimeType("application", "json")

    @Test
    fun `matches GET root path with default headers`() {
        val req = APIGatewayProxyRequestEvent()
            .withPath("/")
            .withHttpMethod("GET")
            .withHeaders(defaultAcceptHeader)

        val predicate = RequestPredicate("get", "/", produces = setOf(jsonMimeType), consumes = emptySet())

        val result = predicate.match(req)
        assertNotNull(result)
        assertTrue(result.matches)
    }

    @Test
    fun `matches GET root path which produces html`() {
        val req = APIGatewayProxyRequestEvent()
            .withPath("/")
            .withHttpMethod("GET")
            .withHeaders(mapOf("accept" to "text/html"))
        val predicate = RequestPredicate("get","/", produces = setOf(MimeType("text","html")), consumes = emptySet())

        val result = predicate.match(req)
        assertNotNull(result)
        assertTrue(result.matches)
    }

    @Test
    fun `matches GET request even with no headers`() {
        val req = APIGatewayProxyRequestEvent()
            .withPath("/")
            .withHttpMethod("GET")

        val accepted = req.acceptedMediaTypes()
        println("accepted: $accepted")

        val predicate = RequestPredicate("get","/", produces = emptySet(), consumes = emptySet())
println(predicate)
        val result = predicate.match(req)
        println(result)
        assertNotNull(result)
        assertTrue(result.matches)
    }

    @Test
    fun `matches POST path accepting and producing json`() {
        val req = APIGatewayProxyRequestEvent()
            .withPath("/new")
            .withHttpMethod("POST")
            .withHeaders(mapOf("content-type" to jsonMimeType.toString(),"accept" to jsonMimeType.toString()))

        val predicate = RequestPredicate("post", "/new", produces = setOf(jsonMimeType), consumes = setOf(jsonMimeType))

        val result = predicate.match(req)
        assertNotNull(result)
        assertTrue(result.matches)
    }

    @Test
    fun `does not match when method is wrong`() {
        val req = APIGatewayProxyRequestEvent()
            .withPath("/")
            .withHttpMethod("POST")
            .withHeaders(defaultAcceptHeader)

        val predicate = RequestPredicate("get", "/", produces = setOf(jsonMimeType), consumes = emptySet())

        val result = predicate.match(req)
        assertNotNull(result)
        assertFalse(result.matches)
    }

}