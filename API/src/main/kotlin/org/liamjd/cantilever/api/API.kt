package org.liamjd.cantilever.api

import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module
import org.liamjd.cantilever.api.controllers.*
import org.liamjd.cantilever.auth.CognitoJWTAuthorizer
import org.liamjd.cantilever.common.MimeType
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
    private val generatorController = GeneratorController(sourceBucket = sourceBucket)
    private val projectController = ProjectController(sourceBucket = sourceBucket)
    private val metadataController = MetadataController(sourceBucket = sourceBucket)
    private val mediaController = MediaController(sourceBucket = sourceBucket)

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

        /**
        /warm is an attempt to pre-warm this lambda. /ping is an API Gateway reserved route
         */
        get("/warm") { _: Request<Unit> -> println("Ping received; warming"); ResponseEntity.ok("warming") }.supplies(
            setOf(MimeType.plainText)
        ).spec(Spec.PathItem("Warm", "Warms the lambda router"))

        /** ============ /pages ================ **/
        group("/pages", Spec.Tag(name = "Pages", description = "Create, update and manage static pages")) {
            auth(cognitoJWTAuthorizer) {
                get("", pageController::getPages).spec(Spec.PathItem("Get pages", "Returns a list of all pages"))

                post(
                    "/save",
                    pageController::saveMarkdownPageSource,
                ).supplies(setOf(MimeType.plainText)).spec(
                    Spec.PathItem("Save page", "Save markdown page source")
                )

                get(
                    "/$SRCKEY",
                    pageController::loadMarkdownSource,
                ).spec(
                    Spec.PathItem("Get page source", "Returns the markdown source for a page")
                )

                put(
                    "/folder/new/{folderName}",
                    pageController::createFolder,
                ).spec(
                    Spec.PathItem("Create folder", "Pages can be nested in folders, but don't go too deep!")
                ).supplies(setOf(MimeType.plainText))

                delete("/$SRCKEY", pageController::deleteMarkdownPageSource).supplies(setOf(MimeType.plainText))
                    .spec(Spec.PathItem("Delete page", "Delete a static page"))

                delete("/folder/{srcKey}", pageController::deleteFolder).supplies(setOf(MimeType.plainText))
                    .spec(Spec.PathItem("Delete folder", "Delete a folder. It must be empty"))

                post("/reassignIndex", pageController::reassignIndex).supplies(setOf(MimeType.plainText)).spec(
                    Spec.PathItem(
                        "Reassign index page for folder",
                        "Set a new index page for the folder, so that it becomes index.html for that folder, and unset the previous index page"
                    )
                )
            }
        }

        /** ============ /folders ================ **/
        auth(cognitoJWTAuthorizer) {
            get("/folders", pageController::getFolders).spec(
                Spec.PathItem(
                    "Get folders",
                    "Returns a list of all folders"
                )
            )
        }

        /** ============ /project ================ **/
        group(
            "/project", Spec.Tag(name = "Project", description = "Manage the overall project settings")
        ) {
            auth(cognitoJWTAuthorizer) {
                get("/load/{domainKey}", projectController::getProject).spec(
                    Spec.PathItem("Get project definition", "Returns the <project>.yaml definition file"))

                get("/list", projectController::getProjectList).spec(
                    Spec.PathItem("Get all projects", "Returns a list of all projects")
                )

                put(
                    "/",
                    projectController::updateProjectDefinition,
                ).expects(
                    setOf(MimeType.yaml)
                ).supplies(setOf(MimeType.json)).spec(
                    Spec.PathItem("Update project definition", "Supply an updated cantilever.yaml definition file")
                )

                post(
                    "/new",
                    projectController::createProject,
                ).expects(
                    setOf(MimeType.yaml)
                ).supplies(setOf(MimeType.json)).spec(
                    Spec.PathItem("Create new project", "Supply a new yaml definition file for a new project")
                )

                // TODO: move this set of routes outside of the project group
                // TODO: I think this is obsolete, replaced with GET /metadata
                get(
                    "/templates/{templateKey}",
                    templateController::getTemplateMetadata,
                ).spec(
                    Spec.PathItem("Get template metadata", "Returns the metadata for a template")
                )
            }
        }

        /** ============ /posts ================ **/
        group("/posts", Spec.Tag(name = "Posts", description = "Create, update and manage blog posts")) {
            auth(cognitoJWTAuthorizer) {
                get("", postController::getPosts).spec(Spec.PathItem("Get posts", "Returns a list of all posts"))

                get(
                    "/$SRCKEY",
                    postController::loadMarkdownSource,
                ).spec(Spec.PathItem("Get post source", "Returns the markdown source for a post"))

                get(pattern = "/preview/$SRCKEY") { request: Request<Unit> ->
                    ResponseEntity.notImplemented(
                        body = "Not actually returning a preview of ${request.pathParameters["srcKey"]} yet!"
                    )
                }.supplies(
                    setOf(MimeType.html)
                ).spec(Spec.PathItem("Preview post", "When implemented, this will return a preview of a post"))

                post(
                    "/save", postController::saveMarkdownPost
                ).supplies(
                    setOf(MimeType.plainText)
                ).spec(Spec.PathItem("Save post", "Save markdown post source"))

                delete(
                    "/$SRCKEY", postController::deleteMarkdownPost
                ).supplies(setOf(MimeType.plainText)).spec(Spec.PathItem("Delete post", "Delete a blog post"))
            }
        }

        /** ============ /templates ================ **/
        group("/templates", Spec.Tag(name = "Templates", description = "Create, update and manage templates")) {
            auth(cognitoJWTAuthorizer) {
                get(
                    "",
                    templateController::getTemplates,
                ).spec(Spec.PathItem("Get templates", "Returns a list of all templates"))

                get(
                    "/$SRCKEY",
                    templateController::loadHandlebarsSource,
                ).spec(Spec.PathItem("Get template source", "Returns the handlebars source for a template"))

                post(
                    "/save",
                    templateController::saveTemplate,
                ).supplies(setOf(MimeType.plainText))
                    .spec(Spec.PathItem("Save template", "Save handlebars template source"))

                get("/usage/$SRCKEY", templateController::getTemplateUsage)
                    .spec(
                        Spec.PathItem(
                            "Get template usage", "Returns the count of pages and posts which use this template"
                        )
                    )

                delete("/$SRCKEY", templateController::deleteTemplate).supplies(setOf(MimeType.plainText))
                    .spec(Spec.PathItem("Delete template", "Delete a template"))
            }
        }

        /** ============ /media ================ **/
        group(
            "/media",
            Spec.Tag(name = "Media", description = "Create, update and manage images and other media files")
        ) {
            auth(cognitoJWTAuthorizer) {
                get(
                    "/images",
                    mediaController::getImages,
                ).spec(Spec.PathItem("Get images", "Returns a list of all images"))

                get("/images/$SRCKEY/{resolution}", mediaController::getImage).spec(
                    Spec.PathItem(
                        "Get image",
                        "Returns an image with the given key and image resolution"
                    )
                )

                post("/images/", mediaController::uploadImage)
                    .spec(Spec.PathItem("Upload image", "Upload an image to the source bucket"))

                delete("/images/$SRCKEY", mediaController::deleteImage).supplies(setOf(MimeType.plainText))
                    .spec(Spec.PathItem("Delete image", "Delete an image from the source bucket"))

            }
        }

        /** ============ /generate ================ **/
        group("/generate", Spec.Tag("Generation", "Trigger the regeneration of pages and blog posts")) {
            auth(cognitoJWTAuthorizer) {
                put(
                    "/post/$SRCKEY",
                    generatorController::generatePost,
                ).supplies(setOf(MimeType.plainText))
                    .spec(Spec.PathItem("Regenerate a post", "Trigger the regeneration of a post"))

                put(
                    "/page/$SRCKEY",
                    generatorController::generatePage,
                ).supplies(setOf(MimeType.plainText))
                    .spec(Spec.PathItem("Regenerate a page", "Trigger the regeneration of a page"))

                put("/images/resolutions") { _: Request<Unit> ->
                    ResponseEntity.notImplemented(
                        body = "Not implemented yet!"
                    )
                }.supplies(
                    setOf(MimeType.plainText)
                ).spec(
                    Spec.PathItem(
                        "Regenerate images",
                        "Trigger the regeneration of all images at all resolutions - NOT IMPLEMENTED"
                    )
                )

                put(
                    "/template/{templateKey}", generatorController::generateTemplate
                ).supplies(setOf(MimeType.plainText)).expects(emptySet())
                    .spec(
                        Spec.PathItem(
                            "Regenerate content based on a template",
                            "Regenerate all the pages or posts that use this template"
                        )
                    )
            }
        }

        /** ============ /metadata ================ **/
        group("/metadata", Spec.Tag("Metadata", "Manage the metadata.json file for the project")) {
            auth(cognitoJWTAuthorizer) {
                put(
                    "/rebuild",
                    metadataController::rebuildFromSources,
                ).expects(emptySet()).spec(
                    Spec.PathItem(
                        "Rebuild metadata",
                        "Rebuild the metadata.json file from the source pages, posts, templates and images"
                    )
                )
            }
        }

        /** ============ route overviews ================ **/
        get("/openAPI") { _: Request<Unit> ->
            val openAPI = this.openAPI()
            ResponseEntity.ok(openAPI)
        }.supplies(
            setOf(MimeType.plainText)
        ).addHeaders(mapOf("Access-Control-Allow-Origin" to "*"))
            .spec(Spec.PathItem("OpenAPI", "Returns the OpenAPI specification for this API"))


        get("/showAllRoutes") { _: Request<Unit> ->
            val routeList = this.listRoutes()
            ResponseEntity.ok(routeList)
        }.supplies(setOf(MimeType.plainText)).spec(
            Spec.PathItem(
                "Show all routes", "Returns a list of all routes in the API, a rather clumsy text list."
            )
        )

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