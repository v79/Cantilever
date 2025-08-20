package org.liamjd.cantilever.services

import org.liamjd.cantilever.models.ContentNode

/**
 * Result of a DynamoDB operation
 * @property OK for any successful operation that returns no data
 * @property Node for any operation that returns a single [ContentNode]
 * @property Nodes for any operation that returns a list of [ContentNode]
 * @property Data for any operation that returns bespoke data
 * @property Error for any operation that returns an error message
 */
sealed class DynamoDBResult {
    object OK : DynamoDBResult()
    data class Node(val node: ContentNode) : DynamoDBResult()
    data class Nodes(val nodes: List<ContentNode>) : DynamoDBResult()
    data class Data(val data: Any) : DynamoDBResult()
    data class Error(val message: String) : DynamoDBResult()
}