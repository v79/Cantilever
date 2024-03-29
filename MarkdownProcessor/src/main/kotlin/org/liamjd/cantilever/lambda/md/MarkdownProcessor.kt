package org.liamjd.cantilever.lambda.md

import com.vladsch.flexmark.ast.Image
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.data.MutableDataSet

/**
 * Using the flexmark-java library, convert the markdown source to an HTML string
 * Extensions enabled:
 * - [TablesExtension](https://github.com/vsch/flexmark-java/wiki/Tables-Extension)
 */

/**
 * Generic interface for markdown conversion functions
 */
interface MarkdownConverter {
    /**
     * Convert a markdown file to HTML
     * @param mdSource the markdown source
     * @return the HTML string
     */
    fun convertMDToHTML(mdSource: String): String

    /**
     * Extract a list of all the images from the markdown source.
     * This is so we can find out what image references are in the markdown source
     * @param mdSource the markdown source
     * @return a list of [Image] objects
     */
    fun extractImages(mdSource: String): List<Image>
}

class FlexmarkMarkdownConverter : MarkdownConverter {

    private val options = MutableDataSet().set(Parser.EXTENSIONS, listOf(TablesExtension.create()))
    private val parser: Parser = Parser.builder(options).build()

    override fun extractImages(mdSource: String): List<Image> {
        val document: Node = parser.parse(mdSource)
        return document.descendants.filterIsInstanceTo(mutableListOf<Image>()).toList()
    }

    override fun convertMDToHTML(mdSource: String): String {
        val renderer: HtmlRenderer = HtmlRenderer.builder(options).build()

        val document: Node = parser.parse(mdSource)
        return renderer.render(document)
    }
}
