package org.liamjd.cantilever.api.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.liamjd.cantilever.routing.ResponseEntity

class APIResultTests {

    @Test
    fun `can serialize non-generic Result OK object`() {
        val ok = APIResult.OK(message = "OK message")

        val result = Json.encodeToString(ok)

        assertNotNull(result)
        println(result)
        assertTrue(result.startsWith("{"))
        assertTrue(result.endsWith("}"))
        assertTrue(result.contains("OK message"))
    }

    @Test
    fun `can serialize non-generic Result Error object`() {
        val error = APIResult.Error(message = "Error message")

        val result = Json.encodeToString(error)

        assertNotNull(result)
        println(result)
        assertTrue(result.startsWith("{"))
        assertTrue(result.endsWith("}"))
        assertTrue(result.contains("Error message"))
    }

    @Test
    fun `can serialize generic Result Success object`() {
        val bigObject = BigObject(count = 23, text = "This isn't so big")
        val success = APIResult.Success<BigObject>(value = bigObject)

        val result = Json.encodeToString(success)

        assertNotNull(result)
        println(result)
        assertTrue(result.startsWith("{"))
        assertTrue(result.endsWith("}"))
        assertTrue(result.contains("23"))
        assertTrue(result.contains("This isn't so big"))
    }

    @Test
    fun `can serialize really big generic Result Success object`() {
        val reallyBig = ReallyBigObject(longValue = 751L, isTrue = false, bigObject = BigObject(count = 23, text = "This isn't so big"))
        val success = APIResult.Success<ReallyBigObject>(value = reallyBig)

        val result = Json.encodeToString(success)

        assertNotNull(result)
        println(result)
        assertTrue(result.startsWith("{"))
        assertTrue(result.endsWith("}"))
        assertTrue(result.contains("23"))
        assertTrue(result.contains("751"))
        assertTrue(result.contains("This isn't so big"))
    }

    @Test
    fun `can deserialize really big object`() {
        val json = """{"value":{"longValue":751,"isTrue":false,"bigObject":{"count":23,"text":"This isn't so big"}}}"""
        val result = Json.decodeFromString<APIResult.Success<ReallyBigObject>>(json)

        assertNotNull(result)
        result.let { success ->
            success.value.let {
                assertEquals(751L, it.longValue)
                assertFalse(it.isTrue)
                assertNotNull(it.bigObject)
            }
        }
    }

    @Test
    fun `can serialise a response entity wrapping an APIResult`() {
        val ok = APIResult.OK(message = "OK message")
        val entity = ResponseEntity.ok(body = ok)

        val result = Json.encodeToString(entity)

        assertNotNull(result)
        println(result)
        result.let {
            it.startsWith("{")
            it.endsWith("}")
        }
    }

    @Test
    fun `can serialise a response entity wrapping an bigger APIResult`() {
        val bigObject = BigObject(count = 23, text = "This isn't so big")
        val success = APIResult.Success<BigObject>(value = bigObject)
        val entity = ResponseEntity.ok(body = success)

        val result = Json.encodeToString(entity)

        assertNotNull(result)
        println(result)
        result.let {
            it.startsWith("{")
            it.endsWith("}")
        }
    }

 }

@Serializable
data class BigObject(val count: Int, val text: String)

@Serializable
data class ReallyBigObject(val longValue: Long, val isTrue: Boolean, val bigObject: BigObject)