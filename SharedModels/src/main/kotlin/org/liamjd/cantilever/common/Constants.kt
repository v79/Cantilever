package org.liamjd.cantilever.common

/**
 * Represents the standard folders within S3 bucket
 */
object S3_KEY {
    const val sources = "sources/"
    const val fragments = "generated/htmlFragments/"
    const val templates = "templates/"
}

/**
 * Standard input file types
 */
object FILE_TYPE {
    const val MD = ".md"
    const val HTML_HBS = ".html.hbs"
    const val HTML = ".html"
}

/**
 * The core types of object that Cantilever can process
 */
object SOURCE_TYPE {
    const val PAGES = "pages"
    const val POSTS = "posts"
    const val STATICS = "statics"
}

object QUEUE {
    const val MARKDOWN = "markdown_processing_queue"
    const val HANDLEBARS = "handlebar_template_queue"
}