package org.liamjd.cantilever.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class PageTreeNode(val isFolder: Boolean) {
    @Serializable
    @SerialName("folder")
    data class FolderNode(val srcKey: String, var children: MutableList<PageTreeNode>?) : PageTreeNode(true) {
        @OptIn(ExperimentalSerializationApi::class)
        @EncodeDefault
        var count: Int = children?.size ?: 0
    }

    @Serializable
    @SerialName("fakePage")
    data class FakePageNode(val srcKey: String, val title: String) : PageTreeNode(false)

    /**
     * PageMeta is a summary class representing an individual Page. It does not contain the content for each section; they are flattened to empty strings
     * @property title user-provided title
     * @property srcKey the full S3 key for the page, must be unique, in format 'sources/pages/folder/leafname.md'
     * @property templateKey the leaf of the S3 key for the template this page is based on (e.g. if the full template key is /sources/templates/myTemplate.hbs then this value will be 'myTemplate'
     * @property url the calculated url for this page, initially based on the title, can be overridden, and needs to reflect the folder structure the source file is stored in in S3
     * @property attributes a map of custom attributes, both key and value
     * @property sections a map of the custom sections in the page, but the value is simply stored as an empty string
     * @property lastUpdated internal property updated whenever the page is saved
     *
     *
     * How to represent a folder structure? Well, the S3 key simulates this with the / separator.
     * So a page with key /sources/pages/apple/pear/banana.md will be a file called "banana.md" in a "apple > pear" folder path.
     * I would need to be able to create a 'folder' from the web UI.
     * Would such folders be recorded in pages.json? Or just implied?
     * One problem with srcKey is that it starts at /sources/pages/ - these are not relevant to the URL generation or UI
     * But that can be skipped fairly easily.
     * URL generation needs to change. And the destination in S3 needs to better reflect the source key.
     *
     */
    @Serializable
    @SerialName("page")
    data class PageMeta(
        val title: String,
        val srcKey: String,
        val templateKey: String,
        val url: String,            // generated url, in format '/folder/leafname' (no .html unless it's index.html)
        val attributes: Map<String, String>,
        val sections: Map<String, String>,
        val lastUpdated: Instant = Clock.System.now()
    ) : PageTreeNode(false)
}

@Serializable
class PageTree(val lastUpdated: Instant, val root: PageTreeNode.FolderNode) {
}
