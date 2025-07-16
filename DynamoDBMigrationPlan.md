# Cantilever DynamoDB Migration Plan

## Overview

This document outlines the plan to migrate Cantilever from storing JSON and markdown files in S3 to using AWS DynamoDB for metadata storage. The actual content files (markdown, templates, images) will still be stored in S3, but the metadata and relationships currently stored in the JSON cache will be moved to DynamoDB.

## Current Implementation

Currently, Cantilever uses:
- S3 buckets for storing source files (markdown, templates), intermediate files, and the final website
- A JSON cache file (`metadata.json`) stored in S3 that contains the ContentTree
- The ContentTree tracks relationships between pages, posts, templates, and other content

## DynamoDB Schema Design

### Table Design

We'll use a single-table design pattern with appropriate partition and sort keys to efficiently support all required access patterns.

#### Primary Keys
- **Partition Key**: `domainId` - The project domain identifier
- **Sort Key**: `entityType#entityId` - Composite key with entity type prefix and ID

#### Global Secondary Indexes (GSIs)

1. **TemplateIndex**:
   - Partition Key: `domainId`
   - Sort Key: `templateKey`
   - Purpose: Find all pages/posts using a specific template

2. **ParentChildIndex**:
   - Partition Key: `parentKey`
   - Sort Key: `entityType#entityId`
   - Purpose: Find all children of a folder

3. **DateIndex** (for posts):
   - Partition Key: `domainId`
   - Sort Key: `date`
   - Purpose: Sort posts by date for navigation

### Item Structure Examples

#### Page Item
```json
{
  "domainId": "example.com",
  "entityType#entityId": "PAGE#pages/about",
  "lastUpdated": "2025-07-16T10:00:00Z",
  "url": "/about",
  "title": "About Us",
  "templateKey": "templates/page.html.hbs",
  "slug": "about",
  "isRoot": false,
  "attributes": { "key1": "value1", "key2": "value2" },
  "sections": { "body": "generated/fragments/about/body" },
  "parentKey": "FOLDER#pages"
}
```

#### Post Item
```json
{
  "domainId": "example.com",
  "entityType#entityId": "POST#posts/2023/07/hello-world",
  "lastUpdated": "2025-07-16T10:00:00Z",
  "url": "/posts/2023/07/hello-world",
  "title": "Hello World",
  "templateKey": "templates/post.html.hbs",
  "date": "2023-07-16",
  "slug": "hello-world",
  "attributes": { "key1": "value1", "key2": "value2" },
  "prevKey": "POST#posts/2023/06/previous-post",
  "nextKey": "POST#posts/2023/08/next-post"
}
```

#### Folder Item
```json
{
  "domainId": "example.com",
  "entityType#entityId": "FOLDER#pages",
  "lastUpdated": "2025-07-16T10:00:00Z",
  "url": "/pages",
  "children": ["PAGE#pages/about", "PAGE#pages/contact"],
  "indexPage": "PAGE#pages/index"
}
```

## Access Patterns

1. Get all content for a domain
   - Query by `domainId` with no sort key filter

2. Get a specific content node
   - Get item with `domainId` and `entityType#entityId`

3. Get all pages/posts using a specific template
   - Query TemplateIndex with `domainId` and `templateKey`

4. Get all children of a folder
   - Query ParentChildIndex with `parentKey`

5. Get posts in chronological order
   - Query DateIndex with `domainId` and sort by `date`

6. Get next/previous post
   - Get item with `domainId` and `nextKey`/`prevKey` from a post

## Implementation Steps

1. Create a DynamoDB service interface and implementation
2. Update the CDK stack to create the DynamoDB table and indexes
3. Create repository classes for each entity type
4. Update the application code to use the new DynamoDB repositories
5. Implement migration utilities to move data from S3 to DynamoDB