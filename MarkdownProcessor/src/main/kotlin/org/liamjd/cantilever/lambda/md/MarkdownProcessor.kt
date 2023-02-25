package org.liamjd.cantilever.lambda.md

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.data.MutableDataSet

/**
 * Using the flexmark-java library, convert the markdown source to an HTML string
 */
fun convertMDToHTML(mdSource: String): String {
    val options = MutableDataSet()
    val parser: Parser = Parser.builder(options).build()
    val renderer: HtmlRenderer = HtmlRenderer.builder(options).build()

    val document: Node = parser.parse(mdSource)
    val html: String = renderer.render(document)
    return html
}