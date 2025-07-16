package org.liamjd.cantilever.services.impl

import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import org.liamjd.cantilever.services.DynamoDBService
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.*
import java.time.Instant as JavaInstant

class DynamoDBServiceImpl(region: Region) : DynamoDBService {

    override val dynamoDbClient: DynamoDbClient = DynamoDbClient.builder().region(region).build()

    override fun getItem(
        tableName: String,
        partitionKey: String,
        partitionValue: String,
        sortKey: String,
        sortValue: String
    ): Map<String, AttributeValue>? {
        val request = GetItemRequest.builder()
            .tableName(tableName)
            .key(
                mapOf(
                    partitionKey to stringValue(partitionValue),
                    sortKey to stringValue(sortValue)
                )
            )
            .build()
        val response = dynamoDbClient.getItem(request)
        return if (response.hasItem()) response.item() else null
    }

    override fun putItem(tableName: String, item: Map<String, AttributeValue>): Boolean {
        val request = PutItemRequest.builder()
            .tableName(tableName)
            .item(item)
            .build()
        val response = dynamoDbClient.putItem(request)
        return response.sdkHttpResponse().isSuccessful
    }

    override fun deleteItem(
        tableName: String,
        partitionKey: String,
        partitionValue: String,
        sortKey: String,
        sortValue: String
    ): Boolean {
        val request = DeleteItemRequest.builder()
            .tableName(tableName)
            .key(
                mapOf(
                    partitionKey to stringValue(partitionValue),
                    sortKey to stringValue(sortValue)
                )
            )
            .build()
        val response = dynamoDbClient.deleteItem(request)
        return response.sdkHttpResponse().isSuccessful
    }

    override fun queryItems(
        tableName: String,
        partitionKey: String,
        partitionValue: String,
        sortKeyCondition: String?,
        indexName: String?
    ): QueryResponse {
        val builder = QueryRequest.builder()
            .tableName(tableName)
            .keyConditionExpression("$partitionKey = :partitionValue")
            .expressionAttributeValues(
                mapOf(
                    ":partitionValue" to stringValue(partitionValue)
                )
            )

        sortKeyCondition?.let {
            builder.keyConditionExpression("$partitionKey = :partitionValue AND $it")
        }

        indexName?.let {
            builder.indexName(it)
        }

        return dynamoDbClient.query(builder.build())
    }

    override fun stringValue(value: String): AttributeValue {
        return AttributeValue.builder().s(value).build()
    }

    override fun numberValue(value: Number): AttributeValue {
        return AttributeValue.builder().n(value.toString()).build()
    }

    override fun booleanValue(value: Boolean): AttributeValue {
        return AttributeValue.builder().bool(value).build()
    }

    override fun instantValue(value: Instant): AttributeValue {
        return AttributeValue.builder().s(value.toString()).build()
    }

    override fun stringListValue(values: List<String>): AttributeValue {
        return AttributeValue.builder().ss(values).build()
    }

    override fun mapValue(values: Map<String, String>): AttributeValue {
        val attributeValues = values.mapValues { stringValue(it.value) }
        return AttributeValue.builder().m(attributeValues).build()
    }

    override fun mapAttributeValue(values: Map<String, AttributeValue>): AttributeValue {
        return AttributeValue.builder().m(values).build()
    }

    override fun getString(attributeValue: AttributeValue): String {
        return attributeValue.s()
    }

    override fun getNumber(attributeValue: AttributeValue): Number {
        return attributeValue.n().toDouble()
    }

    override fun getBoolean(attributeValue: AttributeValue): Boolean {
        return attributeValue.bool()
    }

    override fun getInstant(attributeValue: AttributeValue): Instant {
        return JavaInstant.parse(attributeValue.s()).toKotlinInstant()
    }

    override fun getStringList(attributeValue: AttributeValue): List<String> {
        return attributeValue.ss()
    }

    override fun getStringMap(attributeValue: AttributeValue): Map<String, String> {
        return attributeValue.m().mapValues { getString(it.value) }
    }

    override fun getAttributeValueMap(attributeValue: AttributeValue): Map<String, AttributeValue> {
        return attributeValue.m()
    }
}