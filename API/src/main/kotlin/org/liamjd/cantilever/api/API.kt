package org.liamjd.cantilever.api

import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module
import org.liamjd.cantilever.api.controllers.*
import org.liamjd.cantilever.api.models.DummyClass
import org.liamjd.cantilever.auth.CognitoJWTAuthorizer
import org.liamjd.cantilever.routing.*
import org.liamjd.cantilever.services.S3Service
import org.liamjd.cantilever.services.SQSService
import org.liamjd.cantilever.services.impl.S3ServiceImpl
import org.liamjd.cantilever.services.impl.SQSServiceImpl
import software.amazon.awssdk.regions.Region

/**
 * Set up koin dependency injection
 */
val appModule = module {
    single<S3Service> { S3ServiceImpl(Region.EU_WEST_2) }
    single<SQSService> { SQSServiceImpl(Region.EU_WEST_2) }
}

/**
 * The Router class responds to the AWS API Gateway {proxy+} "path" parameter
 */
class LambdaRouter : RequestHandlerWrapper() {

    private val sourceBucket: String = System.getenv("source_bucket")
    private val destinationBucket: String = System.getenv("destination_bucket")
    override val corsDomain: String = System.getenv("cors_domain") ?: "https://www.cantilevers.org/"

    init {
        startKoin {
            modules(appModule)
        }
    }

    // May need some DI here once I start needing to add services for S3 etc
    private val postController = PostController(sourceBucket = sourceBucket)
    private val pageController = PageController(sourceBucket = sourceBucket)
    private val templateController = TemplateController(sourceBucket = sourceBucket)
    private val generatorController =
        GeneratorController(sourceBucket = sourceBucket)
    private val projectController = ProjectController(sourceBucket = sourceBucket)
    private val metadataController = MetadataController(sourceBucket = sourceBucket)

    private val cognitoJWTAuthorizer = CognitoJWTAuthorizer(
        mapOf(
            "cognito_region" to System.getenv("cognito_region"),
            "cognito_user_pools_id" to System.getenv("cognito_user_pools_id")
        )
    )

    companion object {
        const val SRCKEY = "{srcKey}"
    }

