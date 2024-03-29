package org.liamjd.cantilever.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The pages tree will be composed of 'folders' (shared prefixes in S3) and actual Pages
 */
@Deprecated("Use ContentNode instead")
@Serializable
sealed class PageTreeNode {

    /**
     * FolderNode represents a 'folder' in S3, or more accurately a 'common prefix' as S3 does not have folders
     * @property srcKey an S3 object prefix common to one or more PageMeta items
     * @property children nested collection of other [PageTreeNode]
     */
    @Serializable
    @SerialName("folder")
    data class FolderNode(val nodeType:String = "folder", val srcKey: String, var children: MutableList<PageTreeNode>?, val isRoot: Boolean) : PageTreeNode() {
        @OptIn(ExperimentalSerializationApi::class)
        @EncodeDefault
        var count: Int = children?.size ?: 0
    }

    /**
     * PageMeta is a summary class representing an individual Page. It does not contain the content for each section; they are flattened to empty strings
     * @property nodeType needed in the front end, but defaults to "page" here.
     * @property title user-provided title
     * @property srcKey the full S3 key for the page, must be unique, in format 'sources/pages/folder/leafname.md'
     * @property templateKey the leaf of the S3 key for the template this page is based on (e.g. if the full template key is /sources/templates/myTemplate.hbs then this value will be 'myTemplate'
     * @property url the calculated url for this page, initially based on the title, can be overridden, and needs to reflect the folder structure the source file is stored in in S3
     * @property attributes a map of custom attributes, both key and value
     * @property sections a map of the custom sections in the page, but the value is simply stored as an empty string
     * @property lastUpdated internal property updated whenever the page is saved
     */
    @Serializable
    @SerialName("page")
    data class PageMeta(
        val nodeType: String = "page",
        val title: String,
        val srcKey: String,
        val templateKey: String,
        val url: String,            // generated url, in format '/folder/leafname' (no .html unless it's index.html)
        val attributes: Map<String, String>,
        val sections: Map<String, String>,
        val lastUpdated: Instant = Clock.System.now()
    ) : PageTreeNode()
}

/**
 * The parent class containing all the [PageTreeNode]s
 * @property lastUpdated generated date for pages.json
 * @property container the main folder that contains the tree
 */
@Deprecated("Use ContentTree instead")
@Serializable
class PageTree(val lastUpdated: Instant, val container: PageTreeNode.FolderNode)
