package org.liamjd.cantilever.api.controllers

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import io.mockk.coEvery
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkClass
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
import org.liamjd.cantilever.models.CantileverProject
import org.liamjd.cantilever.services.DynamoDBService
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.impl.DynamoDBServiceImpl
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockKExtension::class)
internal class ProjectControllerTest : KoinTest {

    private val mockS3: S3Service by inject()
    private val mockDynamoDB: DynamoDBService by inject()
    private val sourceBucket = "sourceBucket"
    private val generationBucket = "generationBucket"
    private val srcKey = "www.cantilevers.org"
    private val postsKey = "generated/posts.json"
    private val domain = "www.cantilevers.org"
    private val projectName = "Cantilevers"

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

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `returns a project definition object`() {
        val mockProject = CantileverProject(
            domain = domain,
            projectName = projectName,
            author = "Author name",
            dateFormat = "dd/MM/yyyy",
            dateTimeFormat = "HH:mm dd/MM/yyyy"
        )

        declareMock<DynamoDBService> {
            coEvery { mockDynamoDB.getProject(domain) } returns mockProject
        }

        val controller = ProjectController(sourceBucket, generationBucket)
        val request = buildRequest(path = "/project/www.cantilevers.org", pathPattern = "/project/{projectKey}")
        val response = controller.getProject(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `returns 404 if project is not found`() {
        declareMock<DynamoDBService> {
            coEvery { mockDynamoDB.getProject(domain) } returns null
        }

        val controller = ProjectController(sourceBucket, generationBucket)
        val request = buildRequest(path = "/project/www.cantilevers.org", pathPattern = "/project/{projectKey}")
        val response = controller.getProject(request)

        assertNotNull(response)
        assertEquals(404, response.statusCode)
    }

    @Test
    fun `successfully updates existing project definition`() {
        val mockCantileverProject = CantileverProject(
            domain = "example.com",
            projectName = "Project name 2",
            author = "Author name"
        )

        val updatedProject = CantileverProject(
            domain = "example.com",
            projectName = "Project name 2",
            author = "Author name",
            dateFormat = "dd/MM/yyyy",
            dateTimeFormat = "HH:mm dd/MM/yyyy"
        )

        declareMock<DynamoDBService> {
            coEvery { mockDynamoDB.saveProject(any()) } returns updatedProject
        }

        val apiProxyEvent = APIGatewayProxyRequestEvent()

        val controller = ProjectController(sourceBucket, generationBucket)
        val request = org.liamjd.apiviaduct.routing.Request(
            apiRequest = apiProxyEvent,
            body = mockCantileverProject,
            pathPattern = "/project/{projectKey}"
        )
        val response = controller.updateProjectDefinition(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `return 400 if project name is blank`() {
        val mockProject = CantileverProject(
            domain = "https://example.com",
            projectName = "",
            author = "Author name"
        )

        val apiProxyEvent = APIGatewayProxyRequestEvent()

        val controller = ProjectController(sourceBucket, generationBucket)
        val request = org.liamjd.apiviaduct.routing.Request(
            apiRequest = apiProxyEvent,
            body = mockProject,
            pathPattern = "/project/"
        )
        val response = controller.updateProjectDefinition(request)

        assertNotNull(response)
        assertEquals(400, response.statusCode)
    }

    @Test
    fun `successfully creates a new project`() {
        val mockCantileverProject = CantileverProject(
            domain = "newdomain.com",
            projectName = "New Project",
            author = "Author name"
        )

        val savedProject = CantileverProject(
            domain = "newdomain.com",
            projectName = "New Project",
            author = "Author name",
            dateFormat = "dd/MM/yyyy",
            dateTimeFormat = "HH:mm dd/MM/yyyy"
        )

        declareMock<DynamoDBService> {
            coEvery { mockDynamoDB.getProject("newdomain.com") } returns null
            coEvery { mockDynamoDB.saveProject(any()) } returns savedProject
        }

        val apiProxyEvent = APIGatewayProxyRequestEvent()

        val controller = ProjectController(sourceBucket, generationBucket)
        val request = org.liamjd.apiviaduct.routing.Request(
            apiRequest = apiProxyEvent,
            body = mockCantileverProject,
            pathPattern = "/project/"
        )
        val response = controller.createProject(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `returns conflict if project already exists`() {
        val mockCantileverProject = CantileverProject(
            domain = "existingdomain.com",
            projectName = "Existing Project",
            author = "Author name"
        )

        val existingProject = CantileverProject(
            domain = "existingdomain.com",
            projectName = "Existing Project",
            author = "Author name",
            dateFormat = "dd/MM/yyyy",
            dateTimeFormat = "HH:mm dd/MM/yyyy"
        )

        declareMock<DynamoDBService> {
            coEvery { mockDynamoDB.getProject("existingdomain.com") } returns existingProject
        }

        val apiProxyEvent = APIGatewayProxyRequestEvent()

        val controller = ProjectController(sourceBucket, generationBucket)
        val request = org.liamjd.apiviaduct.routing.Request(
            apiRequest = apiProxyEvent,
            body = mockCantileverProject,
            pathPattern = "/project/"
        )
        val response = controller.createProject(request)

        assertNotNull(response)
        assertEquals(409, response.statusCode)
    }

    /**
     * Utility function to build the fake request object
     */
    private fun buildRequest(
        path: String,
        pathPattern: String,
        body: String = ""
    ): org.liamjd.apiviaduct.routing.Request<Unit> {
        val apiGatewayProxyRequestEvent = APIGatewayProxyRequestEvent()
        apiGatewayProxyRequestEvent.body = ""
        apiGatewayProxyRequestEvent.path = path
        apiGatewayProxyRequestEvent.headers = mapOf("cantilever-project-domain" to "test")
        return org.liamjd.apiviaduct.routing.Request(apiGatewayProxyRequestEvent, Unit, pathPattern)
    }
}