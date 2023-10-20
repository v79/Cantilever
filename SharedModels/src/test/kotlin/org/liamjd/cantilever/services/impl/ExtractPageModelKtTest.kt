package org.liamjd.cantilever.services.impl

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.liamjd.cantilever.models.sqs.MarkdownSection

internal class ExtractPageModelKtTest {

    private val filename = "AFile"

    @Test
    fun `extract model for core metadata only`() {
        val result = extractPageModel(filename, coreOnly)

        assertEquals("index", result.templateKey)
        assertEquals(0, result.attributes.size)
        assertEquals(0, result.sections.size)
        assertNotNull(result.lastModified)
    }

    @Test
    fun `extract model with one named section not closed`() {
        val result = extractPageModel(filename, singleNamedSection)

        assertEquals("index", result.templateKey)
        assertEquals(0, result.attributes.size)
        assertEquals(1, result.sections.size)
        assertNotNull(result.lastModified)
        assertEquals("body", result.sections.keys.first())
    }

    @Test
    fun `extract model with one named section is closed`() {
        val result = extractPageModel(filename, sectionIsClosed)

        assertEquals("index", result.templateKey)
        assertEquals(0, result.attributes.size)
        assertEquals(1, result.sections.size)
        assertNotNull(result.lastModified)
        assertEquals("body", result.sections.keys.first())
    }

    @Test
    fun `extract model with multiple sections not closed`() {
        val result = extractPageModel(filename, multipleNamedSections)

        assertEquals("index", result.templateKey)
        assertEquals(0, result.attributes.size)
        assertEquals(2, result.sections.size)
        assertNotNull(result.lastModified)
        assertEquals("body", result.sections.keys.first())
    }

    @Test
    fun `extract model with custom attributes in metadata`() {
        val result = extractPageModel(filename, customMetadata)

        assertEquals("index", result.templateKey)
        assertEquals(1, result.attributes.size)
        assertEquals(0, result.sections.size)
        assertNotNull(result.lastModified)
        assertEquals("author", result.attributes.keys.first())
        assertEquals("Bob", result.attributes.values.first())
    }

    @Test
    fun `if template is blank it parses but no template is set`() {
        val result = extractPageModel(filename, templateMissing)

        assertEquals("", result.templateKey)
    }

    @Test
    fun `sections without names are skipped`() {
        val result = extractPageModel(filename, unnamedSections)

        println(result)

        assertEquals("index", result.templateKey)
        assertEquals(0, result.attributes.size)
        assertEquals(0, result.sections.size)
        assertNotNull(result.lastModified)
    }

    @Test
    fun `extract a complex model`() {
        val result = extractPageModel(filename, complexContent)
        assertEquals("index", result.templateKey)
        assertEquals(3, result.attributes.size)
        assertEquals(3, result.sections.size)
        assertNotNull(result.lastModified)
        assertEquals("author", result.attributes.keys.first())
        assertEquals("Sue", result.attributes.values.first())
        val number = result.attributes["number"]
        assertEquals("27", number)
        val top = result.sections["top"]
        top?.let { t ->
            assertTrue(t.length > 50)
            assertTrue(t.endsWith("99"))
        }
        val middle = result.sections["middle"]
        middle?.let { m ->
            assertTrue(m.length > 50)
        }
        val bottom = result.sections["bottom"]
        bottom?.let { b ->
            assertTrue(b.startsWith("#"))
        }
    }

    @Test
    fun `extract metadata when embedded raw markdown contains block separators`() {
        val result = extractPageModel(filename, embeddedMarkdownDashes)

        assertEquals("index", result.templateKey)
        assertEquals(1, result.attributes.size)
        assertEquals(1, result.sections.size)
        assertNotNull(result.lastModified)
        assertEquals("author", result.attributes.keys.first())
        assertEquals("Bob", result.attributes.values.first())
    }

