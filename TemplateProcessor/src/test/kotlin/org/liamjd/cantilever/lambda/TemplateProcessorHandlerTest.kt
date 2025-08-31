package org.liamjd.cantilever.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import io.mockk.*
import kotlinx.datetime.LocalDate
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

class TemplateProcessorHandlerTest : KoinTest {

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
    fun tearDown() { stopKoin() }

    @Test
    fun `happy path - page render`() {
        val project = CantileverProject(domain = "example.com", projectName = "Example", author = "Alice")
        coEvery { mockDynamo.getProject("example.com") } returns project

        every { mockS3.getObjectAsString("example.com/templates/page.hbs", "source-bkt") } returns "<html><title>{{title}}</title><body>{{{body}}}</body></html>"
        every { mockS3.getObjectAsString("example.com/gen/body.html", "gen-bkt") } returns "<p>Hello</p>"

        val keySlot: CapturingSlot<String> = slot()
        val bucketSlot: CapturingSlot<String> = slot()
        val bodySlot: CapturingSlot<String> = slot()
        val typeSlot: CapturingSlot<String> = slot()
        every { mockS3.putObjectAsString(capture(keySlot), capture(bucketSlot), capture(bodySlot), capture(typeSlot)) } returns 123

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
    }

    @Test
    fun `render a page where the template contains yaml front matter`() {
        val project = CantileverProject(domain = "example.com", projectName = "Example", author = "Alice")
        coEvery { mockDynamo.getProject("example.com") } returns project

        every { mockS3.getObjectAsString("example.com/templates/page.hbs", "source-bkt") } returns "--- name: post --- <html><title>{{title}}</title><body>{{{body}}}</body></html>"
        every { mockS3.getObjectAsString("example.com/gen/body.html", "gen-bkt") } returns "<p>Hello</p>"

        val keySlot: CapturingSlot<String> = slot()
        val bucketSlot: CapturingSlot<String> = slot()
        val bodySlot: CapturingSlot<String> = slot()
        val typeSlot: CapturingSlot<String> = slot()
        every { mockS3.putObjectAsString(capture(keySlot), capture(bucketSlot), capture(bodySlot), capture(typeSlot)) } returns 123

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
    }

    @Test
    fun `happy path - post render`() {
        val project = CantileverProject(domain = "example.com", projectName = "Example", author = "Alice")
        coEvery { mockDynamo.getProject("example.com") } returns project

        every { mockS3.getObjectAsString("example.com/gen/post-body.html", "gen-bkt") } returns "<p>Post</p>"
        every { mockS3.getObjectAsString("example.com/templates/post.hbs", "source-bkt") } returns "<html><title>{{title}}</title><body>{{{body}}}</body></html>"

        val keySlot: CapturingSlot<String> = slot()
        val bucketSlot: CapturingSlot<String> = slot()
        val bodySlot: CapturingSlot<String> = slot()
        val typeSlot: CapturingSlot<String> = slot()
        every { mockS3.putObjectAsString(capture(keySlot), capture(bucketSlot), capture(bodySlot), capture(typeSlot)) } returns 456

        val postMeta = ContentNode.PostNode(
            srcKey = "example.com/sources/posts/hello.md",
            title = "Hello",
            templateKey = "templates/post.hbs",
            date = LocalDate(2024, 1, 2),
            slug = "hello"
        )
        val msg = TemplateSQSMessage.RenderPostMsg(
            projectDomain = "example.com",
            fragmentSrcKey = "example.com/gen/post-body.html",
            metadata = postMeta
        )

        val sqsEvent = SQSEvent().apply {
            records = listOf(SQSEvent.SQSMessage().apply { body = Json.encodeToString<TemplateSQSMessage>(msg) })
        }

        val handler = TemplateProcessorHandler(env)
        val result = handler.handleRequest(sqsEvent, mockContext)

        assertEquals("200 OK", result)
        verify { mockS3.putObjectAsString("example.com/posts/2024/01/hello", "dest-bkt", any(), "text/html") }
        assertTrue(bodySlot.captured.contains("<p>Post</p>"))
        assertTrue(bodySlot.captured.contains("<title>Hello</title>"))
    }

    @Test
    fun `happy path - static render`() {
        val project = CantileverProject(domain = "example.com", projectName = "Example", author = "Alice")
        coEvery { mockDynamo.getProject("example.com") } returns project

        every { mockS3.getObjectAsString("example.com/assets/site.css.hbs", "source-bkt") } returns "body{color:{{project.projectName}};}"

        val cssSlot: CapturingSlot<String> = slot()
        every { mockS3.putObjectAsString("example.com/assets/site.css", "dest-bkt", capture(cssSlot), "text/css") } returns 789

        val msg = TemplateSQSMessage.StaticRenderMsg(
            projectDomain = "example.com",
            srcKey = "example.com/assets/site.css.hbs",
            destinationKey = "assets/site.css"
        )

        val sqsEvent = SQSEvent().apply {
            records = listOf(SQSEvent.SQSMessage().apply { body = Json.encodeToString<TemplateSQSMessage>(msg) })
        }

        val handler = TemplateProcessorHandler(env)
        val result = handler.handleRequest(sqsEvent, mockContext)

        assertEquals("200 OK", result)
        verify { mockS3.putObjectAsString("example.com/assets/site.css", "dest-bkt", any(), "text/css") }
        assertEquals("body{color:Example;}", cssSlot.captured)
    }
}
