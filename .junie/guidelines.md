# Cantilever Project Guidelines

## Project Overview

Cantilever is an AWS Lambda-driven static site generator written in Kotlin. It converts markdown files to HTML using Flexmark-java and processes templates with handlebars.java. The application is deployed to AWS using CDK (Cloud Development Kit) and consists of multiple Lambda functions triggered by S3 events and SQS queues. There are CDK stacks for "dev" and "prod". AI agents such as Junie must NEVER push to prod without explicit instruction to do so.

The project is being refactored to store project and document metadata in a DynamoDB database. Source and output files will remain in S3. There is a clear separation of front-end and back-end elements.

A project is split across S3 buckets - a _source_ bucket, where files are uploaded, or placed by the web front end. A _generated_ bucket contains intermediate data, such as HTML fragments created by the Markdown processor. FIially, a destination bucket contains the complete HTML, CSS, and images for the static output. A fourth bucket contains the web editor interface, a static SvelteKit 4 project.

This application is built on a Windows machine.

## Project Structure

The project follows a modular architecture with the following main components:

- **Root Project**: Contains AWS CDK code for infrastructure deployment. The code is written in Kotlin but using the Java CDK libraries.
- **API**: REST API implementation for the web editor. This uses a custom API routing library, written in Kotlin and running on AWS Lambda. This custom library is available in maven local and on Github at https://github.com/v79/APIViaduct
- **FileUploadHandler**: Lambda function for handling file uploads. This lambda analyses the uploaded files and determines how they should be processed. Events are placed on an SQS queue for further processing by other lambdas.
- **MarkdownProcessor**: Lambda function for converting markdown to HTML
- **TemplateProcessor**: Lambda function for applying Handlebars templates
- **ImageProcessor**: Lambda function for processing images
- **SharedModels**: Common data models shared across modules
- **WebEditor**: SvelteKit-based web interface for content management, deployed as a static SPA.

Each module follows the standard Maven/Gradle project structure:
```
module/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   └── resources/
│   └── test/
│       ├── kotlin/
│       └── resources/
└── build.gradle.kts
```

## Build System

The project uses Gradle with Kotlin DSL (build.gradle.kts) for build configuration. Key aspects:

- JDK 21 is required for building and running the application
- Kotlin 2.2.0 is used as the primary programming language
- Dependencies are managed through Gradle's dependency management system
- Each module has its own build.gradle.kts file with module-specific dependencies
- Runtime dependencies are injected using the Koin library (DI)
- The front-end in the WebEditor folder is built using vite and npm.

To build the project:
```
./gradlew build
```

To deploy to AWS:
```
cdk deploy <CantileverStack|Cantilever-Dev-Stack>
```

## Testing Guidelines

### Testing Framework

The project uses JUnit 5 as the primary testing framework with the following supporting libraries:

- **MockK**: For mocking dependencies in tests
- **Koin Test**: For dependency injection in tests
- **AWS Lambda Java Tests**: For testing Lambda functions

### Test Structure

Tests follow these conventions:

1. Test classes are named with a `Test` suffix (e.g., `TemplateControllerTest`)
2. Test methods use backtick notation for descriptive names (e.g., `` `updates an existing template file` ``)
3. Tests extend `KoinTest` when dependency injection is needed
4. `@Test` annotation is used to mark test methods
5. Tests are organized in the same package structure as the implementation code

### Test Patterns

Common test patterns include:

1. **Mocking external dependencies**:
   ```kotlin
   declareMock<S3Service> {
       every { mockS3.objectExists("my-template", sourceBucket) } returns true
   }
   ```

2. **Assertion patterns**:
   ```kotlin
   assertNotNull(response)
   assertEquals(200, response.statusCode)
   ```

3. **Setup with Koin extensions**:
   ```kotlin
   @JvmField
   @RegisterExtension
   val koinTestExtension = KoinTestExtension.create {
       modules(module {
           single<S3Service> { S3ServiceImpl(Region.EU_WEST_2) }
       })
   }
   ```

4. **Teardown**:
   ```kotlin
   @AfterEach
   fun tearDown() {
       stopKoin()
   }
   ```

## Deployment Process

The project uses GitHub Actions for CI/CD with two main workflows:

1. **Gradle Build**: Builds the project on pull requests
   - Uses JDK 21 (Amazon Corretto)
   - Runs on Ubuntu

2. **AWS CDK Deployment**: Deploys to AWS on pushes to master
   - Configures AWS credentials
   - Sets up JDK 21 and Node.js 18
   - Installs AWS CDK globally
   - Runs `cdk deploy --context env=prod --all`

## Development Guidelines

### Code Style

- Follow Kotlin coding conventions
- Use meaningful names for classes, methods, and variables
- Write descriptive comments for complex logic
- Keep methods focused on a single responsibility

### Architecture Principles

1. **Modularity**: Each component has a specific responsibility
2. **Serverless**: Utilise AWS Lambda for event-driven processing
3. **Stateless**: Functions should be stateless to enable scaling
4. **Event-driven**: Components communicate through events (S3 events, SQS messages)

### Adding New Features

1. Create appropriate unit tests first (TDD approach)
2. Implement the feature in the relevant module
3. Update shared models if needed
4. Test locally using the provided test events
5. Deploy to a development environment for integration testing

### Common Patterns

1. **Lambda Handler Pattern**:
   ```kotlin
   class MyHandler : RequestHandler<S3Event, String> {
       override fun handleRequest(input: S3Event, context: Context): String {
           // Process event
       }
   }
   ```

2. **Dependency Injection with Koin**:
   ```kotlin
   val myModule = module {
       single<MyService> { MyServiceImpl() }
   }
   ```

3. **API Request Handling**:
   ```kotlin
   fun handleRequest(request: Request<MyRequestType>): APIGatewayProxyResponseEvent {
       // Process request and return response
   }
   ```
   
4. **Defining HTTP restful routes**
```kotlin
   group("/path") {
       get("", controller::getMethod) // HTTP GET /path calls the method 'getMethod'
       post("/save", controller::saveItem) // HTTP POST /path/save, expects a JSON body
       get("/id", controller::getId).supplies(MimeType.plainText) // HTTP GET /path/id, will return a text/plain response
}
```

## Troubleshooting

- Check CloudWatch logs for Lambda function errors
- Use the test events in the `testEvents` directory for local testing
- Verify S3 bucket permissions when encountering access issues
- Check API Gateway configuration for API-related issues