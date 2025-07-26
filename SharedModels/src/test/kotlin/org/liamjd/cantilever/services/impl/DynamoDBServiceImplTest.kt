package org.liamjd.cantilever.services.impl

import org.junit.jupiter.api.Test
import org.liamjd.cantilever.models.dynamodb.Project
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
    
    @Test
    fun `verify project model structure`() {
        // This test verifies that the Project model has the expected structure
        val project = Project(
            domain = "example.com",
            projectName = "test-project",
            author = "Test Author",
            dateFormat = "yyyy-MM-dd",
            dateTimeFormat = "yyyy-MM-dd HH:mm:ss"
        )

        assert(project.domain == "example.com")
        assert(project.projectName == "test-project")
        assert(project.author == "Test Author")
        assert(project.dateFormat == "yyyy-MM-dd")
        assert(project.dateTimeFormat == "yyyy-MM-dd HH:mm:ss")
        assert(project.srcKey == "example.com.yaml")
    }
}