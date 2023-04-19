package org.liamjd.cantilevers.models.rest

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.liamjd.cantilever.models.Page
import org.liamjd.cantilever.models.rest.MarkdownPage

class MarkdownPageTest {

    private val separator = "---"

    @Test
    fun `should create simplest page with no custom parts`() {
        val page = Page(title = "Page", srcKey = "page.md", templateKey = "templateKey",url = "", attributes = emptyMap(), sections = emptyMap())

        val markdownPage = MarkdownPage(metadata = page)

        val result = markdownPage.toString()
        println(result)
        val lines = result.lines()
        assertEquals(separator,lines[0])
        assertEquals("title: Page", lines[1])
        assertEquals("template: templateKey",lines[2])
        assertEquals(3,lines.size)
    }

    @Test
    fun `should create simplest page with custom attributes`() {
        val attributes = mapOf("name" to "Bob", "Age" to "43")
        val page = Page(title = "Page", srcKey = "page.md", templateKey = "templateKey",url = "", attributes = attributes, sections = emptyMap())

        val markdownPage = MarkdownPage(metadata = page)

        val result = markdownPage.toString()
        println(result)
        val lines = result.lines()
        assertEquals(separator,lines[0])
        assertEquals("title: Page", lines[1])
        assertEquals("template: templateKey",lines[2])
        assertEquals(5,lines.size)
    }

    @Test
    fun `should create simplest page with custom sections`() {
        val sections = mapOf("apples" to "Green and red", "Berries" to "Blue and straw")
        val page = Page(title = "Page", srcKey = "page.md", templateKey = "templateKey",url = "", attributes = emptyMap(),sections = sections)

        val markdownPage = MarkdownPage(metadata = page)

        val result = markdownPage.toString()
        println(result)
        val lines = result.lines()
        assertEquals(separator,lines[0])
        assertEquals("title: Page", lines[1])
        assertEquals("template: templateKey",lines[2])
        assertEquals(7,lines.size)
    }
}