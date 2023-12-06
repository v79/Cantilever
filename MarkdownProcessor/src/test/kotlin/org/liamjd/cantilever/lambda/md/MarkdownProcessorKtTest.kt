package org.liamjd.cantilever.lambda.md

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MarkdownProcessorKtTest {

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
        val actual = convertMDToHTML(mdSource)
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
        val actual = convertMDToHTML(mdSource)
        assertEquals(expected, actual.trim())
    }
}