package org.liamjd.cantilevers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.liamjd.cantilever.common.getFrontMatter
import org.liamjd.cantilever.common.stripFrontMatter

internal class ExtensionsTest {
    private val simpleSource = """
        ---
        name: bob
        ---
        Text here
        """.trimIndent()

    private val multiSectionPage = """
        ---
        name: bob
        --- #body
        Text here
        --- # end
        Ending here
    """.trimIndent()

    @Test
    fun `get frontmatter for simple source`() {
        val frontmatter = simpleSource.getFrontMatter().trim();
        assertEquals("name: bob", frontmatter)
    }

    @Test
    fun `strip frontmatter for simple source`() {
        val body = simpleSource.stripFrontMatter().trim()
        assertEquals("Text here",body)
    }

    @Test
    fun `get frontmatter for multi section page`() {
        val frontmatter = multiSectionPage.getFrontMatter().trim();
        assertEquals("name: bob",frontmatter)
    }

    @Test
    fun `strip frontmatter for multi section page`() {
        val body = simpleSource.stripFrontMatter().trim()
        assertEquals("Text here",body)
    }
}