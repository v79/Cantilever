# Cantilever S3 to DynamoDB Migration Guide

## Overview

This guide explains the migration of Cantilever's metadata storage from S3 JSON files to AWS DynamoDB. This migration addresses reliability issues with the previous JSON cache approach and provides a more scalable and robust solution for storing metadata.

## Changes Made

### 1. DynamoDB Schema

We've implemented a single-table design with the following structure:

- **Table Name**: `cantilever-content`
- **Primary Key**: 
  - Partition Key: `domainId` (String) - The project domain identifier
  - Sort Key: `entityType#entityId` (String) - Composite key with entity type prefix and ID

- **Global Secondary Indexes**:
  - **TemplateIndex**: For finding all pages/posts using a specific template
  - **ParentChildIndex**: For finding all children of a folder
  - **DateIndex**: For sorting posts by date for navigation

### 2. New Components

- **DynamoDBService**: A service interface and implementation for interacting with DynamoDB
- **ContentRepository**: A repository interface and implementation for managing content entities
- **ContentRepositoryFactory**: A factory for creating ContentRepository instances

### 3. Updated Components

- **APIController**: Updated to use the ContentRepository for loading and saving the ContentTree
- **CantileverStack**: Updated to create the DynamoDB table and grant permissions to Lambda functions

### 4. Migration Strategy

The implementation includes an automatic migration path:

1. When loading the ContentTree, it first tries to load from DynamoDB
2. If DynamoDB is empty, it falls back to loading from S3
3. If data is loaded from S3, it's automatically migrated to DynamoDB
4. Data continues to be saved to both DynamoDB and S3 for backward compatibility

## Benefits of Using DynamoDB

1. **Improved Reliability**: DynamoDB is a fully managed, highly available database service, eliminating the reliability issues with the JSON cache approach.

2. **Better Performance**: DynamoDB provides consistent, single-digit millisecond response times, making content operations faster.

3. **Scalability**: DynamoDB automatically scales to handle increased load, supporting projects of any size.

4. **Transactional Operations**: DynamoDB supports atomic transactions, ensuring data consistency.

5. **Fine-grained Access Control**: DynamoDB's IAM integration provides more granular control over who can access and modify content.

6. **Reduced Complexity**: Eliminates the need to manage complex JSON file operations and handle concurrency issues.

## Migration Instructions for Existing Projects

### Automatic Migration

For most users, migration will happen automatically:

1. Deploy the updated application
2. The first time content is loaded, it will be migrated from S3 to DynamoDB
3. No manual steps required

### Manual Migration (if needed)

If you need to manually migrate data:

1. Deploy the updated application
2. Use the AWS CLI to copy your metadata.json file locally:
   ```
   aws s3 cp s3://your-generation-bucket/your-domain/metadata.json metadata.json
   ```
3. Use the AWS CLI to load the data into DynamoDB:
   ```
   aws dynamodb put-item --table-name cantilever-content --item file://metadata.json
   ```

## Troubleshooting

If you encounter issues during migration:

1. Check CloudWatch logs for error messages
2. Verify that Lambda functions have the correct permissions to access DynamoDB
3. Ensure the DynamoDB table exists and has the correct schema
4. If automatic migration fails, try the manual migration steps

## Reverting to S3 (if necessary)

The application continues to save data to S3 for backward compatibility. If you need to revert to using S3 only:

1. Remove the DynamoDB table from your CantileverStack
2. Remove the ContentRepository-related code from your application
3. The application will continue to work with S3 as before

## Future Enhancements

Future versions may include:

1. Performance optimizations for DynamoDB queries
2. Additional indexes for more complex queries
3. Removal of S3 backward compatibility once migration is complete
4. Enhanced backup and restore capabilities

## Feedback

If you encounter any issues or have suggestions for improvements, please open an issue on the GitHub repository.