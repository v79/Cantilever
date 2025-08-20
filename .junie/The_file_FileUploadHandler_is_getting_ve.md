### Summary
Your FileUploadHandler mixes several responsibilities: DI bootstrap, event routing, validation, metadata extraction, queue publishing, and DynamoDB node updates. You can keep a single Lambda entry point (handleRequest) while splitting the class into cohesive parts using a Router + Handler (Strategy) pattern and thin adapters around S3/SQS/Dynamo. This prepares you for deletion events and keeps each unit testable.

### Target design
- FileUploadLambda (thin entry point)
  - Parses each S3 record, builds an UploadContext, and delegates to S3EventProcessor.
- S3EventProcessor (router)
  - Chooses an UploadHandler based on SOURCE_TYPE for ObjectCreated events.
  - Chooses a DeletionHandler based on SOURCE_TYPE for ObjectRemoved events.
- Handlers (small classes per type)
  - PostUploadHandler, PageUploadHandler, TemplateUploadHandler, StaticUploadHandler, ImageUploadHandler, FolderCreationHandler
  - Future: PostDeletionHandler, PageDeletionHandler, TemplateDeletionHandler, ImageDeletionHandler, FolderDeletionHandler
- Ports (interfaces) for external dependencies
  - ObjectStore (wraps S3Service)
  - ContentNodeRepository (wraps DynamoDBService)
  - MarkdownPublisher, TemplatePublisher, ImagePublisher (wrap SQSService)
  - UploadConfig (reads env/queue URLs)
- Core utilities
  - UploadContext (per-record data), KeyParser/SourceKey helpers.

This reduces FileUploadLambda to ~60–90 LOC and each handler ~40–120 LOC.

### Suggested package layout
- org.liamjd.cantilever.lambda
  - FileUploadLambda.kt (entry point)
  - S3EventProcessor.kt (router)
  - config/UploadConfig.kt
  - core/
    - UploadContext.kt
    - KeyParser.kt (or reuse SourceHelper and SrcKey)
  - handlers/upload/
    - PostUploadHandler.kt
    - PageUploadHandler.kt
    - TemplateUploadHandler.kt
    - StaticUploadHandler.kt
    - ImageUploadHandler.kt
    - FolderCreationHandler.kt
  - handlers/delete/
    - PostDeletionHandler.kt
    - PageDeletionHandler.kt
    - TemplateDeletionHandler.kt
    - ImageDeletionHandler.kt
    - FolderDeletionHandler.kt
  - ports/
    - UploadHandler.kt, DeletionHandler.kt
    - ContentNodeRepository.kt, ObjectStore.kt
    - MarkdownPublisher.kt, TemplatePublisher.kt, ImagePublisher.kt
  - adapters/
    - DynamoContentNodeRepository.kt (uses DynamoDBService)
    - S3ObjectStore.kt (uses S3Service)
    - SqsMarkdownPublisher.kt, SqsTemplatePublisher.kt, SqsImagePublisher.kt (use SQSService)
  - di/FileUploadModule.kt (Koin bindings)

### Core interfaces
- UploadHandler
  - One per SOURCE_TYPE. Encapsulates validation, metadata building, upsert, publish.
  - suspend fun supports(type: SOURCE_TYPE): Boolean (or register by map key)
  - suspend fun handle(ctx: UploadContext)
- DeletionHandler
  - Encapsulates relationship cleanup, counters, parent/child updates.
- ContentNodeRepository
  - suspend fun upsert(srcKey, domain, type, node, attributes = emptyMap())
  - suspend fun get(srcKey, domain, type): ContentNode?
  - Helpers for folder relationships: ensureFolderExists, addFolderChild, removeFolderChild, etc.
- Publishers: MarkdownPublisher, TemplatePublisher, ImagePublisher
- ObjectStore: getObjectAsString, getContentType

### Thin entry point sketch
```kotlin
class FileUploadLambda(
    private val processor: S3EventProcessor,
    private val objectStore: ObjectStore,
    private val env: UploadConfig
) : RequestHandler<S3Event, String>, AWSLogger, KoinComponent {
    override var logger: LambdaLogger? = null

    override fun handleRequest(event: S3Event, context: Context): String {
        logger = context.logger
        runBlocking {
            event.records.forEach { r ->
                val srcKey = r.s3.`object`.urlDecodedKey
                val bucket = r.s3.bucket.name
                val domain = srcKey.substringBefore('/')
                val folder = srcKey.removePrefix("$domain/sources/").substringBefore('/')
                val type = SourceHelper.fromFolderName(folder)
                val contentType = objectStore.getContentType(srcKey, bucket)
                val ext = srcKey.substringAfterLast('.', "").lowercase().ifEmpty { null }
                val ctx = UploadContext(domain, srcKey, bucket, type, ext, contentType, env.queues, logger)
                processor.process(r.eventName, ctx)
            }
        }
        return "200 OK"
    }
}
```

