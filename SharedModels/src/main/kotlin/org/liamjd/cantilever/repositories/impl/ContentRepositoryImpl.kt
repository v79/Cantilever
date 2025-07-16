package org.liamjd.cantilever.repositories.impl

import kotlinx.datetime.Instant
import org.liamjd.cantilever.models.ContentNode
import org.liamjd.cantilever.models.ContentTree
import org.liamjd.cantilever.repositories.ContentRepository
import org.liamjd.cantilever.services.DynamoDBService
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

/**
 * Implementation of ContentRepository that uses DynamoDB
 */
class ContentRepositoryImpl(
    private val dynamoDBService: DynamoDBService,
    override val tableName: String
) : ContentRepository {

    // Constants for DynamoDB table
    companion object {
        const val PARTITION_KEY = "domainId"
        const val SORT_KEY = "entityType#entityId"
        const val ENTITY_TYPE_PAGE = "PAGE"
        const val ENTITY_TYPE_POST = "POST"
        const val ENTITY_TYPE_FOLDER = "FOLDER"
        const val ENTITY_TYPE_TEMPLATE = "TEMPLATE"
        const val ENTITY_TYPE_STATIC = "STATIC"
        const val ENTITY_TYPE_IMAGE = "IMAGE"

        // GSI names
        const val TEMPLATE_INDEX = "TemplateIndex"
        const val PARENT_CHILD_INDEX = "ParentChildIndex"
        const val DATE_INDEX = "DateIndex"

        // Attribute names
        const val ATTR_TEMPLATE_KEY = "templateKey"
        const val ATTR_PARENT_KEY = "parentKey"
        const val ATTR_DATE = "date"
        const val ATTR_LAST_UPDATED = "lastUpdated"
        const val ATTR_URL = "url"
        const val ATTR_TITLE = "title"
        const val ATTR_SLUG = "slug"
        const val ATTR_IS_ROOT = "isRoot"
        const val ATTR_ATTRIBUTES = "attributes"
        const val ATTR_SECTIONS = "sections"
        const val ATTR_CHILDREN = "children"
        const val ATTR_INDEX_PAGE = "indexPage"
        const val ATTR_PREV_KEY = "prevKey"
        const val ATTR_NEXT_KEY = "nextKey"
    }

    override fun getContentTree(domainId: String): ContentTree {
        val contentTree = ContentTree()

        // Query all items for the domain
        val response = dynamoDBService.queryItems(
            tableName = tableName,
            partitionKey = PARTITION_KEY,
            partitionValue = domainId
        )

        // Process each item and add to the appropriate collection in ContentTree
        response.items().forEach { item ->
            val sortKeyValue = dynamoDBService.getString(item[SORT_KEY]!!)
            val entityType = sortKeyValue.substringBefore("#")

            when (entityType) {
                ENTITY_TYPE_PAGE -> {
                    val page = mapToPageNode(item)
                    contentTree.items.add(page)
                }

                ENTITY_TYPE_POST -> {
                    val post = mapToPostNode(item)
                    contentTree.items.add(post)
                }

                ENTITY_TYPE_FOLDER -> {
                    val folder = mapToFolderNode(item)
                    contentTree.items.add(folder)
                }

                ENTITY_TYPE_TEMPLATE -> {
                    val template = mapToTemplateNode(item)
                    contentTree.templates.add(template)
                }

                ENTITY_TYPE_STATIC -> {
                    val static = mapToStaticNode(item)
                    contentTree.statics.add(static)
                }

                ENTITY_TYPE_IMAGE -> {
                    val image = mapToImageNode(item)
                    contentTree.images.add(image)
                }
            }
        }

        return contentTree
    }

    override fun saveContentTree(domainId: String, contentTree: ContentTree): Boolean {
        var success = true

        // Save all items in the ContentTree
        contentTree.items.forEach { node ->
            when (node) {
                is ContentNode.PageNode -> {
                    success = success && savePage(domainId, node)
                }

                is ContentNode.PostNode -> {
                    success = success && savePost(domainId, node)
                }

                is ContentNode.FolderNode -> {
                    success = success && saveFolder(domainId, node)
                }

                is ContentNode.TemplateNode -> {
                    success = success && saveTemplate(domainId, node)
                }

                is ContentNode.StaticNode -> {
                    success = success && saveStatic(domainId, node)
                }

                is ContentNode.ImageNode -> {
                    success = success && saveImage(domainId, node)
                }
            }
        }

        // Save all templates
        contentTree.templates.forEach { template ->
            success = success && saveTemplate(domainId, template)
        }

        // Save all statics
        contentTree.statics.forEach { static ->
            success = success && saveStatic(domainId, static)
        }

        // Save all images
        contentTree.images.forEach { image ->
            success = success && saveImage(domainId, image)
        }

        return success
    }

    override fun getPage(domainId: String, pageId: String): ContentNode.PageNode? {
        println("Fetching page with ID $pageId in domain $domainId")
        val sortKeyValue = "$ENTITY_TYPE_PAGE#$pageId"
        val item = dynamoDBService.getItem(
            tableName = tableName,
            partitionKey = PARTITION_KEY,
            partitionValue = domainId,
            sortKey = SORT_KEY,
            sortValue = sortKeyValue
        ) ?: return null

        return mapToPageNode(item)
    }

    override fun savePage(domainId: String, page: ContentNode.PageNode): Boolean {
        println("Saving page with ID $page")
        val item = mapFromPageNode(domainId, page)
        return dynamoDBService.putItem(tableName, item)
    }

    override fun deletePage(domainId: String, pageId: String): Boolean {
        println("Deleting page with ID $pageId in domain $domainId")
        val sortKeyValue = "$ENTITY_TYPE_PAGE#$pageId"
        return dynamoDBService.deleteItem(
            tableName = tableName,
            partitionKey = PARTITION_KEY,
            partitionValue = domainId,
            sortKey = SORT_KEY,
            sortValue = sortKeyValue
        )
    }

    override fun getPost(domainId: String, postId: String): ContentNode.PostNode? {
        println("Fetching post with ID $postId in domain $domainId")
        val sortKeyValue = "$ENTITY_TYPE_POST#$postId"
        val item = dynamoDBService.getItem(
            tableName = tableName,
            partitionKey = PARTITION_KEY,
            partitionValue = domainId,
            sortKey = SORT_KEY,
            sortValue = sortKeyValue
        ) ?: return null

        return mapToPostNode(item)
    }

    override fun savePost(domainId: String, post: ContentNode.PostNode): Boolean {
        println("Saving post with ID ${post.srcKey} in domain $domainId")
        val item = mapFromPostNode(domainId, post)
        return dynamoDBService.putItem(tableName, item)
    }

    override fun deletePost(domainId: String, postId: String): Boolean {
        println("Deleting post with ID $postId in domain $domainId")
        val sortKeyValue = "$ENTITY_TYPE_POST#$postId"
        return dynamoDBService.deleteItem(
            tableName = tableName,
            partitionKey = PARTITION_KEY,
            partitionValue = domainId,
            sortKey = SORT_KEY,
            sortValue = sortKeyValue
        )
    }

    override fun getFolder(domainId: String, folderId: String): ContentNode.FolderNode? {
        println("Fetching folder with ID $folderId in domain $domainId")
        val sortKeyValue = "$ENTITY_TYPE_FOLDER#$folderId"
        val item = dynamoDBService.getItem(
            tableName = tableName,
            partitionKey = PARTITION_KEY,
            partitionValue = domainId,
            sortKey = SORT_KEY,
            sortValue = sortKeyValue
        ) ?: return null

        return mapToFolderNode(item)
    }

    override fun saveFolder(domainId: String, folder: ContentNode.FolderNode): Boolean {
        println("Saving folder with ID ${folder.srcKey} in domain $domainId")
        val item = mapFromFolderNode(domainId, folder)
        return dynamoDBService.putItem(tableName, item)
    }

    override fun deleteFolder(domainId: String, folderId: String): Boolean {
        println("Deleting folder with ID $folderId in domain $domainId")
        val sortKeyValue = "$ENTITY_TYPE_FOLDER#$folderId"
        return dynamoDBService.deleteItem(
            tableName = tableName,
            partitionKey = PARTITION_KEY,
            partitionValue = domainId,
            sortKey = SORT_KEY,
            sortValue = sortKeyValue
        )
    }

    override fun getTemplate(domainId: String, templateId: String): ContentNode.TemplateNode? {
        println("Fetching template with ID $templateId in domain $domainId")
        val sortKeyValue = "$ENTITY_TYPE_TEMPLATE#$templateId"
        val item = dynamoDBService.getItem(
            tableName = tableName,
            partitionKey = PARTITION_KEY,
            partitionValue = domainId,
            sortKey = SORT_KEY,
            sortValue = sortKeyValue
        ) ?: return null

        return mapToTemplateNode(item)
    }

    override fun saveTemplate(domainId: String, template: ContentNode.TemplateNode): Boolean {
        println("Saving template with ID ${template.srcKey} in domain $domainId")
        val item = mapFromTemplateNode(domainId, template)
        return dynamoDBService.putItem(tableName, item)
    }

    override fun deleteTemplate(domainId: String, templateId: String): Boolean {
        println("Deleting template with ID $templateId in domain $domainId")
        val sortKeyValue = "$ENTITY_TYPE_TEMPLATE#$templateId"
        return dynamoDBService.deleteItem(
            tableName = tableName,
            partitionKey = PARTITION_KEY,
            partitionValue = domainId,
            sortKey = SORT_KEY,
            sortValue = sortKeyValue
        )
    }

    override fun getPagesForTemplate(domainId: String, templateKey: String): List<ContentNode.PageNode> {
        println("Fetching pages for template $templateKey in domain $domainId")
        val response = dynamoDBService.queryItems(
            tableName = tableName,
            partitionKey = PARTITION_KEY,
            partitionValue = domainId,
            sortKeyCondition = "$ATTR_TEMPLATE_KEY = :templateKey",
            indexName = TEMPLATE_INDEX
        )

        return response.items()
            .filter { dynamoDBService.getString(it[SORT_KEY]!!).startsWith(ENTITY_TYPE_PAGE) }
            .map { mapToPageNode(it) }
    }

    override fun getPostsForTemplate(domainId: String, templateKey: String): List<ContentNode.PostNode> {
        println("Fetching posts for template $templateKey in domain $domainId")
        val response = dynamoDBService.queryItems(
            tableName = tableName,
            partitionKey = PARTITION_KEY,
            partitionValue = domainId,
            sortKeyCondition = "$ATTR_TEMPLATE_KEY = :templateKey",
            indexName = TEMPLATE_INDEX
        )

        return response.items()
            .filter { dynamoDBService.getString(it[SORT_KEY]!!).startsWith(ENTITY_TYPE_POST) }
            .map { mapToPostNode(it) }
    }

    override fun getChildrenOfFolder(domainId: String, folderId: String): List<ContentNode> {
        println("Fetching children of folder $folderId in domain $domainId")
        val parentKey = "$ENTITY_TYPE_FOLDER#$folderId"
        val response = dynamoDBService.queryItems(
            tableName = tableName,
            partitionKey = ATTR_PARENT_KEY,
            partitionValue = parentKey,
            indexName = PARENT_CHILD_INDEX
        )

        return response.items().map { item ->
            val sortKeyValue = dynamoDBService.getString(item[SORT_KEY]!!)
            val entityType = sortKeyValue.substringBefore("#")

            when (entityType) {
                ENTITY_TYPE_PAGE -> mapToPageNode(item)
                ENTITY_TYPE_POST -> mapToPostNode(item)
                else -> throw IllegalStateException("Unexpected entity type: $entityType")
            }
        }
    }

    override fun getPostsInOrder(domainId: String, limit: Int, startDate: Instant?): List<ContentNode.PostNode> {
        println("Fetching posts in order for domain $domainId with limit $limit and startDate $startDate")
        val response = dynamoDBService.queryItems(
            tableName = tableName,
            partitionKey = PARTITION_KEY,
            partitionValue = domainId,
            sortKeyCondition = if (startDate != null) "$ATTR_DATE <= :startDate" else null,
            indexName = DATE_INDEX
        )

        return response.items()
            .filter { dynamoDBService.getString(it[SORT_KEY]!!).startsWith(ENTITY_TYPE_POST) }
            .map { mapToPostNode(it) }
            .sortedByDescending { it.date }
            .take(limit)
    }

    // Helper methods for saving other entity types

    private fun saveStatic(domainId: String, static: ContentNode.StaticNode): Boolean {
        val item = mapFromStaticNode(domainId, static)
        return dynamoDBService.putItem(tableName, item)
    }

    private fun saveImage(domainId: String, image: ContentNode.ImageNode): Boolean {
        val item = mapFromImageNode(domainId, image)
        return dynamoDBService.putItem(tableName, item)
    }

    // Mapping methods for converting between DynamoDB items and ContentNode objects

    private fun mapToPageNode(item: Map<String, AttributeValue>): ContentNode.PageNode {
        val sortKeyValue = dynamoDBService.getString(item[SORT_KEY]!!)
        val entityId = sortKeyValue.substringAfter("#")

        return ContentNode.PageNode(
            srcKey = entityId,
            lastUpdated = dynamoDBService.getInstant(item[ATTR_LAST_UPDATED]!!),
            title = dynamoDBService.getString(item[ATTR_TITLE]!!),
            templateKey = dynamoDBService.getString(item[ATTR_TEMPLATE_KEY]!!),
            slug = dynamoDBService.getString(item[ATTR_SLUG]!!),
            isRoot = dynamoDBService.getBoolean(item[ATTR_IS_ROOT]!!),
            attributes = dynamoDBService.getStringMap(item[ATTR_ATTRIBUTES]!!),
            sections = dynamoDBService.getStringMap(item[ATTR_SECTIONS]!!),
            parent = item[ATTR_PARENT_KEY]?.let { dynamoDBService.getString(it) } ?: ""
        )
    }

    private fun mapFromPageNode(domainId: String, page: ContentNode.PageNode): Map<String, AttributeValue> {
        val sortKeyValue = "$ENTITY_TYPE_PAGE#${page.srcKey}"

        val item = mutableMapOf(
            PARTITION_KEY to dynamoDBService.stringValue(domainId),
            SORT_KEY to dynamoDBService.stringValue(sortKeyValue),
            ATTR_LAST_UPDATED to dynamoDBService.instantValue(page.lastUpdated),
            ATTR_URL to dynamoDBService.stringValue(page.url),
            ATTR_TITLE to dynamoDBService.stringValue(page.title),
            ATTR_TEMPLATE_KEY to dynamoDBService.stringValue(page.templateKey),
            ATTR_SLUG to dynamoDBService.stringValue(page.slug),
            ATTR_IS_ROOT to dynamoDBService.booleanValue(page.isRoot),
            ATTR_ATTRIBUTES to dynamoDBService.mapValue(page.attributes),
            ATTR_SECTIONS to dynamoDBService.mapValue(page.sections)
        )

        if (page.parent.isNotEmpty()) {
            item[ATTR_PARENT_KEY] = dynamoDBService.stringValue(page.parent)
        }

        return item
    }

    private fun mapToPostNode(item: Map<String, AttributeValue>): ContentNode.PostNode {
        val sortKeyValue = dynamoDBService.getString(item[SORT_KEY]!!)
        val entityId = sortKeyValue.substringAfter("#")

        val post = ContentNode.PostNode(
            srcKey = entityId,
            title = dynamoDBService.getString(item[ATTR_TITLE]!!),
            templateKey = dynamoDBService.getString(item[ATTR_TEMPLATE_KEY]!!),
            date = dynamoDBService.getInstant(item[ATTR_DATE]!!).toString().substringBefore("T")
                .let { kotlinx.datetime.LocalDate.parse(it) },
            slug = dynamoDBService.getString(item[ATTR_SLUG]!!),
            attributes = dynamoDBService.getStringMap(item[ATTR_ATTRIBUTES]!!)
        )

        item[ATTR_PREV_KEY]?.let { post.prev = dynamoDBService.getString(it) }
        item[ATTR_NEXT_KEY]?.let { post.next = dynamoDBService.getString(it) }

        return post
    }

    private fun mapFromPostNode(domainId: String, post: ContentNode.PostNode): Map<String, AttributeValue> {
        val sortKeyValue = "$ENTITY_TYPE_POST#${post.srcKey}"

        val item = mutableMapOf(
            PARTITION_KEY to dynamoDBService.stringValue(domainId),
            SORT_KEY to dynamoDBService.stringValue(sortKeyValue),
            ATTR_LAST_UPDATED to dynamoDBService.instantValue(post.lastUpdated),
            ATTR_URL to dynamoDBService.stringValue(post.url),
            ATTR_TITLE to dynamoDBService.stringValue(post.title),
            ATTR_TEMPLATE_KEY to dynamoDBService.stringValue(post.templateKey),
            ATTR_DATE to dynamoDBService.stringValue(post.date.toString()),
            ATTR_SLUG to dynamoDBService.stringValue(post.slug),
            ATTR_ATTRIBUTES to dynamoDBService.mapValue(post.attributes)
        )

        post.prev?.let { item[ATTR_PREV_KEY] = dynamoDBService.stringValue(it) }
        post.next?.let { item[ATTR_NEXT_KEY] = dynamoDBService.stringValue(it) }

        return item
    }

    private fun mapToFolderNode(item: Map<String, AttributeValue>): ContentNode.FolderNode {
        val sortKeyValue = dynamoDBService.getString(item[SORT_KEY]!!)
        val entityId = sortKeyValue.substringAfter("#")

        val folder = ContentNode.FolderNode(
            srcKey = entityId,
            lastUpdated = dynamoDBService.getInstant(item[ATTR_LAST_UPDATED]!!)
        )

        item[ATTR_CHILDREN]?.let {
            folder.children.addAll(dynamoDBService.getStringList(it))
        }

        item[ATTR_INDEX_PAGE]?.let {
            folder.indexPage = dynamoDBService.getString(it)
        }

        return folder
    }

    private fun mapFromFolderNode(domainId: String, folder: ContentNode.FolderNode): Map<String, AttributeValue> {
        val sortKeyValue = "$ENTITY_TYPE_FOLDER#${folder.srcKey}"

        val item = mutableMapOf(
            PARTITION_KEY to dynamoDBService.stringValue(domainId),
            SORT_KEY to dynamoDBService.stringValue(sortKeyValue),
            ATTR_LAST_UPDATED to dynamoDBService.instantValue(folder.lastUpdated),
            ATTR_URL to dynamoDBService.stringValue(folder.url)
        )

        if (folder.children.isNotEmpty()) {
            item[ATTR_CHILDREN] = dynamoDBService.stringListValue(folder.children)
        }

        folder.indexPage?.let {
            item[ATTR_INDEX_PAGE] = dynamoDBService.stringValue(it)
        }

        return item
    }

    private fun mapToTemplateNode(item: Map<String, AttributeValue>): ContentNode.TemplateNode {
        val sortKeyValue = dynamoDBService.getString(item[SORT_KEY]!!)
        val entityId = sortKeyValue.substringAfter("#")

        // Extract sections if they exist
        val sections = item["sections"]?.let {
            dynamoDBService.getStringList(it)
        } ?: emptyList()

        // Create template with sections
        val template = ContentNode.TemplateNode(
            srcKey = entityId,
            lastUpdated = dynamoDBService.getInstant(item[ATTR_LAST_UPDATED]!!),
            title = dynamoDBService.getString(item[ATTR_TITLE]!!),
            sections = sections
        )

        return template
    }

    private fun mapFromTemplateNode(domainId: String, template: ContentNode.TemplateNode): Map<String, AttributeValue> {
        val sortKeyValue = "$ENTITY_TYPE_TEMPLATE#${template.srcKey}"

        val item = mutableMapOf(
            PARTITION_KEY to dynamoDBService.stringValue(domainId),
            SORT_KEY to dynamoDBService.stringValue(sortKeyValue),
            ATTR_LAST_UPDATED to dynamoDBService.instantValue(template.lastUpdated),
            ATTR_TITLE to dynamoDBService.stringValue(template.title)
        )

        if (template.sections.isNotEmpty()) {
            item["sections"] = dynamoDBService.stringListValue(template.sections)
        }

        return item
    }

    private fun mapToStaticNode(item: Map<String, AttributeValue>): ContentNode.StaticNode {
        val sortKeyValue = dynamoDBService.getString(item[SORT_KEY]!!)
        val entityId = sortKeyValue.substringAfter("#")

        val static = ContentNode.StaticNode(
            srcKey = entityId,
            lastUpdated = dynamoDBService.getInstant(item[ATTR_LAST_UPDATED]!!)
        )

        item["fileType"]?.let {
            static.fileType = dynamoDBService.getString(it)
        }

        return static
    }

    private fun mapFromStaticNode(domainId: String, static: ContentNode.StaticNode): Map<String, AttributeValue> {
        val sortKeyValue = "$ENTITY_TYPE_STATIC#${static.srcKey}"

        val item = mutableMapOf(
            PARTITION_KEY to dynamoDBService.stringValue(domainId),
            SORT_KEY to dynamoDBService.stringValue(sortKeyValue),
            ATTR_LAST_UPDATED to dynamoDBService.instantValue(static.lastUpdated),
            ATTR_URL to dynamoDBService.stringValue(static.url)
        )

        static.fileType?.let {
            item["fileType"] = dynamoDBService.stringValue(it)
        }

        return item
    }

    private fun mapToImageNode(item: Map<String, AttributeValue>): ContentNode.ImageNode {
        val sortKeyValue = dynamoDBService.getString(item[SORT_KEY]!!)
        val entityId = sortKeyValue.substringAfter("#")

        val image = ContentNode.ImageNode(
            srcKey = entityId,
            lastUpdated = dynamoDBService.getInstant(item[ATTR_LAST_UPDATED]!!)
        )

        item["contentType"]?.let {
            image.contentType = dynamoDBService.getString(it)
        }

        return image
    }

    private fun mapFromImageNode(domainId: String, image: ContentNode.ImageNode): Map<String, AttributeValue> {
        val sortKeyValue = "$ENTITY_TYPE_IMAGE#${image.srcKey}"

        val item = mutableMapOf(
            PARTITION_KEY to dynamoDBService.stringValue(domainId),
            SORT_KEY to dynamoDBService.stringValue(sortKeyValue),
            ATTR_LAST_UPDATED to dynamoDBService.instantValue(image.lastUpdated),
            ATTR_URL to dynamoDBService.stringValue(image.url)
        )

        image.contentType?.let {
            item["contentType"] = dynamoDBService.stringValue(it)
        }

        return item
    }
}