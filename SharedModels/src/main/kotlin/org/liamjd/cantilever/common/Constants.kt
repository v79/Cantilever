package org.liamjd.cantilever.common

/**
 * Represents the standard folders and key files within S3 sources bucket
 */
@Deprecated("This has becoming less helpful as the project has grown")
object S3_KEY {
    const val sources = "sources"
    const val generated = "generated"
    const val sourcesPrefix = "$sources/"
    const val postsPrefix = "$sources/posts/"
    const val pagesPrefix = "$sources/pages/"
    const val imagesPrefix = "$sources/images/"
    const val templatesPrefix = "${sources}/templates/"
    const val staticsPrefix = "${sources}/statics/"
    const val projectKey = "$sources/cantilever.yaml"
    const val fragments = "$generated/htmlFragments/"
    const val generatedImagesPrefix = "$generated/images/"
    const val thumbnail = "__thumb"

    @Deprecated("Use metadataKey instead")
    const val postsKey = "$generated/posts.json"

    @Deprecated("Use metadataKey instead")
    const val pagesKey = "$generated/pages.json"

    @Deprecated("Use metadataKey instead")
    const val templatesKey = "$generated/templates.json"
    const val defaultPostTemplateKey = "$templatesPrefix/post.hbs"
    const val metadataKey = "$generated/metadata.json"
}

/**
 * Certain fixed files with constant names
 */
object FILES {
    const val INDEX_MD = "index.md"
    const val INDEX_HTML = "index.html"
}

/**
 * Standard input file types
 */
object FILE_TYPE {
    const val MD = "md"
    const val HTML_HBS = "html.hbs"
    const val HTML = "html"
    const val YAML = "yaml"
    const val CSS = "css"
}

/**
 * The core types of object that Cantilever can process
 */
enum class SOURCE_TYPE(val folder: String) {
    Pages("pages"),
    Posts("posts"),
    Templates("templates"),
    Statics("statics"),
    Images("images"),
    Root("");

    object SourceHelper {
        /**
         * Return a [SOURCE_TYPE] for the given folder name
         * @param folderName should be "posts", "pages", "templates" or "statics"
         */
        fun fromFolderName(folderName: String): SOURCE_TYPE =
            if(folderName.isBlank()) {
                Root
            } else {
                entries.first { it.folder == folderName.lowercase() }
            }
    }
}

/**
 * AWS SQS queue names
 */
object QUEUE {
    const val MARKDOWN = "markdown_processing_queue"
    const val HANDLEBARS = "handlebar_template_queue"
    const val IMAGES = "image_processing_queue"
}