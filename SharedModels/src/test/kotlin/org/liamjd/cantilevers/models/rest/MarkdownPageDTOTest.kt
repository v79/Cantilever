package org.liamjd.cantilevers.models.rest

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.rest.MarkdownPageDTO

class MarkdownPageDTOTest {

    private val separator = "---"

    @Test
    fun `should create simplest page with no custom parts`() {
        val pageMeta = ContentNode.PageNode(
            title = "Page",
            srcKey = "page.md",
            templateKey = "templateKey",
            slug = "",
            attributes = emptyMap(),
            sections = emptyMap(),
            isRoot = false
        )

        val markdownPageDTO = MarkdownPageDTO(metadata = pageMeta)

        val result = markdownPageDTO.toString()
        println(result)
        val lines = result.lines()
        assertEquals(separator,lines[0])
        assertEquals("title: Page", lines[1])
        assertEquals("templateKey: templateKey",lines[2])
        assertEquals(3,lines.size)
    }

    @Test
    fun `should create simplest page with custom attributes`() {
        val attributes = mapOf("name" to "Bob", "Age" to "43")
        val pageMeta = ContentNode.PageNode(
            title = "Page",
            srcKey = "page.md",
            templateKey = "templateKey",
            slug = "",
            attributes = attributes,
            sections = emptyMap(),
            isRoot = false
        )

        val markdownPageDTO = MarkdownPageDTO(metadata = pageMeta)

        val result = markdownPageDTO.toString()
        println(result)
        val lines = result.lines()
        assertEquals(separator,lines[0])
        assertEquals("title: Page", lines[1])
        assertEquals("templateKey: templateKey",lines[2])
        assertEquals(5,lines.size)
    }

    @Test
    fun `should create simplest page with custom sections`() {
        val sections = mapOf("apples" to "Green and red", "Berries" to "Blue and straw")
        val pageMeta = ContentNode.PageNode(
            title = "Page",
            srcKey = "page.md",
            templateKey = "templateKey",
            slug = "",
            attributes = emptyMap(),
            sections = sections,
            isRoot = false
        )

        val markdownPageDTO = MarkdownPageDTO(metadata = pageMeta)

        val result = markdownPageDTO.toString()
        println(result)
        val lines = result.lines()
        assertEquals(separator,lines[0])
        assertEquals("title: Page", lines[1])
        assertEquals("templateKey: templateKey",lines[2])
        assertEquals(7,lines.size)
    }
}