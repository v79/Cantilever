package org.liamjd.cantilever.api.controllers

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import io.mockk.*
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.test.KoinTest
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.inject
import org.koin.test.junit5.KoinTestExtension
import org.koin.test.junit5.mock.MockProviderExtension
import org.koin.test.mock.declareMock
import org.liamjd.cantilever.api.models.APIResult
import org.liamjd.cantilever.models.CantileverProject
import org.liamjd.cantilever.routing.Request
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import software.amazon.awssdk.regions.Region

import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockKExtension::class)
internal class ProjectControllerTest : KoinTest {

    private val mockS3: S3Service by inject()
    private val sourceBucket = "sourceBucket"
    private val srcKey = "sources/cantilever.yaml"
    private val postsKey = "generated/posts.json"

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
    fun `returns a project definition object`() {
        val mockYaml = """
            projectName: "Project name"
            author: "Author name"
            dateFormat: "dd/MM/yyyy"
            dateTimeFormat: "HH:mm dd/MM/yyyy"
        """.trimIndent()
        declareMock<S3Service> {
            every { mockS3.objectExists(srcKey, sourceBucket) } returns true
            every { mockS3.getObjectAsString(srcKey, sourceBucket) } returns mockYaml
        }

        val controller = ProjectController(sourceBucket)
        val request = buildRequest(path = "/project/", pathPattern = "")
        val response = controller.getProject(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `returns 404 if project file is not found`() {
        declareMock<S3Service> {
            every { mockS3.objectExists(srcKey, sourceBucket) } returns false
        }

        val controller = ProjectController(sourceBucket)
        val request = buildRequest(path = "/project/", pathPattern = "")
        val response = controller.getProject(request)

        assertNotNull(response)
        println(response)
        assertEquals(404, response.statusCode)
    }

    @Test
    fun `returns a 500 if the yaml file is invalid`() {
        val mockYaml = """
            author: "Author name"
            dateFormat: "dd/MM/yyyy"
            dateTimeFormat: "HH:mm dd/MM/yyyy"
        """.trimIndent()
        declareMock<S3Service> {
            every { mockS3.objectExists(srcKey, sourceBucket) } returns true
            every { mockS3.getObjectAsString(srcKey, sourceBucket) } returns mockYaml
        }

        val controller = ProjectController(sourceBucket)
        val request = buildRequest(path = "/project/", pathPattern = "")
        val response = controller.getProject(request)

        assertNotNull(response)
        assertEquals(500, response.statusCode)
    }

    @Test
    fun `successfully updates existing project definition`() {
        val mockYaml = """
            projectName: "Project name"
            author: "Author name"
            dateFormat: "dd/MM/yyyy"
            dateTimeFormat: "HH:mm dd/MM/yyyy"
        """.trimIndent()
        val mockProject = CantileverProject(
            projectName = "Project name",
            author = "Author name",
            dateFormat = "dd/MM/yyyy",
            dateTimeFormat = "HH:mm dd/MM/yyyy",
            imageResolutions = emptyMap(),
            attributes = null
        )
        declareMock<S3Service> {
            every { mockS3.getObjectAsString(srcKey, sourceBucket) } returns mockYaml
            every {
                mockS3.putObject(
                    srcKey,
                    sourceBucket,
                    any(),
                    "application/yaml"
                )
            } returns 2345
        }

        val apiProxyEvent = APIGatewayProxyRequestEvent()

        val controller = ProjectController(sourceBucket)
        val request = Request(
            apiRequest = apiProxyEvent,
            body = mockProject,
            pathPattern = "/project/"
        )
        val response = controller.updateProjectDefinition(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `return 400 if project name is blank`() {
        val mockProject = CantileverProject(
            projectName = "",
            author = "Author name",
            dateFormat = "dd/MM/yyyy",
            dateTimeFormat = "HH:mm dd/MM/yyyy",
            imageResolutions = emptyMap(),
            attributes = null
        )


        val apiProxyEvent = APIGatewayProxyRequestEvent()

        val controller = ProjectController(sourceBucket)
        val request = Request(
            apiRequest = apiProxyEvent,
            body = mockProject,
            pathPattern = "/project/"
        )
        val response = controller.updateProjectDefinition(request)

        assertNotNull(response)
        assertEquals(400, response.statusCode)
    }

    @Test
    fun `returns a set of a single Post`() {
        val mockPostsJson = """
            {
            "count": 1,
            "lastUpdated": "2023-09-16T07:29:30.951027439Z",
            "posts": [{
            "title": "A Post",
            "srcKey": "sources/posts/a-post.md",
            "url": "a-post",
            "date": "2023-09-10",
            "lastUpdated": "2023-09-16T07:29:30.385391301Z",
            "templateKey": "templates/post.html.hbs"
            }]
            }
        """.trimIndent()
        declareMock<S3Service> {
            every { mockS3.objectExists(postsKey, sourceBucket) } returns true
            every { mockS3.getObjectAsString(postsKey, sourceBucket) } returns mockPostsJson
        }
        val controller = ProjectController(sourceBucket)
        val request = buildRequest(path = "/project/posts", pathPattern = "/project/posts")
        val response = controller.getPosts(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
        assertNotNull(response.body) {
            val success = it as APIResult.Success
            assertEquals(1, success.value.count)
        }
    }

    @Test
    fun `return 404 when posts json is not found`() {
        declareMock<S3Service> {
            every { mockS3.objectExists(postsKey, sourceBucket) } returns false
        }

        val controller = ProjectController(sourceBucket)
        val request = buildRequest(path = "/project/posts", pathPattern = "/project/posts")
        val response = controller.getPosts(request)

        assertNotNull(response)
        assertEquals(404, response.statusCode)
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