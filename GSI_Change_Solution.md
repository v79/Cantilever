# DynamoDB GSI Change Solution

## Problem

The deployment of the AWS stack failed with the following error:

```
CantileverStack | 19:12:35 | UPDATE_FAILED | AWS::DynamoDB::Table | cantilever-content (cantilevercontent7C18A304) 
Resource handler returned message: "Cannot update GSI's properties other than Provisioned Throughput and Contributor Insights Specification. 
You can create a new GSI with a different name." (RequestToken: 98060a1e-0f91-9b41-1824-e366bb2b9b71, HandlerErrorCode: InvalidRequest)
```

The issue was caused by attempting to change the sort key of the "DateIndex" GSI from "date" to "lastUpdated". AWS DynamoDB does not allow changing GSI properties other than Provisioned Throughput and Contributor Insights Specification.

## Solution

The solution involves:

1. Reverting the "DateIndex" GSI to use "date" as the sort key (for backward compatibility)
2. Creating a new GSI named "LastUpdatedIndex" with "lastUpdated" as the sort key
3. Updating the application code to use the new GSI name

### Changes Made

#### 1. In CantileverStack.kt:

```kotlin
// Reverted to original DateIndex with "date" as sort key
table.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
    .indexName("DateIndex")
    .partitionKey(Attribute.builder()
        .name("domainId")
        .type(AttributeType.STRING)
        .build())
    .sortKey(Attribute.builder()
        .name("date")
        .type(AttributeType.STRING)
        .build())
    .projectionType(ProjectionType.ALL)
    .build())
    
// Added new GSI with "lastUpdated" as sort key
table.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
    .indexName("LastUpdatedIndex")
    .partitionKey(Attribute.builder()
        .name("domainId")
        .type(AttributeType.STRING)
        .build())
    .sortKey(Attribute.builder()
        .name("lastUpdated")
        .type(AttributeType.STRING)
        .build())
    .projectionType(ProjectionType.ALL)
    .build())
```

#### 2. In ContentRepositoryImpl.kt:

Added a new constant for the LastUpdatedIndex:

```kotlin
// GSI names
const val TEMPLATE_INDEX = "TemplateIndex"
const val PARENT_CHILD_INDEX = "ParentChildIndex"
const val DATE_INDEX = "DateIndex"
const val LAST_UPDATED_INDEX = "LastUpdatedIndex"
```

Updated the getPostsInOrder function to use the new GSI:

```kotlin
override fun getPostsInOrder(domainId: String, limit: Int, startDate: Instant?): List<ContentNode.PostNode> {
    println("Fetching posts in order for domain $domainId with limit $limit and startDate $startDate")
    val response = dynamoDBService.queryItems(
        tableName = tableName,
        partitionKey = PARTITION_KEY,
        partitionValue = domainId,
        sortKeyCondition = if (startDate != null) "$ATTR_LAST_UPDATED <= :startDate" else null,
        indexName = LAST_UPDATED_INDEX
    )

    return response.items()
        .filter { dynamoDBService.getString(it[SORT_KEY]!!).startsWith(ENTITY_TYPE_POST) }
        .map { mapToPostNode(it) }
        .sortedByDescending { it.lastUpdated }
        .take(limit)
}
```

## Why This Works

This solution works because:

1. We're no longer trying to modify the existing DateIndex GSI, which is not allowed by AWS
2. We're creating a new GSI with a different name that uses the desired sort key
3. We're updating the application code to use the new GSI name

The original DateIndex GSI is kept for backward compatibility, but the application now uses the new LastUpdatedIndex GSI for querying by lastUpdated.

### AWS DynamoDB Constraints

According to AWS documentation and the error message we received:

> "Cannot update GSI's properties other than Provisioned Throughput and Contributor Insights Specification. You can create a new GSI with a different name."

AWS DynamoDB has the following constraints regarding GSI modifications:

1. You **cannot** change the following properties of an existing GSI:
   - Index name
   - Partition key
   - Sort key
   - Projection type or projected attributes

2. You **can** change the following properties of an existing GSI:
   - Provisioned throughput (read and write capacity)
   - Contributor Insights Specification

The error occurred because we were attempting to change the sort key of the DateIndex GSI from "date" to "lastUpdated", which is not allowed. Our solution follows AWS's recommendation to "create a new GSI with a different name" instead of trying to modify the existing one.

## Deployment Instructions

Deploy the updated stack using:

```
cdk deploy --context env=prod --all
```

The deployment should now succeed because we're creating a new GSI instead of trying to modify an existing one.