### Router sketch
```kotlin
class S3EventProcessor(
    private val uploadHandlers: Map<SOURCE_TYPE, UploadHandler>,
    private val deleteHandlers: Map<SOURCE_TYPE, DeletionHandler>
) : AWSLogger {
    suspend fun process(eventName: String, ctx: UploadContext) {
        when {
            eventName.startsWith("ObjectCreated") ->
                (if (ctx.sourceType == SOURCE_TYPE.Folders && ctx.srcKey.endsWith("/"))
                    uploadHandlers[SOURCE_TYPE.Folders]
                 else uploadHandlers[ctx.sourceType])
                ?.handle(ctx) ?: log("WARN", "No upload handler for ${ctx.sourceType}")

            eventName.startsWith("ObjectRemoved") ->
                deleteHandlers[ctx.sourceType]?.handle(ctx) ?: log("WARN", "No deletion handler for ${ctx.sourceType}")

            else -> log("INFO", "Ignoring event '$eventName'")
        }
    }
}
```

### Mapping your current methods to handlers
- processPostUpload -> PostUploadHandler
- processPageUpload -> PageUploadHandler (move folder-relationship code to ContentNodeRepository helpers to slim the handler)
- processCSSUpload -> StaticUploadHandler
- processImageUpload -> ImageUploadHandler
- processFolderCreation -> FolderCreationHandler
- processTemplateUpload -> TemplateUploadHandler
- sendMarkdownMessage -> inside MarkdownPublisher
- upsertContentNode -> inside ContentNodeRepository

### Koin wiring example
```kotlin
val fileUploadModule = module {
    // Adapters
    single<ObjectStore> { S3ObjectStore(get()) }
    single<ContentNodeRepository> { DynamoContentNodeRepository(get()) }
    single<MarkdownPublisher> { SqsMarkdownPublisher(get()) }
    single<TemplatePublisher> { SqsTemplatePublisher(get()) }
    single<ImagePublisher> { SqsImagePublisher(get()) }
    single { UploadConfig(SystemEnvironmentProvider()) }

    // Upload handlers
    single(named(Posts.name)) { PostUploadHandler(get(), get(), get()) }   // ObjectStore, Repo, MarkdownPublisher
    single(named(Pages.name)) { PageUploadHandler(get(), get(), get()) }
    single(named(Templates.name)) { TemplateUploadHandler(get(), get()) }
    single(named(Statics.name)) { StaticUploadHandler(get(), get()) }
    single(named(Images.name)) { ImageUploadHandler(get(), get(), get()) }
    single(named(Folders.name)) { FolderCreationHandler(get()) }

    // Deletion handlers (add as you implement)
    // single(named(Posts.name)) { PostDeletionHandler(get(), get()) }

    // Router
    single {
        val uploads = mapOf(
            Posts to get<UploadHandler>(named(Posts.name)),
            Pages to get<UploadHandler>(named(Pages.name)),
            Templates to get<UploadHandler>(named(Templates.name)),
            Statics to get<UploadHandler>(named(Statics.name)),
            Images to get<UploadHandler>(named(Images.name)),
            Folders to get<UploadHandler>(named(Folders.name)),
        )
        val deletes = emptyMap<SOURCE_TYPE, DeletionHandler>()
        S3EventProcessor(uploads, deletes)
    }
}
```

### Folder relationship logic
Move the children-management currently in processPageUpload into repository helpers:
- ensureFolderExists(folderSrcKey, domain)
- addFolderChild(folderSrcKey, domain, childSrcKey)
- addRootPagesChild(domainRoot, childSrcKey)

The PageUploadHandler then becomes:
- build page metadata
- repo.upsert(page)
- If in subfolder: repo.ensureFolderExists(parent); repo.addFolderChild(parent, pageSrcKey)
- Else: repo.ensureFolderExists(rootPagesFolder); repo.addFolderChild(root, pageSrcKey)

### Error handling and validation
- Keep type-specific validation in handlers (e.g., .md, .hbs, MIME checks for images). Fail fast with clear logs.
- Router logs unknown events and continues. The entry function can still return "200 OK" even if some records fail, or you can track failures and return "207 Multi-Status"-like strings if desired.

### Testing plan (per your guidelines)
- Unit test each handler with MockK and KoinTestExtension:
  - Mock ObjectStore/Repository/Publishers
  - Verify: correct parsing, metadata, upsert calls, and messages
- Unit test S3EventProcessor routing for create/delete and each SOURCE_TYPE.
- Keep existing integration tests for the Lambda entry or add new ones fed by test events.

### Migration steps
1. Introduce ports and adapters (ObjectStore, ContentNodeRepository, Publishers).
2. Extract current private methods into dedicated handlers.
3. Implement UploadContext and S3EventProcessor.
4. Replace FileUploadHandler with the thin FileUploadLambda that calls the router (or keep the class name but strip to thin orchestrator to avoid AWS config changes).
5. Wire up Koin bindings for new components.
6. Add deletion handlers incrementally.
7. Add/adjust tests.

### Benefits
- Single AWS entry point preserved.
- Smaller, cohesive classes improve readability and maintainability.
- Easy to add deletion support and new source types without touching the entry point.
- Clear DI/ports make unit testing straightforward.
