package org.liamjd.cantilever.api.controllers

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.coEvery
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
import org.liamjd.cantilever.common.SOURCE_TYPE
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.ImageDTO
import org.liamjd.cantilever.services.DynamoDBService
import org.liamjd.cantilever.services.S3Service
import kotlin.io.encoding.Base64
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockKExtension::class)
internal class MediaControllerTest : KoinTest {

    private val mockS3: S3Service by inject()
    private val mockDynamo: DynamoDBService by inject()
    private val sourceBucket = "sourceBucket"
    private val generationBucket = "generationBucket"

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(module { })
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
    fun `getImageList returns ImageListDTO with images from dynamo`() {
        val images = listOf(
            ContentNode.ImageNode(srcKey = "test/sources/images/a.jpg", lastUpdated = kotlinx.datetime.Clock.System.now()),
            ContentNode.ImageNode(srcKey = "test/sources/images/b.jpg", lastUpdated = kotlinx.datetime.Clock.System.now())
        )
        declareMock<DynamoDBService> {
            coEvery { mockDynamo.listAllNodesForProject("test", SOURCE_TYPE.Images) } returns images
        }
        val controller = MediaController(sourceBucket, generationBucket)
        val request = buildRequest(path = "/media/images", pathPattern = "/media/images")

        val response = controller.getImageList(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
        val body = response.body
        assert(body is org.liamjd.cantilever.api.models.APIResult.Success)
        val dto = (body as org.liamjd.cantilever.api.models.APIResult.Success).value
        assertEquals(2, dto.count)
        assertEquals(2, dto.images.size)
    }

    @Test
    fun `getImage returns base64 image bytes when object exists with resolution`() {
        val encodedBytes = Base64.encode(byteArrayOf(1,2,3,4))
        val srcKey = "test.com%2Fsources%2Fimages%2Fpic.jpg"
        val generatedKey = "test.com/generated/images/pic/__thumb.jpg"
        declareMock<S3Service> {
            every { mockS3.objectExists(generatedKey, generationBucket) } returns true
            every { mockS3.getObjectAsBytes(generatedKey, generationBucket) } returns byteArrayOf(1,2,3,4)
        }
        val controller = MediaController(sourceBucket, generationBucket)
        val request = buildRequest(path = "/media/image/$srcKey/__thumb", pathPattern = "/media/image/{srcKey}/{resolution}")

        val response = controller.getImage(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
        val body = response.body as org.liamjd.cantilever.api.models.APIResult.Success<ImageDTO>
        assertEquals(srcKey, body.value.srcKey)
        assertEquals("image/jpeg", body.value.contentType)
        assertEquals(encodedBytes, body.value.bytes)
    }

    @Test
    fun `uploadImage succeeds and returns ok`() {
        val imageBytes = Base64.encode(byteArrayOf(9,8,7))
        val dataUrl = "data:image/png;base64,$imageBytes"
        val srcKey = "sources/images/folder/file.png"
        val dto = ImageDTO(srcKey = "folder/file.png", contentType = "image/png", bytes = dataUrl)
        declareMock<S3Service> {
            every { mockS3.putObjectAsBytes(key = "test/${srcKey}", bucket = sourceBucket, contents = any(), contentType = "image/png") } returns 3
        }
        val controller = MediaController(sourceBucket, generationBucket)
        val request = org.liamjd.apiviaduct.routing.Request(
            apiRequest = APIGatewayProxyRequestEvent().withHeaders(mapOf("cantilever-project-domain" to "test")),
            body = dto,
            pathPattern = "/media/upload"
        )

        val response = controller.uploadImage(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
        val body = response.body as org.liamjd.cantilever.api.models.APIResult.Success<ImageDTO>
        assertEquals("test/${srcKey}", body.value.srcKey)
        assertEquals("image/png", body.value.contentType)
    }

    @Test
    fun `deleteImage deletes original and generated variants and returns ok`() {
        val srcKeyEncoded = "test.com%2Fsources%2Fimages%2Fphoto.webp"
        val controller = MediaController(sourceBucket, generationBucket)
        // initialise project to avoid lateinit crash in deleteImage
        controller.project = org.liamjd.cantilever.models.CantileverProject(
            domain = "test",
            projectName = "Test",
            author = "Author",
            imageResolutions = mapOf("small" to org.liamjd.cantilever.models.ImgRes(100,100))
        )
        declareMock<S3Service> {
            every { mockS3.deleteObject(any(), any()) } returns io.mockk.mockk()
        }
        val request = buildRequest(path = "/media/image/$srcKeyEncoded", pathPattern = "/media/image/{srcKey}")

        val response = controller.deleteImage(request)

        assertNotNull(response)
        assertEquals(200, response.statusCode)
    }

    @Test
    fun `generated key calculation works correctly`() {
        val controller = MediaController(sourceBucket, generationBucket)
        val key1 = controller.calculateGeneratedKey("example.com/sources/images/pic.jpg", "jpg", null)
        assertEquals("example.com/generated/images/pic.jpg", key1)
        val key2 = controller.calculateGeneratedKey("example.com/sources/images/pic.jpg", "jpg", "thumb")
        assertEquals("example.com/generated/images/pic/__thumb.jpg", key2)
        val key3 = controller.calculateGeneratedKey("example.com/sources/images/folder/pic.png", "png", "small")
        assertEquals("example.com/generated/images/folder/pic/__small.png", key3)
        val key4 = controller.calculateGeneratedKey("example.com/sources/images/folder/subfolder/pic.gif", "gif", "medium")
        assertEquals("example.com/generated/images/folder/subfolder/pic/__medium.gif", key4)
    }

    private fun buildRequest(path: String, pathPattern: String): org.liamjd.apiviaduct.routing.Request<Unit> {
        val apiGatewayProxyRequestEvent = APIGatewayProxyRequestEvent()
        apiGatewayProxyRequestEvent.body = ""
        apiGatewayProxyRequestEvent.path = path
        apiGatewayProxyRequestEvent.headers = mapOf("cantilever-project-domain" to "test")
        return org.liamjd.apiviaduct.routing.Request(apiGatewayProxyRequestEvent, Unit, pathPattern)
    }
}
