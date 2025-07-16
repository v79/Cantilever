package org.liamjd.cantilever.repositories

import kotlinx.datetime.Instant
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.ContentTree

/**
 * Repository for managing content entities in DynamoDB
 */
interface ContentRepository {

    /**
     * The name of the DynamoDB table
     */
    val tableName: String

    /**
     * Get all content for a domain
     * @param domainId the domain identifier
     * @return a ContentTree containing all content for the domain
     */
    fun getContentTree(domainId: String): ContentTree

    /**
     * Save a ContentTree to DynamoDB
     * @param domainId the domain identifier
     * @param contentTree the ContentTree to save
     * @return true if successful, false otherwise
     */
    fun saveContentTree(domainId: String, contentTree: ContentTree): Boolean

    /**
     * Get a specific page
     * @param domainId the domain identifier
     * @param pageId the page identifier
     * @return the page, or null if not found
     */
    fun getPage(domainId: String, pageId: String): ContentNode.PageNode?

    /**
     * Save a page
     * @param domainId the domain identifier
     * @param page the page to save
     * @return true if successful, false otherwise
     */
    fun savePage(domainId: String, page: ContentNode.PageNode): Boolean

    /**
     * Delete a page
     * @param domainId the domain identifier
     * @param pageId the page identifier
     * @return true if successful, false otherwise
     */
    fun deletePage(domainId: String, pageId: String): Boolean

    /**
     * Get a specific post
     * @param domainId the domain identifier
     * @param postId the post identifier
     * @return the post, or null if not found
     */
    fun getPost(domainId: String, postId: String): ContentNode.PostNode?

    /**
     * Save a post
     * @param domainId the domain identifier
     * @param post the post to save
     * @return true if successful, false otherwise
     */
    fun savePost(domainId: String, post: ContentNode.PostNode): Boolean

    /**
     * Delete a post
     * @param domainId the domain identifier
     * @param postId the post identifier
     * @return true if successful, false otherwise
     */
    fun deletePost(domainId: String, postId: String): Boolean

    /**
     * Get a specific folder
     * @param domainId the domain identifier
     * @param folderId the folder identifier
     * @return the folder, or null if not found
     */
    fun getFolder(domainId: String, folderId: String): ContentNode.FolderNode?

    /**
     * Save a folder
     * @param domainId the domain identifier
     * @param folder the folder to save
     * @return true if successful, false otherwise
     */
    fun saveFolder(domainId: String, folder: ContentNode.FolderNode): Boolean

    /**
     * Delete a folder
     * @param domainId the domain identifier
     * @param folderId the folder identifier
     * @return true if successful, false otherwise
     */
    fun deleteFolder(domainId: String, folderId: String): Boolean

    /**
     * Get a specific template
     * @param domainId the domain identifier
     * @param templateId the template identifier
     * @return the template, or null if not found
     */
    fun getTemplate(domainId: String, templateId: String): ContentNode.TemplateNode?

    /**
     * Save a template
     * @param domainId the domain identifier
     * @param template the template to save
     * @return true if successful, false otherwise
     */
    fun saveTemplate(domainId: String, template: ContentNode.TemplateNode): Boolean

    /**
     * Delete a template
     * @param domainId the domain identifier
     * @param templateId the template identifier
     * @return true if successful, false otherwise
     */
    fun deleteTemplate(domainId: String, templateId: String): Boolean

    /**
     * Get all pages for a template
     * @param domainId the domain identifier
     * @param templateKey the template key
     * @return a list of pages using the template
     */
    fun getPagesForTemplate(domainId: String, templateKey: String): List<ContentNode.PageNode>

    /**
     * Get all posts for a template
     * @param domainId the domain identifier
     * @param templateKey the template key
     * @return a list of posts using the template
     */
    fun getPostsForTemplate(domainId: String, templateKey: String): List<ContentNode.PostNode>

    /**
     * Get all children of a folder
     * @param domainId the domain identifier
     * @param folderId the folder identifier
     * @return a list of content nodes that are children of the folder
     */
    fun getChildrenOfFolder(domainId: String, folderId: String): List<ContentNode>

    /**
     * Get all posts in chronological order
     * @param domainId the domain identifier
     * @param limit the maximum number of posts to return
     * @param startDate the date to start from (optional)
     * @return a list of posts in chronological order
     */
    fun getPostsInOrder(domainId: String, limit: Int, startDate: Instant? = null): List<ContentNode.PostNode>
}