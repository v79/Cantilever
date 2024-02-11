package org.liamjd.cantilever.lambda.md

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MarkdownProcessorKtTest {

    private val converter = FlexmarkMarkdownConverter()
    @Test
    fun `test convertMDToHTML`() {
        val mdSource = """
            # This is a test
            ## This is a subheading
            This is some text
            """.trimIndent()
        val expected = """
            <h1>This is a test</h1>
            <h2>This is a subheading</h2>
            <p>This is some text</p>
            """.trimIndent()
        val actual = converter.convertMDToHTML(mdSource)
        assertEquals(expected, actual.trim())
    }

    @Test
    fun `convert to html when there is an image`() {
        val mdSource = """
            ![image](/images/my-image.jpg "image title here")
            """.trimIndent()
        val expected = """
            <p><img src="/images/my-image.jpg" alt="image" title="image title here" /></p>
        """.trimIndent()
        val actual = converter.convertMDToHTML(mdSource)
        assertEquals(expected, actual.trim())
    }

    @Test
    fun `extract images from markdown`() {
        val mdSource = """
            # This is a test
            ## This is a subheading
            This is some text
            ![image](/images/my-image.jpg "image title here")
            """.trimIndent()
        val expected = 1
        val actual = converter.extractImages(mdSource)
        assertEquals(expected, actual.size)
        actual.forEach { image ->
            assertEquals("/images/my-image.jpg", image.url.toString())
            assertEquals("image title here", image.title.toString())
        }
    }
}