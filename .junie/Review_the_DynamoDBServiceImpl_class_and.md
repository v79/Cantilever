### Overall impressions
- The service covers project and content-node CRUD plus several query helpers. The public surface is reasonable, but the class is quite large (nearly 1k lines) and mixes:
  - Low-level DynamoDB request construction
  - Error handling
  - Domain mapping for multiple node types
  - Query utilities with varying patterns

This makes the class harder to maintain and test. There are also some correctness issues and inconsistencies.

---

### High-impact issues and bugs
1. Timestamp parsing bug
   - createLastUpdatedAttribute stores milliseconds: `"$type#${System.currentTimeMillis()}"`.
   - extractLastUpdatedTimestamp parses the second segment and then calls `Instant.fromEpochSeconds(timestamp)`. This interprets milliseconds as seconds.
   - Impact: lastUpdated on nodes will be incorrect by a factor of 1000.
   - Fix: use `Instant.fromEpochMilliseconds(timestamp)`.

2. getKeyListFromLSI builds a dynamic placeholder in the expression but hard-codes a different placeholder in the values
   - Expression uses `:\${attribute.first}` but values map builds `":date"` regardless of `attribute.first`.
   - Tests pass only because they use `attribute.first == "date"`.
   - Fix: use the same dynamic key for the values map, e.g.:
     ```kotlin
     val valuePlaceholder = ":${attribute.first}"
     .expressionAttributeValues(
         mapOf(
             ":domainType" to AttributeValue.builder().s("$projectDomain#${contentType.dbType}").build(),
             valuePlaceholder to AttributeValue.builder().s(attribute.second).build()
         )
     )
     ```

3. getContentNode omits Pages
   - The `when` only covers Templates, Posts, Statics, Folders. Pages are supported elsewhere (upsert, listAllNodesForProject).
   - Fix: add `SOURCE_TYPE.Pages -> mapToPageNode(response.item())` to getContentNode.

4. Possible inverted ordering flag in getKeyListMatchingAttributes
   - The comment says `descending` default is true. In DynamoDB, `scanIndexForward(true)` = ascending. The code currently passes `scanIndexForward(descending)`, meaning `true` -> ascending, which contradicts the default documentation.
   - Fix: use `.scanIndexForward(!descending)` like you do in getKeyListFromLSI, or clarify naming.

5. Overwriting type#lastUpdated for static/image nodes
   - At the start of upsertContentNode you set `type#lastUpdated` using the contentType: `createLastUpdatedAttribute(contentType.dbType)`.
   - In the branch for `is ContentNode.StaticNode, is ContentNode.ImageNode`, you then set it again to `createLastUpdatedAttribute("static")`, overriding the first value, and for images this is likely wrong.
   - Fix: don’t override; the initial line is sufficient and consistent. If you intended a different type prefix, choose it explicitly per content type.

6. Test data bug: duplicate srcKey for post2 and post3 in tests
   - In tests `post2.srcKey` and `post3.srcKey` are both set to `"posts/2025/08/non-matching-post.md"`. This will overwrite the previous item or cause confusion in expectations.
   - Fix: give post3 a distinct key (e.g., `another-non-matching-post.md`).

7. listAllProjects scan uses `contains(domain#type, "#project")`
   - This is functional but semantically odd; value is of the form `domain#project`, so contains("#project") matches, but `begins_with` or a GSI designed for projects would be clearer and more efficient.

---

### Inconsistencies and design issues
- Error handling duplication:
  - Some methods use the `executeDynamoOperation` wrapper (getProject, getNodeCount, listAllNodesForProject, key-list methods), others re-implement try/catch (saveProject, deleteProject, upsertContentNode, getContentNode).
  - Recommendation: use the wrapper consistently for all Dynamo calls to remove repetition and centralize logging/handling.

