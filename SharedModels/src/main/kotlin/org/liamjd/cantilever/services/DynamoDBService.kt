package org.liamjd.cantilever.services

import kotlinx.datetime.Instant
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.QueryResponse

/**
 * Wrapper around AWS DynamoDB to simplify interactions with DynamoDB
 */
interface DynamoDBService {

    val dynamoDbClient: DynamoDbClient

    /**
     * Get an item from DynamoDB
     * @param tableName the DynamoDB table name
     * @param partitionKey the partition key name
     * @param partitionValue the partition key value
     * @param sortKey the sort key name
     * @param sortValue the sort key value
     * @return the item as a Map of attribute name to AttributeValue, or null if not found
     */
    fun getItem(
        tableName: String,
        partitionKey: String,
        partitionValue: String,
        sortKey: String,
        sortValue: String
    ): Map<String, AttributeValue>?

    /**
     * Put an item into DynamoDB
     * @param tableName the DynamoDB table name
     * @param item the item to put, as a Map of attribute name to AttributeValue
     * @return true if successful, false otherwise
     */
    fun putItem(tableName: String, item: Map<String, AttributeValue>): Boolean

    /**
     * Delete an item from DynamoDB
     * @param tableName the DynamoDB table name
     * @param partitionKey the partition key name
     * @param partitionValue the partition key value
     * @param sortKey the sort key name
     * @param sortValue the sort key value
     * @return true if successful, false otherwise
     */
    fun deleteItem(
        tableName: String,
        partitionKey: String,
        partitionValue: String,
        sortKey: String,
        sortValue: String
    ): Boolean

    /**
     * Query items from DynamoDB
     * @param tableName the DynamoDB table name
     * @param partitionKey the partition key name
     * @param partitionValue the partition key value
     * @param sortKeyCondition optional sort key condition
     * @param indexName optional index name
     * @return the query response
     */
    fun queryItems(
        tableName: String,
        partitionKey: String,
        partitionValue: String,
        sortKeyCondition: String? = null,
        indexName: String? = null
    ): QueryResponse

    /**
     * Convert a string to an AttributeValue
     * @param value the string value
     * @return the AttributeValue
     */
    fun stringValue(value: String): AttributeValue

    /**
     * Convert a number to an AttributeValue
     * @param value the number value
     * @return the AttributeValue
     */
    fun numberValue(value: Number): AttributeValue

    /**
     * Convert a boolean to an AttributeValue
     * @param value the boolean value
     * @return the AttributeValue
     */
    fun booleanValue(value: Boolean): AttributeValue

    /**
     * Convert an Instant to an AttributeValue
     * @param value the Instant value
     * @return the AttributeValue
     */
    fun instantValue(value: Instant): AttributeValue

    /**
     * Convert a list of strings to an AttributeValue
     * @param values the list of strings
     * @return the AttributeValue
     */
    fun stringListValue(values: List<String>): AttributeValue

    /**
     * Convert a map of string to string to an AttributeValue
     * @param values the map of string to string
     * @return the AttributeValue
     */
    fun mapValue(values: Map<String, String>): AttributeValue

    /**
     * Convert a map of string to AttributeValue to an AttributeValue
     * @param values the map of string to AttributeValue
     * @return the AttributeValue
     */
    fun mapAttributeValue(values: Map<String, AttributeValue>): AttributeValue

    /**
     * Get a string from an AttributeValue
     * @param attributeValue the AttributeValue
     * @return the string value
     */
    fun getString(attributeValue: AttributeValue): String

    /**
     * Get a number from an AttributeValue
     * @param attributeValue the AttributeValue
     * @return the number value
     */
    fun getNumber(attributeValue: AttributeValue): Number

    /**
     * Get a boolean from an AttributeValue
     * @param attributeValue the AttributeValue
     * @return the boolean value
     */
    fun getBoolean(attributeValue: AttributeValue): Boolean

    /**
     * Get an Instant from an AttributeValue
     * @param attributeValue the AttributeValue
     * @return the Instant value
     */
    fun getInstant(attributeValue: AttributeValue): Instant

    /**
     * Get a list of strings from an AttributeValue
     * @param attributeValue the AttributeValue
     * @return the list of strings
     */
    fun getStringList(attributeValue: AttributeValue): List<String>

    /**
     * Get a map of string to string from an AttributeValue
     * @param attributeValue the AttributeValue
     * @return the map of string to string
     */
    fun getStringMap(attributeValue: AttributeValue): Map<String, String>

    /**
     * Get a map of string to AttributeValue from an AttributeValue
     * @param attributeValue the AttributeValue
     * @return the map of string to AttributeValue
     */
    fun getAttributeValueMap(attributeValue: AttributeValue): Map<String, AttributeValue>
}