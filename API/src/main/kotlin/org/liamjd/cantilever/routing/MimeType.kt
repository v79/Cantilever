package org.liamjd.cantilever.routing

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
        if(this == other) {
            true
        } else {
            type == other.type && (subType.contains("+")) && other.subType.contains("+")
        }


    companion object {

        val json = MimeType("application", "json")
        val html = MimeType("text", "html")
        val plainText = MimeType("text","plain")
        val yaml = MimeType("application","yaml")
        fun parse(s: String): MimeType {
            val parts = s.split('/')
            return MimeType(parts[0].lowercase(), parts[1].lowercase())
        }
    }
}