- Attribute duplication in upsertContentNode
  - For `PostNode` and `PageNode`, you write both the node’s own attributes (from `node.attributes`) and again the `attributes` param (which your tests often pass equal to `node.attributes`). The second pass will overwrite the same `attr#` keys.
  - Recommendation: adopt a single source of truth. Either:
    - Use only `node.attributes` and remove the separate `attributes` parameter, OR
    - Keep the parameter and do not write `node.attributes` automatically; let the caller decide.

- Mapping completeness
  - `mapToPageNode` declares TODOs and deliberately leaves `sections = emptyMap()` though you write section keys (SS) in upsert.
  - `mapToPostNode` doesn’t restore `next`/`prev` even though you write them. Consider fully round-tripping fields or documenting why they aren’t returned.

- Table/Index alignment
  - `listAllProjects` depends on scanning by a string pattern for `domain#type`. The test’s createTable also defines GSIs/LSIs. Ensure production table/CDK keep these in sync; the test file warns this may drift.

- Hard-coded default table name
  - The constructor default table name is a long “dev” table physical ID. It should be injected from config/env, and prod/dev separation enforced by DI or module wiring.

- Region parameter not used
  - You accept `region: Region` in the constructor but don’t use it. If the client is always injected, remove region from the service to avoid confusion.

- Logging polish
  - A few log messages indicate copy/paste (e.g., mapToFolderNode catch says StaticNode). Also consider structured logging for repeated patterns.

---

### Specific merge/split suggestions
1. Centralize error handling using executeDynamoOperation
   - Update saveProject, deleteProject, upsertContentNode, getContentNode to use the wrapper. This removes repeated try/catch blocks and makes behavior consistent.

2. Split upsertContentNode into type-specific builders
   - Current `when(node)` is large and mixes concerns. Extract private helpers:
     - `buildPostItem(...)`, `buildPageItem(...)`, `buildTemplateItem(...)`, `buildStaticItem(...)`, `buildImageItem(...)`, `buildFolderItem(...)`
   - Each returns `MutableMap<String, AttributeValue>` merged with a common base map: `domain#type`, `srcKey`, and unified `type#lastUpdated`.
   - This makes adding/removing attributes per-type safer and easier to test.

3. Merge getKeyListMatchingTemplate into getKeyListMatchingAttributes (or implement as a thin wrapper)
   - The “templateKey equals X” case is a special case of the attributes-based function. Consider:
     - Keep `getKeyListMatchingTemplate` as a convenience wrapper that calls `getKeyListMatchingAttributes(projectDomain, contentType, mapOf("templateKey" to templateKey), limit=..., descending=...)`.
     - Or remove the separate method to reduce API surface.

4. Introduce an enum for “first/last”
   - `getFirstOrLastKeyFromLSI(..., operation: String)` currently checks `operation == "first"`. Using an enum or a boolean `first: Boolean` eliminates stringly-typed errors.

5. Extract common query builders
   - Repeated patterns building `QueryRequest`/`GetItemRequest`/`PutItemRequest` can be moved to small private helpers to reduce duplication, especially for the “partition by domain#type” condition.

---

### Concrete code fixes (illustrative snippets)
- Fix milliseconds bug:
  ```kotlin
  private fun extractLastUpdatedTimestamp(item: Map<String, AttributeValue>, itemType: String): Instant {
      val lastUpdatedStr = item["type#lastUpdated"]?.s()
      return try {
          if (lastUpdatedStr != null && lastUpdatedStr.contains("#")) {
              val timestamp = lastUpdatedStr.substringAfter('#').toLongOrNull() ?: 0L
              Instant.fromEpochMilliseconds(timestamp)
          } else {
              log("WARN", "Invalid type#lastUpdated format in $itemType item: $lastUpdatedStr")
              Clock.System.now()
          }
      } catch (e: Exception) {
          log("WARN", "Failed to parse lastUpdated timestamp: $lastUpdatedStr", e)
          Clock.System.now()
      }
  }
  ```

