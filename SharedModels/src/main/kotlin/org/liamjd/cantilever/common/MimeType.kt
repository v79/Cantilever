package org.liamjd.cantilever.common

import kotlinx.serialization.Serializable

/**
 * Basic representation of a mime type like "text/html" or "application/json"
 */
@Serializable
data class MimeType(val type: String, val subType: String) {

    /**
     * Check if the given Accept header matches this mime type
     * @param acceptHeader the Accept header to check
     * @return true if the Accept header matches this mime type
     */
    fun matches(acceptHeader: String?): Boolean {
        return acceptHeader?.let { parse(it).type == type && parse(acceptHeader).subType == subType } ?: false
    }

    override fun toString() = "$type/$subType"

    // TODO : this is just scratching the surface of the requirements around mimetypes
    fun isCompatibleWith(other: MimeType): Boolean =
        if (this == other) {
            true
        } else {
            type == other.type && (subType.contains("+")) && other.subType.contains("+")
        }


    /** Constant definitions for the various mime types */
    companion object {
        // application
        val json = MimeType("application", "json")
        val yaml = MimeType("application", "yaml")
        val javascript = MimeType("application", "javascript")
        val pdf = MimeType("application", "pdf")
        val xml = MimeType("application", "xml")
        // text
        val html = MimeType("text", "html")
        val plainText = MimeType("text", "plain")
        val markdown = MimeType("text", "markdown")
        val css = MimeType("text", "css")
        // image
        val jpg = MimeType("image", "jpeg")
        val png = MimeType("image", "png")
        val gif = MimeType("image", "gif")
        val svg = MimeType("image", "svg+xml")
        val webp = MimeType("image", "webp")

        /**
         * Parse a string into a MimeType object
         * @param string the string to parse, e.g. "text/html"
         * @return a MimeType object
         */
        fun parse(string: String): MimeType {
            val parts = string.split('/')
            return MimeType(parts[0].lowercase(), parts[1].lowercase())
        }

        /**
         * Get a MimeType from a file extension
         * @param extension the file extension, e.g. "html"
         * @return a MimeType object, or application/octet-stream if the extension is not recognized
         */
        fun fromExtension(extension: String): MimeType = when (extension.lowercase()) {
            "html", "htm" -> html
            "md", "markdown" -> markdown
            "txt", "text" -> plainText
            "json" -> json
            "yaml", "yml" -> yaml
            "js" -> javascript
            "css" -> css
            "jpg", "jpeg" -> jpg
            "png" -> png
            "gif" -> gif
            "svg" -> svg
            "webp" -> webp
            "pdf" -> pdf
            "xml" -> xml
            else -> MimeType("application", "octet-stream")
        }
    }
}