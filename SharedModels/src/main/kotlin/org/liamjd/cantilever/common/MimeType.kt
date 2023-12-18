package org.liamjd.cantilever.common

/**
 * Basic representation of a mime type like "text/html" or "application/json"
 */
data class MimeType(val type: String, val subType: String) {
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


    companion object {

        val json = MimeType("application", "json")
        val html = MimeType("text", "html")
        val plainText = MimeType("text", "plain")
        val yaml = MimeType("application", "yaml")
        val jpg = MimeType("image", "jpg")
        val png = MimeType("image", "png")
        val gif = MimeType("image", "gif")
        val svg = MimeType("image", "svg+xml")
        val webp = MimeType("image", "webp")
        val css = MimeType("text", "css")
        val pdf = MimeType("application", "pdf")
        val javascript = MimeType("application", "javascript")
        val xml = MimeType("application", "xml")

        fun parse(s: String): MimeType {
            val parts = s.split('/')
            return MimeType(parts[0].lowercase(), parts[1].lowercase())
        }
    }
}