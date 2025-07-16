package org.liamjd.cantilever.repositories

import org.liamjd.cantilever.repositories.impl.ContentRepositoryImpl
import org.liamjd.cantilever.services.DynamoDBService
import org.liamjd.cantilever.services.impl.DynamoDBServiceImpl
import software.amazon.awssdk.regions.Region

/**
 * Factory for creating ContentRepository instances
 */
object ContentRepositoryFactory {

    /**
     * Create a ContentRepository instance
     * @param region the AWS region
     * @param tableName the DynamoDB table name
     * @return a ContentRepository instance
     */
    fun createRepository(region: Region, tableName: String): ContentRepository {
        val dynamoDBService = DynamoDBServiceImpl(region)
        return ContentRepositoryImpl(dynamoDBService, tableName)
    }

    /**
     * Create a ContentRepository instance using environment variables
     * @return a ContentRepository instance
     */
    fun createRepositoryFromEnv(): ContentRepository {
        val region = Region.of(System.getenv("AWS_REGION") ?: "eu-west-2")
        val tableName = System.getenv("dynamodb_table") ?: "cantilever-content"
        return createRepository(region, tableName)
    }
}