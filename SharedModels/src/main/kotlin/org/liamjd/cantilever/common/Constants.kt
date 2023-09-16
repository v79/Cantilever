package org.liamjd.cantilever.common

/**
 * Represents the standard folders within S3 bucket
 */
object S3_KEY {
    const val sources = "sources/"
    const val fragments = "generated/htmlFragments/"
    const val projectKey = "cantilever.yaml"
    const val postsKey = "generated/posts.json"
    const val pagesKey = "generated/pages.json"
    const val templatesKey = "generated/templates.json"
    const val postsPrefix = "sources/posts/"
    const val pagesPrefix = "sources/pages/"
    const val templatesPrefix = "templates/"

}

/**
 * Standard input file types
 */
object FILE_TYPE {
    const val MD = ".md"
    const val HTML_HBS = ".html.hbs"
    const val HTML = ".html"
    const val YAML = ".yaml"
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