package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.LambdaLogger
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlin.test.*

internal class HandlebarsRendererTest {

    private val mockLogger = mockk<LambdaLogger>()

    @BeforeTest
    fun initTests() {
        every { mockLogger.log(any<String>()) } just runs
    }

    @Test
    fun `render a very simple template and model`() {
        // setup
        val templateString = "<html><title>{{title}}</title></html>"
        val title = "Handlebars"
        val expectedResult = "<html><title>Handlebars</title></html>"
        val model = mapOf<String, Any?>("title" to title)

        with(mockLogger) {
            val renderer = HandlebarsRenderer()

            // execute
            val result = renderer.render(model, templateString)

            // verify
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun `render does not escape HTML in a triple block`() {
        // setup
        val templateString = "<html><title>Test</title><body>{{{body}}}</html>"
        val body = "<p>Handlebars</p>"
        val expectedResult = "<html><title>Test</title><body><p>Handlebars</p></html>"
        val model = mapOf<String, Any?>("body" to body)

        with(mockLogger) {
            val renderer = HandlebarsRenderer()

            // execute
            val result = renderer.render(model, templateString)

            // verify
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun `take helper takes first of a two-element list`() {
        // setup
        val templateString = """<ul>{{#take posts "1" }}<li>{{ this.title }}</li>{{/take}}</ul>"""
        val posts = listOf(TestPost("First"), TestPost("Second"))
        val model = mapOf<String, Any?>("posts" to posts)

        with(mockLogger) {
            val renderer = HandlebarsRenderer()

            val result = renderer.render(model, templateString)
            println(result)
            assertTrue(result.contains("First"))
            assertFalse(result.contains("Second"))
        }
    }

    @Test
    fun `does not mangle emoji in model`() {
        // setup
        val templateString = "<html><title>{{title}}</title></html>"
        val title = "▶️🐈"
        val expectedResult = "<html><title>▶️🐈</title></html>"
        val model = mapOf<String, Any?>("title" to title)
        println("model=$model")

        with(mockLogger) {
            val renderer = HandlebarsRenderer()

            // execute
            val result = renderer.render(model, templateString)

            // verify
            assertEquals(expectedResult, result)
        }
    }
}

class TestPost(val title: String)