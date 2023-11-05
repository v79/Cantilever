package org.liamjd.cantilever.api.controllers

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import io.mockk.*
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.AfterEach
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
import org.liamjd.cantilever.models.Template
import org.liamjd.cantilever.models.TemplateMetadata
import org.liamjd.cantilever.models.rest.HandlebarsTemplate
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockKExtension::class)
internal class TemplateControllerTest : KoinTest {

    private val mockS3: S3Service by inject()
    private val sourceBucket = "sourceBucket"

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(module {
            single<S3Service> { S3ServiceImpl(Region.EU_WEST_2) }
        })
    }

    @JvmField
    @RegisterExtension
    val mockProvider = MockProviderExtension.create { clazz ->
        mockkClass(clazz)
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `respond to request to load handlebars source`() {
        val mockResponse = mockk<GetObjectResponse>()
        val srcKey = "my-template.hbs"
        val mockBody = """
            ---
            name: My Template
            ---
        """.trimIndent()
        declareMock<S3Service> {
            every { mockS3.objectExists("my-template.hbs", sourceBucket) } returns true
            every { mockS3.getObject(any(), sourceBucket) } returns mockResponse
            every { mockS3.getObjectAsString(any(), sourceBucket) } returns mockBody
        }
        every { mockResponse.lastModified() } returns Instant.now()


        val controller = TemplateController(sourceBucket)
        val request = buildRequest(path = "/templates/$srcKey", pathPattern = "/templates/{srcKey}")
        val response = controller.loadHandlebarsSource(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `returns a 404 when a missing srcKey is invalid`() {
        val srcKey = "my-template.hbs"
        declareMock<S3Service> {
            every { mockS3.objectExists("my-template.hbs", sourceBucket) } returns false
        }

        val controller = TemplateController(sourceBucket)
        val request = buildRequest(path = "/templates/$srcKey", pathPattern = "/templates/{srcKey}")
        val response = controller.loadHandlebarsSource(request)

        assertNotNull(response)
        assertEquals(404, response.statusCode)
    }

    @Test
    fun `saves a new template file when none exists`() {
        val mockHandlebarsContent = mockk<HandlebarsTemplate>()
        val mockTemplate = mockk<Template>()
        val mockTemplateMeta = mockk<TemplateMetadata>()
        val body = """
            {{{ name }}}
        """.trimIndent()
        every { mockTemplate.key } returns "my-template"
        every { mockTemplate.metadata } returns mockTemplateMeta
        every { mockTemplateMeta.name } returns "My Template"
        every { mockHandlebarsContent.template } returns mockTemplate
        every { mockHandlebarsContent.body } returns body
        every { mockTemplateMeta.sections } returns listOf("body")

        declareMock<S3Service> {
            every { mockS3.objectExists("my-template", sourceBucket) } returns true
            every { mockS3.putObject("my-template", sourceBucket, any(), "text/html") } returns 1234
        }

        val apiProxyEvent = APIGatewayProxyRequestEvent()

        val controller = TemplateController(sourceBucket)
        val request = Request<HandlebarsTemplate>(
            apiRequest = apiProxyEvent,
            body = mockHandlebarsContent,
            pathPattern = "/templates/"
        )
        val response = controller.saveTemplate(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `updates an existing template file`() {
        val mockHandlebarsContent = mockk<HandlebarsTemplate>()
        val mockTemplate = mockk<Template>()
        val mockTemplateMeta = mockk<TemplateMetadata>()
        val body = """
            {{{ name }}}
        """.trimIndent()
        every { mockTemplate.key } returns "my-template"
        every { mockTemplate.metadata } returns mockTemplateMeta
        every { mockTemplateMeta.name } returns "My Template"
        every { mockHandlebarsContent.template } returns mockTemplate
        every { mockHandlebarsContent.body } returns body
        every { mockTemplateMeta.sections } returns listOf("body")

        declareMock<S3Service> {
            every { mockS3.objectExists("my-template", sourceBucket) } returns true
            every { mockS3.putObject("my-template", sourceBucket, any(), "text/html") } returns 1234
        }

        val apiProxyEvent = APIGatewayProxyRequestEvent()

        val controller = TemplateController(sourceBucket)
        val request = Request<HandlebarsTemplate>(
            apiRequest = apiProxyEvent,
            body = mockHandlebarsContent,
            pathPattern = "/templates/"
        )
        val response = controller.saveTemplate(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    /**
     * Utility function to build the fake request object
     */
    private fun buildRequest(path: String, pathPattern: String, body: String = ""): Request<Unit> {
        val apiGatewayProxyRequestEvent = APIGatewayProxyRequestEvent()
        apiGatewayProxyRequestEvent.body = body
        apiGatewayProxyRequestEvent.path = path
        return Request(apiGatewayProxyRequestEvent, Unit, pathPattern)
    }
}