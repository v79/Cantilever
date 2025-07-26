package org.liamjd.cantilever.services.impl

import org.junit.jupiter.api.Test
import software.amazon.awssdk.regions.Region

/**
 * Basic tests for DynamoDBServiceImpl
 * Note: These tests don't actually connect to DynamoDB, they just verify the class structure
 */
class DynamoDBServiceImplTest {

    @Test
    fun `verify service can be instantiated`() {
        // This test just verifies that the class can be instantiated without errors
        val service = DynamoDBServiceImpl(Region.EU_WEST_2, "test-table")
        assert(service is DynamoDBServiceImpl)
    }

}