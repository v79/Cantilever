package org.liamjd.cantilever.lambda.image

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
import org.liamjd.cantilever.common.S3_KEY
import org.liamjd.cantilever.models.CantileverProject
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.ImgRes
import org.liamjd.cantilever.models.sqs.ImageSQSMessage
import org.liamjd.cantilever.services.DynamoDBService
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.SQSService
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.assertEquals

class ImageProcessorHandlerTest : KoinTest {

    private val mockS3: S3Service by inject()
    private val mockDynamo: DynamoDBService by inject()
    private val mockSQS: SQSService by inject()
    private val mockContext: Context = mockk(relaxed = true)

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create { modules(module {
            single<S3Service> { mockk(relaxed = true) }
            single<DynamoDBService> { mockk(relaxed = true) }
            single<SQSService> { mockk(relaxed = true) }
        }) }

    @JvmField
    @RegisterExtension
    val mockProvider = MockProviderExtension.create { clazz -> mockkClass(clazz) }

    @BeforeEach
    fun setup() {
        // Set environment variables the handler reads
        setEnv(
            mapOf(
                "source_bucket" to "source-bkt",
                "generation_bucket" to "gen-bkt",
                "destination_bucket" to "dest-bkt"
            )
        )
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `happy path - resize image`() {
        // Arrange project with image resolutions
        val project = CantileverProject(
            domain = "example.com",
            projectName = "Example",
            author = "Alice",
            imageResolutions = mapOf(
                "small" to ImgRes(100, 100),
                "wide" to ImgRes(300, null)
            )
        )
        coEvery { mockDynamo.getProject("example.com") } returns project

        val imageKey = "example.com/sources/images/photo.png"

        // Create a tiny in-memory PNG
        val originalBytes = createPngBytes(20, 20)

        every { mockS3.objectExists(imageKey, any()) } returns true
        every { mockS3.getObjectAsBytes(imageKey, any()) } returns originalBytes
        every { mockS3.getContentType(imageKey, any()) } returns "image/png"

        // Capture writes
        val putKey: CapturingSlot<String> = slot()
        val putBucket: CapturingSlot<String> = slot()
        val putBytes: CapturingSlot<ByteArray> = slot()
        val putType: CapturingSlot<String> = slot()
        every {
            mockS3.putObjectAsBytes(capture(putKey), capture(putBucket), capture(putBytes), capture(putType))
        } returns 123

        // Copy original
        every { mockS3.copyObject(any(), any(), any(), any()) } returns 123

        val msg = ImageSQSMessage.ResizeImageMsg(
            projectDomain = "example.com",
            metadata = ContentNode.ImageNode(srcKey = imageKey).apply { contentType = "image/png" }
        )

        val sqsEvent = SQSEvent().apply {
            records = listOf(SQSEvent.SQSMessage().apply { body = Json.encodeToString<ImageSQSMessage>(msg) })
        }

        // Act
        val handler = ImageProcessorHandler()
        val result = handler.handleRequest(sqsEvent, mockContext)

        // Assert
        assertEquals("200 OK", result)
        // Expect at least 3 puts: 2 resolutions + 1 thumbnail
        verify(atLeast = 3) { mockS3.putObjectAsBytes(any(), any(), any(), any()) }

        // something like example.com/generated/images/photo.png/small.png for each resolution
        // plus example.com/generated/images/photo.png for the original
        // plus example.com/generated/images/photo.png/__thumb.png for the thumbnail
        // One of the keys should be the thumbnail
        val expectedThumbKey = "example.com/generated/images/${imageKey.substringAfterLast('/')}" + "/${S3_KEY.thumbnail}." + imageKey.substringAfterLast('.')
        assertEquals(
            putKey.captured,
            expectedThumbKey,
            "At least one resized write should occur; thumbnail key computed for reference: $expectedThumbKey"
        )
    }

    @Test
    fun `happy path - copy images`() {
        // Arrange
        val project = CantileverProject(domain = "example.com", projectName = "Example", author = "Alice")
        coEvery { mockDynamo.getProject("example.com") } returns project
        every { mockS3.copyObject(any(), any(), any(), any()) } returns 123

        val imageReq = "/images/pic.jpg"
        val msg = ImageSQSMessage.CopyImagesMsg(
            projectDomain = "example.com",
            imageList = listOf(imageReq)
        )

        val sqsEvent = SQSEvent().apply {
            records = listOf(SQSEvent.SQSMessage().apply { body = Json.encodeToString<ImageSQSMessage>(msg) })
        }

        // Act
        val handler = ImageProcessorHandler()
        val result = handler.handleRequest(sqsEvent, mockContext)

        // Assert
        assertEquals("200 OK", result)
        val expectedSource = "${S3_KEY.generated}${imageReq}"
        val expectedDest = project.domainKey + imageReq.removePrefix("/")
        verify(exactly = 1) { mockS3.copyObject(expectedSource, expectedDest, any(), any()) }
    }

    // Helpers
    private fun createPngBytes(w: Int, h: Int): ByteArray {
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val baos = ByteArrayOutputStream()
        ImageIO.write(img, "png", baos)
        return baos.toByteArray()
    }

    // Crude environment mutator for tests
    private fun setEnv(newenv: Map<String, String>) {
        try {
            val processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment")
            val theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment")
            theEnvironmentField.isAccessible = true
            val env = theEnvironmentField.get(null) as MutableMap<String, String>
            env.putAll(newenv)
            val theCaseInsensitiveEnvironmentField =
                processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment")
            theCaseInsensitiveEnvironmentField.isAccessible = true
            val cienv = theCaseInsensitiveEnvironmentField.get(null) as MutableMap<String, String>
            cienv.putAll(newenv)
        } catch (e: Exception) {
            // For other JVMs
            try {
                val classes = java.util.Collections::class.java.declaredClasses
                val env = System.getenv()
                for (cl in classes) {
                    if (cl.name == "java.util.Collections\$UnmodifiableMap") {
                        val field = cl.getDeclaredField("m")
                        field.isAccessible = true
                        val obj = field.get(env)
                        @Suppress("UNCHECKED_CAST")
                        val map = obj as MutableMap<String, String>
                        map.putAll(newenv)
                    }
                }
            } catch (_: Exception) { }
        }
    }
}
