package org.liamjd.cantilever.api.services

import com.amazonaws.services.lambda.runtime.LambdaLogger
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.liamjd.cantilever.common.now
import org.liamjd.cantilever.services.impl.extractPostMetadata

internal class ExtractMetadataKtTest {

    private val mockLogger = mockk<LambdaLogger>()
    private val service: StructureService = StructureService()

    @BeforeEach
    fun init() {
        every { mockLogger.log(any<String>()) } just runs
    }

    @Test
    fun `can deserialize a metadata string with no --- block or markdown text`() {
        // setup
        val source = """
            title: This is a simple post
            template: post
            date: 2023-01-03
        """.trimIndent()

        // execute
        val metadata = extractPostMetadata("simple.md", source)
        //verify
        assertNotNull(metadata)
        val expectedDate = LocalDate(2023, 1, 3)
        metadata.also { result ->
            assertEquals("This is a simple post", result.title)
            assertEquals("post", result.template)
            expectedDate.also { date ->
                assertEquals(date.year, result.date.year)
                assertEquals(date.month, result.date.month)
                assertEquals(date.dayOfYear, result.date.dayOfYear)
            }
        }
    }

    @Test
    fun `can deserialize a fully valid metadata string`() {
        // setup
        val source = """
            ---
            title: This is a simple post
            template: post
            date: 2023-01-03
            slug: simple-post
            ---
            This is where **the markdown** goes.
        """.trimIndent()
        val expectedDate = LocalDate(2023, 1, 3)

        // execute
        val metadata = extractPostMetadata("simple.md", source)
        //verify
        assertNotNull(metadata)
        metadata.also { result ->
            assertEquals("This is a simple post", result.title)
            assertEquals("post", result.template)
            assertEquals("simple-post", result.slug)
            expectedDate.also { date ->
                assertEquals(date.year, result.date.year)
                assertEquals(date.month, result.date.month)
                assertEquals(date.dayOfYear, result.date.dayOfYear)
            }
        }
    }

    @Test
    fun `generates default metadata when no yaml block found`() {
        // setup
        val filename = "This is a file.md"
        val source = """
            This *markdown* block has no metadata
        """.trimIndent()
        val expectedTitle = "This is a file"
        val expectedDate = LocalDate.now()

        // execute
        val metadata = extractPostMetadata(filename, source)
        metadata.also { result ->
            assertEquals(expectedTitle, result.title)
            assertEquals("post", result.template)
            assertEquals(expectedDate, result.date)
            assertEquals("this-is-a-file", result.slug)
        }
    }

    @Test
    fun `uses PostMetadata defaults when template and slug is missing`() {
        // setup
        val source = """
            ---
            title: This is a simple post
            date: 2023-01-03
            ---
            This is where **the markdown** goes.
        """.trimIndent()
        val expectedDate = LocalDate(2023, 1, 3)

        // execute
        val metadata = extractPostMetadata("simple.md", source)
        //verify
        assertNotNull(metadata)
        metadata.also { result ->
            assertEquals("This is a simple post", result.title)
            assertEquals("post", result.template)
            // if a title is supplied, it forms the slug. If it's not, the filename does in real use
            assertEquals("this-is-a-simple-post", result.slug)
            expectedDate.also { date ->
                assertEquals(date.year, result.date.year)
                assertEquals(date.month, result.date.month)
                assertEquals(date.dayOfYear, result.date.dayOfYear)
            }
        }
    }

    /**
     * This behaviour is probably not what I want; some logic around trying to sensibly fill in the blanks would be good
     */
    @Test
    fun `returns the default object when a required field is missing`() {
        // setup
        val filename = "This is a file.md"
        val source = """
            ---
            template: post
            date: 2023-01-03
            ---
            This is where **the markdown** goes.
        """.trimIndent()
        val expectedDate = LocalDate.now()
        val expectedTitle = "This is a file"

        // execute
        val metadata = extractPostMetadata(filename, source)
        metadata.also { result ->
            assertEquals(expectedTitle, result.title)
            assertEquals("post", result.template)
            assertEquals(expectedDate, result.date)
            assertEquals("this-is-a-file", result.slug)
        }
    }
}