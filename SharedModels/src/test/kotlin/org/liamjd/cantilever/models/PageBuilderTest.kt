package org.liamjd.cantilever.models

import org.liamjd.cantilever.models.ContentMetaDataBuilder.PageBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PageBuilderTest {

    @Test
    fun `buildFromSourceString should parse valid frontmatter and create a PageNode`() {
        // Given
        val sourceString = """
            ---
            title: About Me
            templateKey: sampleTemplate
            slug: about-me
            isRoot: true
            #customAttribute: customValue
            ---
            Some page content here.
        """.trimIndent()
        val srcKey = "sources/pages/bio/about-me.md"

        // When
        val result = PageBuilder.buildFromSourceString(sourceString, srcKey)

        // Then
        assertNotNull(result)
        assertEquals("About Me", result.title)
        assertEquals("sampleTemplate", result.templateKey)
        assertEquals("bio/about-me", result.slug)
        assertTrue(result.isRoot)
        assertEquals("customValue", result.attributes["customAttribute"])
        assertTrue(result.sections.isEmpty())
        assertEquals("sources/pages/bio", result.parent)
    }

    @Test
    fun `buildFromSourceString should handle missing frontmatter`() {
        // Given
        val sourceString = """
            ---
            Some page content without valid frontmatter.
        """.trimIndent()
        val srcKey = "pages/anotherPage.md"

        // When
        val result = PageBuilder.buildFromSourceString(sourceString, srcKey)

        // Then
        assertNotNull(result)
        assertEquals(srcKey, result.title)
        assertTrue(result.templateKey.contains("~~~templateKey wasn't found"))
        assertEquals("pages/anotherPage", result.slug)
        assertFalse(result.isRoot)
        assertTrue(result.attributes.isEmpty())
        assertTrue(result.sections.isEmpty())
    }

    @Test
    fun `buildFromSourceString should parse sections to extract just the section keys`() {
        // Given
        val sourceString = """
            ---
            title: Page With Sections
            templateKey: sectionTemplate
            ---
            --- #section1
            This is the first section content.
            
            --- #section2
            This is the second section content.
        """.trimIndent()
        val srcKey = "pages/sectionedPage.md"

        // When
        val result = PageBuilder.buildFromSourceString(sourceString, srcKey)

        // Then
        assertNotNull(result)
        assertEquals("Page With Sections", result.title)
        assertEquals("sectionTemplate", result.templateKey)
        assertEquals(2, result.sections.size)
        println(result.sections)
        assertTrue(result.sections["section1"].isNullOrEmpty())
        assertTrue(result.sections["section2"].isNullOrEmpty())
    }

    @Test
    fun `buildFromSourceString should handle empty sections`() {
        // Given
        val sourceString = """
            ---
            title: Empty Sections Page
            templateKey: noSectionTemplate
            ---
        """.trimIndent()
        val srcKey = "pages/emptySectionsPage.md"

        // When
        val result = PageBuilder.buildFromSourceString(sourceString, srcKey)

        // Then
        assertNotNull(result)
        assertEquals("Empty Sections Page", result.title)
        assertEquals("noSectionTemplate", result.templateKey)
        assertTrue(result.sections.isEmpty())
    }

    @Test
    fun `buildFromSourceString should handle additional custom attributes`() {
        // Given
        val sourceString = """
            ---
            title: Custom Attributes Page
            templateKey: customTemplate
            slug: custom-attributes
            #attr1: value1
            #attr2: value2
            ---
        """.trimIndent()
        val srcKey = "pages/customAttributesPage.md"

        // When
        val result = PageBuilder.buildFromSourceString(sourceString, srcKey)

        // Then
        assertNotNull(result)
        assertEquals("Custom Attributes Page", result.title)
        assertEquals("customTemplate", result.templateKey)
        assertEquals("pages/custom-attributes", result.slug)
        assertEquals(2, result.attributes.size)
        assertEquals("value1", result.attributes["attr1"])
        assertEquals("value2", result.attributes["attr2"])
        assertTrue(result.sections.isEmpty())
    }

    @Test
    fun `extract sections from source text`() {
        val sourceString = """
            ---
            title: Test Page
            templateKey: templates/page
            slug: test-page
            isRoot: false
            #customAttribute: customValue
            ---
            --- #introduction
            This is the introduction section.
            
            --- #details
            These are the details.
            
            --- #conclusion
            This is the conclusion.
        """.trimIndent()

        val sections = PageBuilder.extractSectionsFromSource(sourceString, includeSectionBodies = true)

        assertEquals(3, sections.size)
        assertEquals("This is the introduction section.", sections["introduction"]?.trim())
        assertEquals("These are the details.", sections["details"]?.trim())
        assertEquals("This is the conclusion.", sections["conclusion"]?.trim())
    }
}