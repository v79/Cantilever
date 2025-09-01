package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import io.mockk.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import org.koin.test.junit5.KoinTestExtension
import org.koin.test.junit5.mock.MockProviderExtension
import org.koin.test.mock.declareMock
import org.liamjd.cantilever.common.EnvironmentProvider
import org.liamjd.cantilever.models.CantileverProject
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.sqs.TemplateSQSMessage
import org.liamjd.cantilever.services.DynamoDBService
import org.liamjd.cantilever.services.S3Service
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PartialTemplateTest : KoinTest {
    private val mockS3: S3Service by inject()
    private val mockDynamo: DynamoDBService by inject()
    private val mockContext: Context = mockk(relaxed = true)
    private lateinit var env: EnvironmentProvider

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create { modules(module { }) }

    @JvmField
    @RegisterExtension
    val mockProvider = MockProviderExtension.create { clazz -> mockkClass(clazz) }

    @BeforeEach
    fun setup() {
        env = mockk(relaxed = true)
        every { env.getEnv("source_bucket") } returns "source-bkt"
        every { env.getEnv("generation_bucket") } returns "gen-bkt"
        every { env.getEnv("destination_bucket") } returns "dest-bkt"
        declareMock<S3Service> { }
        declareMock<DynamoDBService> { }
        // Safe defaults for NavigationBuilder calls
        coEvery { mockDynamo.listAllNodesForProject(any(), any()) } returns emptyList()
        coEvery { mockDynamo.getKeyListFromLSI(any(), any(), any(), any(), any(), any()) } returns emptyList()
        coEvery { mockDynamo.getFirstOrLastKeyFromLSI(any(), any(), any(), any()) } returns null
        coEvery { mockDynamo.getContentNode(any(), any(), any()) } returns null
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `can render a page where the template contains a partial`() {
        val project = CantileverProject(domain = "example.com", projectName = "Example", author = "Alice")
        coEvery { mockDynamo.getProject("example.com") } returns project

        val pageHbs = """
            ---
            name: page
            sections:
              - body
            ---
            <html><title>{{title}}</title><body>{{{body}}}</body>{{> include/footer }}</html>
        """.trimIndent()

        val footerHbs = """
            <footer>Footer</footer>
        """.trimIndent()

        every { mockS3.getObjectAsString("example.com/templates/page.hbs", "source-bkt") } returns pageHbs
        every { mockS3.getObjectAsString("example.com/gen/body.html", "gen-bkt") } returns "<p>Hello</p>"
        every {
            mockS3.getObjectAsString(
                "example.com/sources/templates/include/footer.hbs",
                "source-bkt"
            )
        } returns footerHbs

        val keySlot: CapturingSlot<String> = slot()
        val bucketSlot: CapturingSlot<String> = slot()
        val bodySlot: CapturingSlot<String> = slot()
        val typeSlot: CapturingSlot<String> = slot()
        every {
            mockS3.putObjectAsString(
                capture(keySlot),
                capture(bucketSlot),
                capture(bodySlot),
                capture(typeSlot)
            )
        } returns 123

        val pageMeta = ContentNode.PageNode(
            srcKey = "example.com/sources/pages/index.md",
            title = "Home",
            templateKey = "templates/page.hbs",
            slug = "index",
            isRoot = true,
            attributes = emptyMap(),
            sections = mapOf("body" to "example.com/gen/body.html"),
            parent = "example.com/sources/pages"
        )
        val msg = TemplateSQSMessage.RenderPageMsg(
            projectDomain = "example.com",
            fragmentSrcKey = "example.com/generated/htmlFragments/pages/index.body.html",
            metadata = pageMeta
        )

        val sqsEvent = SQSEvent().apply {
            records = listOf(SQSEvent.SQSMessage().apply { body = Json.encodeToString<TemplateSQSMessage>(msg) })
        }

        val handler = TemplateProcessorHandler(env)
        val result = handler.handleRequest(sqsEvent, mockContext)

        assertEquals("200 OK", result)
        verify(exactly = 1) { mockS3.putObjectAsString("example.com/index.html", "dest-bkt", any(), "text/html") }
        assertTrue(bodySlot.captured.contains("<p>Hello</p>"))
        assertTrue(bodySlot.captured.contains("<title>Home</title>"))
        assertTrue(bodySlot.captured.contains("<footer>Footer</footer>"))
    }
}