- Fix dynamic placeholder in getKeyListFromLSI:
  ```kotlin
  val valueName = ":${attribute.first}"
  val request = QueryRequest.builder()
      .tableName(tableName)
      .indexName(lsiName)
      .keyConditionExpression("#pk = :domainType AND #dt $operation $valueName")
      .expressionAttributeNames(mapOf("#pk" to "domain#type", "#dt" to attribute.first))
      .expressionAttributeValues(
          mapOf(
              ":domainType" to AttributeValue.builder().s("$projectDomain#${contentType.dbType}").build(),
              valueName to AttributeValue.builder().s(attribute.second).build()
          )
      )
      .limit(limit)
      .scanIndexForward(!descending)
      .build()
  ```

- Fix Pages mapping in getContentNode:
  ```kotlin
  when (contentType) {
      SOURCE_TYPE.Templates -> mapToTemplateNode(item)
      SOURCE_TYPE.Posts     -> mapToPostNode(item)
      SOURCE_TYPE.Statics   -> mapToStaticNode(item)
      SOURCE_TYPE.Folders   -> mapToFolderNode(item)
      SOURCE_TYPE.Pages     -> mapToPageNode(item)
      else -> error("Unsupported content type: ${contentType.dbType}")
  }
  ```

- Fix ordering flag in getKeyListMatchingAttributes:
  ```kotlin
  .scanIndexForward(!descending)
  ```

- Avoid overwriting type#lastUpdated for static/image:
  - Remove the second assignment in the `is ContentNode.StaticNode, is ContentNode.ImageNode` branch; rely on the initial one based on `contentType.dbType`.

- Use the operation wrapper everywhere:
  ```kotlin
  override suspend fun saveProject(project: CantileverProject): CantileverProject =
      executeDynamoOperation("save project", "domain: ${project.domain}, project: ${project.projectName}") {
          val item = ...
          dynamoDbClient.putItem(request).await()
          project
      }
  ```

- Consider eliminating the separate `attributes: Map<String, String>` parameter on upsertContentNode and using `node.attributes` consistently, or vice versa, to prevent confusion and accidental overwrites.

---

### Tests: issues and opportunities
- Duplicate srcKey in two tests (post2 and post3 both `non-matching-post.md`). Make them unique to avoid overwriting and to better validate ordering queries.
- The test table’s LSI “Type-Date” is KEYS_ONLY and fine for key projection, but be mindful that production/CDK needs to remain in sync; otherwise, integration failures will occur. Consider a small helper in tests (e.g., shared test fixture) or reading index names from a single source of truth.
- Assertion coverage:
  - Add a test for Pages round-trip via getContentNode (after the fix).
  - Add a test that confirms `descending` truly returns different order compared to `ascending` for getKeyListMatchingAttributes.
  - Add a test for `getKeyListFromLSI` using a non-"date" attribute to catch the placeholder bug in the future.

---

### Should any functions be merged or split?
- Merge:
  - getKeyListMatchingTemplate can be implemented as a thin wrapper over getKeyListMatchingAttributes to reduce code duplication.
  - All repetitive try/catch to the `executeDynamoOperation` path.
- Split:
  - upsertContentNode into type-specific builders as noted above.
  - Consider moving all “node mapping” functions into a dedicated mapper object/class to shorten this service and improve cohesion.

---

### Summary of recommendations
- Correctness
  - Fix epoch milliseconds vs seconds
  - Fix LSI expression placeholder mismatch
  - Add Pages to getContentNode
  - Fix ordering flag in getKeyListMatchingAttributes
  - Stop overwriting `type#lastUpdated` for static/image
- API and consistency
  - Use executeDynamoOperation everywhere
  - Merge template matching into generic attribute matching or make it a wrapper
  - Consider removing duplicated attributes pass in upsert
- Structure
  - Split type-specific item builders and move mappers to a separate component
  - Make table name fully configurable; remove unused region param if client is injected
- Tests
  - Fix duplicate srcKey
  - Add coverage for Pages and for attribute != "date" in LSI function

Implementing these changes will reduce duplication, improve correctness, and make future maintenance easier.