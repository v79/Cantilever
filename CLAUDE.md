# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Kotlin/Gradle (root and all submodules)

```bash
./gradlew build                          # build all modules
./gradlew test                           # run all tests
./gradlew :API:test                      # run tests for a single module
./gradlew :API:shadowJar                 # build fat JAR for a Lambda module
./gradlew :SharedModels:build            # build SharedModels (needed by others)
```

Lambda modules each produce a named fat JAR via the Shadow plugin:
- `API` → `APIRouter.jar`
- `FileUploadHandler` → `FileUploadHandler.jar`
- `MarkdownProcessor` → `MarkdownProcessorHandler.jar`
- `TemplateProcessor` → `TemplateProcessorHandler.jar`
- `ImageProcessor` → `ImageProcessorHandler.jar`

### CDK deployment

```bash
cdk deploy Cantilever-Dev-Stack          # deploy dev environment
cdk deploy Cantilever-Prod-Stack         # deploy prod — only with explicit instruction
cdk deploy --context env=prod --all      # full prod deploy (CI does this)
```

**Never deploy to prod without explicit user instruction.**

### WebEditor (SvelteKit SPA in `WebEditor/`)

```bash
npm run dev        # local dev server on port 5173
npm run build      # production build
npm run staging    # staging build
npm run check      # svelte-check type checking
npm run lint       # prettier + eslint
npm run format     # auto-format
```

## Architecture

### Event-driven pipeline

Files written to the **source S3 bucket** trigger the `FileUploadHandler` Lambda via S3 events. That lambda routes work onto three SQS queues:
- `markdown_processing_queue` → `MarkdownProcessor` Lambda: converts `.md` to HTML fragments, writes to **generated bucket**
- `handlebar_template_queue` → `TemplateProcessor` Lambda: renders Handlebars templates with the HTML fragments, writes final HTML to **destination bucket**
- `image_processing_queue` → `ImageProcessor` Lambda: scales/converts images

The **API Lambda** (`NewLambdaRouter`) provides a REST API through API Gateway. It handles direct CRUD operations on content (reading/writing S3 files) and can also enqueue regeneration jobs.

### S3 bucket layout (source bucket)

```
<domain>/
  sources/
    cantilever.yaml       # project definition
    posts/                # markdown blog posts
    pages/                # markdown pages (nested folders supported)
    templates/            # Handlebars .html.hbs and .hbs partials
    statics/              # CSS, JS etc (copied as-is)
    images/               # source images
  generated/
    htmlFragments/        # intermediate HTML output from MarkdownProcessor
    images/               # scaled images
```

`SrcKey` (typealias for `String`) is always a fully-qualified S3 object key including the domain prefix.

### DynamoDB schema

Single table, `domain#type` (partition key) + `srcKey` (sort key). Custom attributes from page/post frontmatter are stored with an `attr#` prefix on column names. Two secondary indexes:
- GSI `Project-NodeType-LastUpdated`: partition by `domain`, sort by `type#lastUpdated`
- LSI `Type-Date`: sort by `date` (used for posts ordering)

### Modules

| Module | Purpose |
|--------|---------|
| `src/` | CDK app (`CantileverStack`) that defines all AWS infrastructure |
| `SharedModels` | Data classes (`ContentNode`, `CantileverProject`, SQS message models), AWS service interfaces (`S3Service`, `SQSService`, `DynamoDBService`) and their implementations |
| `API` | REST API Lambda using the [APIViaduct](https://github.com/v79/APIViaduct) routing library; Koin for DI; Cognito JWT auth |
| `FileUploadHandler` | S3 event handler; classifies uploads by path and dispatches SQS messages |
| `MarkdownProcessor` | Flexmark-java for Markdown→HTML; passes fragments to template queue |
| `TemplateProcessor` | handlebars.java for template rendering; writes to destination bucket |
| `ImageProcessor` | Image scaling/conversion |
| `OpenAPISchemaAnnotations` / `OpenAPISchemaGenerator` | KSP-based OpenAPI schema generation for the API |
| `WebEditor` | SvelteKit 4 SPA (Skeleton UI + Tailwind); talks to the API Lambda |

### API routing pattern

Routes are defined in `NewLambdaRouter` using APIViaduct's DSL:
```kotlin
group("/posts") {
    auth(cognitoJWTAuthorizer) {
        get("", postsController::getPosts)
        post("/save", postsController::saveMarkdownPost).supplies(MimeType.plainText)
    }
}
```
All API routes except `/warm` require Cognito JWT authentication. The API Lambda reads `source_bucket`, `generation_bucket`, `cors_domain`, `cognito_region`, `cognito_user_pools_id`, and `dynamodb_table` from Lambda environment variables.

## Testing

- JUnit 5 with MockK for mocking; Koin for DI in tests
- Integration tests that hit DynamoDB use TestContainers (Docker required)
- Test method names use backtick notation: `` `updates an existing template file` ``
- Test events for local Lambda testing are in `testEvents/`
- Tests follow Setup / Execute / Verify structure

```kotlin
// Koin test setup pattern
@JvmField
@RegisterExtension
val koinTestExtension = KoinTestExtension.create {
    modules(module { single<S3Service> { mockk() } })
}

@AfterEach
fun tearDown() { stopKoin() }
```

## Key constraints

- Built on Windows; use PowerShell-compatible commands
- JDK 21 required (Amazon Corretto in CI)
- Lambda modules use Java 21 runtime on AWS
- The `APIViaduct` routing library is in both mavenLocal and GitHub Packages (`https://maven.pkg.github.com/v79/APIViaduct`); requires `GITHUB_ACTOR` and `GITHUB_TOKEN` env vars to resolve
- `ContentNode` is a sealed class — when adding new node types, update `DynamoDBServiceImpl` mapping functions and the `when` expressions in `FileUploadHandler` and elsewhere
- The `SOURCE_TYPE` enum's `folder` and `dbType` fields control S3 path routing and DynamoDB partition key values respectively