    @Test
    fun `extract metadata when markdown contains table with header elements`() {
        val result = extractPageModel(filename, embeddedMarkdownTable)

        assertEquals("index", result.templateKey)
        assertEquals(1, result.attributes.size)
        assertEquals(1, result.sections.size)
        assertNotNull(result.lastModified)
        assertEquals("author", result.attributes.keys.first())
        assertEquals("Bob", result.attributes.values.first())
    }

    @Test
    fun `extract metadata when markdown contains table with header elements real`() {
        val result = extractPageModel(filename, realPage)

        assertEquals("sources/templates/about.html.hbs", result.templateKey)
        assertEquals(0, result.attributes.size)
        assertEquals(1, result.sections.size)
        assertNotNull(result.lastModified)
        val section: MarkdownSection? = result.sections["body"]
        assertNotNull(section)
        section?.let {
            val expected = """
There are a number of values which are always available to page and post templates. Some are defined by the project, some are specific to the page type (page or post), and some are calculated.

### Common Elements


| col1 | col2 | col3 |
| ------ | ------ | ------ |
|      |      |      |
|      |      |      |
""".trimIndent()
            assertEquals(expected, it.trimIndent())
        }
    }
}

val templateMissing = """
    ---
    #author: Jim
    ---
""".trimIndent()

val coreOnly = """
    ---
    template: index
    ---
""".trimIndent()

val singleNamedSection = """
    ---
    template: index
    --- #body
    Body text
""".trimIndent()

val sectionIsClosed = """
     ---
    template: index
    --- #body
    Body text
    ---
""".trimIndent()

val multipleNamedSections = """
    ---
    template: index
    --- #body
    Body text
    --- #aside
    Aside goes here
""".trimIndent()

val unnamedSections = """
    ---
    template: index
    ---
    This is section 1.
    ---
    This is section 2.
""".trimIndent()

val customMetadata = """
    ---
    template: index
    #author: Bob
    ---
""".trimIndent()

val complexContent = """
    ---
    template: index
    #author: Sue
    #colour: Green
    #number: 27
    --- #Top
    Lorem ipsum dolor sit amet, consectetur adipiscing elit. Mauris dignissim, metus eu pretium ultricies, ante sem pulvinar purus, non maximus ipsum mauris vitae enim. Nulla commodo lorem vel scelerisque tempus. Pellentesque imperdiet, neque aliquam lacinia dictum, ipsum neque tempor dui, et feugiat justo dolor a lacus. Nullam quis est pharetra, congue sem vitae, elementum tellus. Nam pellentesque finibus dictum. Nunc id varius risus. Morbi egestas nisi porttitor, placerat nibh quis, volutpat mi. Aenean sollicitudin eleifend iaculis. Mauris vel mauris est. 
    
    Blank lines won't scare it.
    
    99
    --- #Middle
    Etiam ligula risus, suscipit at justo ut, cursus dignissim nulla. Aliquam erat volutpat. Maecenas eget rutrum eros. Phasellus efficitur vestibulum erat,
    --- #Bottom
    #Nothing special about this anchor here.
""".trimIndent()

val embeddedMarkdownDashes = """
    ---
    template: index
    #author: Bob
    --- #markdown
    Here is a complex piece of markdown, which contains a raw markdown block, which contains the triple-dash character!
    ```markdown
    Here it comes...
    ---
    That triple shouldn't confuse the metadata extractor.
    ```
    The end.
""".trimIndent()

val embeddedMarkdownTable = """
    ---
    template: index
    #author: Bob
    --- #markdown
    Here is a complex piece of markdown, which contains a table with empty cells
    | col1 | col2 |
    | ------ | ------ |
    | key | ------ |
    Might be tricky.
""".trimIndent()

val realPage = """
    ---
    title: Page Model
    template: sources/templates/about.html.hbs
    --- #body
    There are a number of values which are always available to page and post templates. Some are defined by the project, some are specific to the page type (page or post), and some are calculated.

    ### Common Elements


    | col1 | col2 | col3 |
    | ------ | ------ | ------ |
    |      |      |      |
    |      |      |      |
""".trimIndent()