    override val router = lambdaRouter {

//        filter = loggingFilter()

        /**
        /warm is an attempt to pre-warm this lambda. /ping is an API Gateway reserved route
         */
        get("/warm") { _: Request<Unit> -> println("Ping received; warming"); ResponseEntity.ok("warming") }.supplies(
            setOf(MimeType.plainText)
        )

        group(
            "/project", Spec.Tag(name = "Project", description = "Manage the overall project settings")
        ) {
            auth(cognitoJWTAuthorizer) {
                get(
                    "/",
                    projectController::getProject,
                    Spec.PathItem("Get project definition", "Returns the cantilever.yaml definition file")
                )
                put(
                    "/",
                    projectController::updateProjectDefinition,
                    Spec.PathItem("Update project definition", "Supply an updated cantilever.yaml definition file")
                ).expects(
                    setOf(MimeType.yaml)
                ).supplies(setOf(MimeType.json))
                group("/pages") {
                    get("", pageController::getPages, Spec.PathItem("Get pages", "Returns a list of all pages"))
                    post(
                        "/",
                        pageController::saveMarkdownPageSource,
                        Spec.PathItem("Save page", "Save markdown page source")
                    ).supplies(setOf(MimeType.plainText))
                    get(
                        "/$SRCKEY",
                        pageController::loadMarkdownSource,
                        Spec.PathItem("Get page source", "Returns the markdown source for a page")
                    )
                    put(
                        "/folder/new/{folderName}",
                        pageController::createFolder,
                        Spec.PathItem("Create folder", "Pages can be nested in folders")
                    ).supplies(setOf(MimeType.plainText))
                }
                get(
                    "/templates/{templateKey}",
                    templateController::getTemplateMetadata,
                    Spec.PathItem("Get template metadata", "Returns the metadata for a template")
                )
            }
        }


        group("/posts", Spec.Tag(name = "Posts", description = "Create, update and manage blog posts")) {
            auth(cognitoJWTAuthorizer) {
                get("", postController::getPosts, Spec.PathItem("Get posts", "Returns a list of all posts"))
                get(
                    "/$SRCKEY",
                    postController::loadMarkdownSource,
                    Spec.PathItem("Get post source", "Returns the markdown source for a post")
                )
                get(pattern = "/preview/$SRCKEY") { request: Request<Unit> -> ResponseEntity.notImplemented(body = "Not actually returning a preview of ${request.pathParameters["srcKey"]} yet!") }.supplies(
                    setOf(MimeType.html)
                )
                post(
                    "/save",
                    postController::saveMarkdownPost,
                    Spec.PathItem("Save post", "Save markdown post source")
                ).supplies(
                    setOf(MimeType.plainText)
                )
                delete(
                    "/$SRCKEY",
                    postController::deleteMarkdownPost,
                    Spec.PathItem("Delete post", "Delete a blog post")
                ).supplies(setOf(MimeType.plainText))
            }
        }

        group("/templates", Spec.Tag(name = "Templates", description = "Create, update and manage templates")) {
            auth(cognitoJWTAuthorizer) {
                get(
                    "",
                    templateController::getTemplates,
                    Spec.PathItem("Get templates", "Returns a list of all templates")
                )
                get(
                    "/$SRCKEY",
                    templateController::loadHandlebarsSource,
                    Spec.PathItem("Get template source", "Returns the handlebars source for a template")
                )
                post(
                    "/",
                    templateController::saveTemplate,
                    Spec.PathItem("Save template", "Save handlebars template source")
                ).supplies(setOf(MimeType.plainText))
            }
        }

        group("/generate", Spec.Tag("Generation", "Trigger the regeneration of pages and blog posts")) {
            auth(cognitoJWTAuthorizer) {
                put(
                    "/post/$SRCKEY",
                    generatorController::generatePost,
                    Spec.PathItem("Regenerate a post", "Trigger the regeneration of a post")
                ).supplies(setOf(MimeType.plainText))
                put(
                    "/page/$SRCKEY",
                    generatorController::generatePage,
                    Spec.PathItem("Regenerate a page", "Trigger the regeneration of a page")
                ).supplies(setOf(MimeType.plainText))
                put(
                    "/template/{templateKey}",
                    generatorController::generateTemplate,
                    Spec.PathItem(
                        "Regenerate content based on a template",
                        "Regenerate all the pages or posts that use this template"
                    )
                ).supplies(setOf(MimeType.plainText)).expects(emptySet())
            }
        }

        group("/metadata", Spec.Tag("Metadata", "Manage the metadata.yaml file for the project")) {
            auth(cognitoJWTAuthorizer) {
                put(
                    "/rebuild",
                    metadataController::rebuildFromSources,
                    Spec.PathItem("Rebuild metadata", "Rebuild the metadata.yaml file from the source files")
                )
            }
        }

        get("/openAPI") { _: Request<Unit> ->
            val openAPI = this.openAPI()
            ResponseEntity.ok(openAPI)
        }.supplies(
            setOf(MimeType.plainText)
        ).addHeaders(mapOf("Access-Control-Allow-Origin" to "*"))

        get("/showAllRoutes") { _: Request<Unit> ->
            val routeList = this.listRoutes()
            ResponseEntity.ok(routeList)
        }.supplies(setOf(MimeType.plainText))


        get("/dummy") { request: Request<DummyClass> ->
            ResponseEntity.ok(request.body.toString())
        }.supplies(setOf(MimeType.plainText))
    }
}


/**
 * Possible extension: custom Filters, like a logging filter, which intercepts a route, performs an action, then passes it on to the correct handler.
 * Something like:
 * `private fun loggingFilter() = Filter { next ->
 *          { request ->
 *              println("Handling request ${request.httpMethod} ${request.path}")
 *              next(request)
 *          }
 *      }`
 */