package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkClass
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.koin.test.junit5.KoinTestExtension
import org.koin.test.junit5.mock.MockProviderExtension
import org.koin.test.mock.declareMock
import org.liamjd.cantilever.common.EnvironmentProvider
import org.liamjd.cantilever.services.DynamoDBService
import org.liamjd.cantilever.services.S3Service
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * These tests all use the renderInline method to test the rendering of a template string with a model
 * @see HandlebarsRenderer.renderInline
 */
@ExtendWith(MockKExtension::class)
internal class HandlebarsRendererTest : KoinTest {

    private val mockS3Service: S3Service by inject()
    private val mockDynamoDBService: DynamoDBService by inject()
    private val mockContext: Context = mockk(relaxed = true)
    private val mockLogger = mockk<LambdaLogger>(relaxed = true)
    private val mockEnv = mockk<EnvironmentProvider>()

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(module {
        })
    }

    @JvmField
    @RegisterExtension
    val mockProvider = MockProviderExtension.create { clazz ->
        mockkClass(clazz)
    }

    @BeforeEach
    fun setup() {
        declareMock<Context> {
            every { mockContext.logger } returns mockLogger
        }
        declareMock<EnvironmentProvider> {

        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `render a very simple template and model`() {
        // setup
        val templateString = "<html><title>{{title}}</title></html>"
        val title = "Handlebars"
        val expectedResult = "<html><title>Handlebars</title></html>"
        val model = mapOf<String, Any?>("title" to title)

        val renderer = HandlebarsRenderer(mockS3Service, "srcBucket")

        // execute
        val result = renderer.renderInline(model, templateString)

        // verify
        assertEquals(expectedResult, result)
    }

    @Test
    fun `render does not escape HTML in a triple block`() {
        // setup
        val templateString = "<html><title>Test</title><body>{{{body}}}</html>"
        val body = "<p>Handlebars</p>"
        val expectedResult = "<html><title>Test</title><body><p>Handlebars</p></html>"
        val model = mapOf<String, Any?>("body" to body)

        val renderer = HandlebarsRenderer(mockS3Service, "srcBucket")

        // execute
        val result = renderer.renderInline(model, templateString)

        // verify
        assertEquals(expectedResult, result)
    }

    @Test
    fun `take helper takes first of a two-element list`() {
        // setup
        val templateString = """<ul>{{#take posts "1" }}<li>{{ this.title }}</li>{{/take}}</ul>"""
        val posts = listOf(TestPost("First"), TestPost("Second"))
        val model = mapOf<String, Any?>("posts" to posts)

        val renderer = HandlebarsRenderer(mockS3Service, "srcBucket")
        // execute
        val result = renderer.renderInline(model, templateString)

        //verify
        println(result)
        assertTrue(result.contains("First"))
        assertFalse(result.contains("Second"))
    }

    @Test
    fun `can render an object where the name starts with at`() {
        val templateString = "next: {{@next.title}}"
        val model = mapOf<String, TestLink?>("@next" to TestLink("NEXT"))
        val expectedResult = "next: NEXT"

        val renderer = HandlebarsRenderer(mockS3Service, "srcBucket")
        // execute
        val result = renderer.renderInline(model, templateString)
        // verify
        assertEquals(expectedResult, result)
    }

    @Test
    fun `localDate formatter works when project format supplied`() {
        // setup
        val templateString = """{{ localDate this.date this.dateFormat }}"""
        val expectedResult = "24/09/2023"
        val date = LocalDate(2023, 9, 24)
        val model = mapOf<String, Any?>("date" to date, "dateFormat" to "dd/MM/yyyy")

        val renderer = HandlebarsRenderer(mockS3Service, "srcBucket")
        // execute
        val result = renderer.renderInline(model, templateString)
        // verify
        assertEquals(expectedResult, result)
    }

    @Test
    fun `localDate formatter when custom format supplied`() {
        // setup
        val templateString = """{{ localDate this.date "HH:mm dd MMM yyyy" }}"""
        val expectedResult = "20:14 21 Oct 2023"
        val dateTime = LocalDateTime(year = 2023, month = Month.OCTOBER, dayOfMonth = 21, hour = 20, minute = 14)
        val model = mapOf<String, Any?>("date" to dateTime)

        val renderer = HandlebarsRenderer(mockS3Service, "srcBucket")
        // execute
        val result = renderer.renderInline(model, templateString)
        // verify
        assertEquals(expectedResult, result)
    }

    @Test
    fun `localDate formatter when ISO format supplied`() {
        // setup
        val templateString = """{{ localDate this.date "ISO_WEEK_DATE" }}"""
        val expectedResult = "2023-W42-6"
        val dateTime = LocalDateTime(year = 2023, month = Month.OCTOBER, dayOfMonth = 21, hour = 20, minute = 14)
        val model = mapOf<String, Any?>("date" to dateTime)

        val renderer = HandlebarsRenderer(mockS3Service, "srcBucket")
        // execute
        val result = renderer.renderInline(model, templateString)
        // verify
        assertEquals(expectedResult, result)
    }

    @Test
    fun `does not mangle emoji in model`() {
        // setup
        val templateString = "<html><title>{{title}}</title></html>"
        val title = "▶️🐈"
        val expectedResult = "<html><title>▶️🐈</title></html>"
        val model = mapOf<String, Any?>("title" to title)
        println("model=$model")

        val renderer = HandlebarsRenderer(mockS3Service, "srcBucket")

        // execute
        val result = renderer.renderInline(model, templateString)

        // verify
        assertEquals(expectedResult, result)
    }

    @Test
    fun `yaml stripping cache strips frontispiece from template string`() {
        // setup
        val templateString = """
            ---
            name: Wibble
            sections:
              - body
              - links
            ---
            <html><title>{{title}}</title></html>
            """.trimIndent()
        val title = "Handlebars"
        val expectedResult = "<html><title>Handlebars</title></html>"
        val model = mapOf<String, Any?>("title" to title)

        val renderer = HandlebarsRenderer(mockS3Service, "srcBucket")

        // execute
        val result = renderer.renderInline(model, templateString)

        // verify
        assertEquals(expectedResult, result)
    }
}

internal class TestPost(val title: String)
internal class TestLink(val title: String)