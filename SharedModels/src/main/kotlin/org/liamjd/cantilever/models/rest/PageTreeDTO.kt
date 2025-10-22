package org.liamjd.cantilever.models.rest

import kotlinx.datetime.Instant
import org.liamjd.cantilever.models.ContentNode

/**
 * Data transfer object for the Page & Folder tree structure.
 */
class PageTreeDTO(val rootFolder: TreeNode.FolderNodeDTO) {

    fun buildTreeFromPagesAndFolders(folders: List<ContentNode.FolderNode>, pages: List<ContentNode.PageNode>) {
        // Create a map of DTO folder nodes
        val folderDTOMap = mutableMapOf<String, TreeNode.FolderNodeDTO>()

        // Initialize with the root folder
        folderDTOMap[rootFolder.srcKey] = rootFolder

        // Create DTO folder nodes for all folders
        folders.forEach { folderNode ->
            if (folderNode.srcKey != rootFolder.srcKey) {
                folderDTOMap[folderNode.srcKey] = TreeNode.FolderNodeDTO(folderNode.srcKey, folderNode.lastUpdated)
            }
        }

        // Build the folder hierarchy
        folders.forEach { folderNode ->
            if (folderNode.srcKey == rootFolder.srcKey) {
                return@forEach // Skip root folder as it's already the root of our tree
            }

            // Find the parent folder by removing the last segment from the srcKey
            val lastSlashIndex = folderNode.srcKey.lastIndexOf('/')
            if (lastSlashIndex > 0) {
                val parentSrcKey = folderNode.srcKey.take(lastSlashIndex)
                val parentFolderDTO = folderDTOMap[parentSrcKey]

                if (parentFolderDTO != null) {
                    // Add this folder as a child of its parent
                    val folderDTO = folderDTOMap[folderNode.srcKey]
                    if (folderDTO != null && !parentFolderDTO.children.contains(folderDTO)) {
                        parentFolderDTO.children.add(folderDTO)
                    }
                }
            }
        }

        // Create and add file nodes to their parent folders
        pages.forEach { pageNode ->
            val parentFolderKey = pageNode.parent.ifEmpty {
                // If parent is not set, extract it from the srcKey
                val lastSlashIndex = pageNode.srcKey.lastIndexOf('/')
                if (lastSlashIndex > 0) {
                    pageNode.srcKey.take(lastSlashIndex)
                } else {
                    rootFolder.srcKey // Default to root if no parent can be determined
                }
            }

            val parentFolderDTO = folderDTOMap[parentFolderKey]
            if (parentFolderDTO != null) {
                val fileNode = TreeNode.FileNodeDTO(
                    srcKey = pageNode.srcKey,
                    lastUpdated = pageNode.lastUpdated,
                    title = pageNode.title,
                    slug = pageNode.slug,
                    parentFolder = parentFolderKey,
                    isRoot = pageNode.isRoot,
                    templateKey = pageNode.templateKey
                )

                parentFolderDTO.children.add(fileNode)

                // If this is an index page (isRoot=true), set it as the index page of the folder
                if (pageNode.isRoot) {
                    parentFolderDTO.indexPageKey = pageNode.srcKey
                }
            }
        }
    }

    fun printTree(): String {
        val sb = StringBuilder()
        fun String.leaf(): String = this.substringAfterLast('/')

        fun renderFile(file: TreeNode.FileNodeDTO, prefix: String, isLast: Boolean) {
            val connector = if (isLast) "└── " else "├── "
            val rootMark = if (file.isRoot) " (index)" else ""
            sb.append(prefix)
                .append(connector)
                .append(file.srcKey)
                .append(rootMark)
                .append('\n')
        }

        fun renderFolder(folder: TreeNode.FolderNodeDTO, prefix: String, isLast: Boolean) {
            val connector = if (prefix.isEmpty()) "" else if (isLast) "└── " else "├── "
            val folderName = folder.srcKey.leaf()
            sb.append(prefix)
                .append(connector)
                .append(folderName)
                .append("/")
                .append('\n')

            val (folders, files) = folder.children.partition { it is TreeNode.FolderNodeDTO }

            val nextPrefix = when {
                prefix.isEmpty() -> ""
                isLast -> "$prefix    "
                else -> "$prefix│   "
            }

            // find pages for the current folder
            files.forEachIndexed { index, file ->
                renderFile(
                    file as TreeNode.FileNodeDTO,
                    nextPrefix,
                    index == folder.children.size - 1
                )
            }
            folders.forEach { renderFolder(it as TreeNode.FolderNodeDTO, nextPrefix, false) }
        }

        renderFolder(rootFolder, prefix = "", isLast = true)
        return sb.toString().trimEnd()
    }
}

/**
 * Represents a hierarchical structure of nodes, either as folders or files, within a content tree.
 *
 * @property srcKey The source key or unique identifier for the node within the content tree.
 * @property lastUpdated The timestamp indicating when the node was last updated.
 */
sealed class TreeNode(val srcKey: String, val lastUpdated: Instant) {

    /**
     * Represents a folder node within a content tree. A folder node can contain child nodes and may have a specific
     * index page associated with it.
     *
     * @constructor
     * @param srcKey The unique source key identifying this folder node in the content tree.
     * @param lastUpdated The timestamp indicating when this folder node was last updated.
     * @param children The list of child nodes contained within this folder node.
     * @param indexPageKey The optional source key of the index page associated with this folder, if one exists.
     *
     * @property count The total number of child nodes contained within this folder node.
     */
    class FolderNodeDTO(srcKey: String, lastUpdated: Instant) :
        TreeNode(srcKey, lastUpdated) {
        var children: MutableList<TreeNode> = mutableListOf()
        var indexPageKey: String? = null
        val count: Int
            get() = children.size
    }

    // May or may not need attributes and section details; leaving them out for now
    /**
     * Represents a file node within a content tree structure.
     *
     * A file node holds metadata about a specific file, including its title, URL slug,
     * and the folder it belongs to. It may also indicate whether it is a root node
     * and specifies the template used to render its content.
     *
     * @constructor
     * @param srcKey The unique identifier for this file node.
     * @param lastUpdated The timestamp of the last update to this file node.
     * @param title The display title of this file.
     * @param slug The slug used for generating the URL of this file.
     * @param parentFolder The identifier of the parent folder containing this file, or null if it does not have a parent folder.
     * @param isRoot Indicates whether this file node is the root of the content tree structure.
     * @param templateKey The identifier of the template used to render the file's content.
     */
    class FileNodeDTO(
        srcKey: String,
        lastUpdated: Instant,
        val title: String,
        val slug: String,
        val parentFolder: String?,
        var isRoot: Boolean,
        val templateKey: String
    ) : TreeNode(srcKey, lastUpdated)
}