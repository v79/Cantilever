---
title: OpenAPI and Routing
templateKey: sources/templates/post.html.hbs
date: 2023-11-29
slug: openapi-and-routing
---
There's been a lot of backend work recently, implementing a feature I have long wanted for my lambda routing function - an OpenAPI (swagger) API definition file.

My AWS lambda router allows me to define routes like this:

```kotlin
 override val router = lambdaRouter {
    get("/posts/{postKey}", postsController::getPosts)
    get("/posts/getFirst") { request: Request<Unit> ->
        ResponseEntity.notImplemented("I haven't written this function yet").supplies(setOf(MimeType.plainText))
    }
}
```

This example shows two different ways of defining a route. The first is the most useful - a route is declared by its method (get, post, etc), its path (including possible parameters in { }), and a function to call which implements that route (here, `postsController::getPosts)`.

The second declares the function body inline, without reference to separate function. I often use it for simple routes which only return a plain text response and don't need to call out to a service to retrieve data.

My router also allows me to group routes, and to add authentication to routes:

```kotlin
group("/posts") {
  auth(cognitoJWTAuth) {
    get("/{postKey}", postsController::getPosts)
  }
}
```

This declares a route GET /posts/{postKey}, as before, but it checks for authentication first - in this case, an AWS Cognito JWT bearer token.

So I have lots of routes declared and grouped - functions to create, load, and save pages, and posts, and to retrieve lists of pages, templates, etc etc. And it has been getting a little messy keep of track of how I want my API to be structured. There's been a couple of reworks already.

I'd really like a way of visualising the routes, to see how they are grouped, and to provide some narrative documentation. And now I can, thanks to OpenAPI.

### OpenAPI / Swagger Definitions

The OpenAPI specification provides a way of declaring and documenting your routes in yaml format (or JSON). There are various tools and editors available to view the specification file, and even to authenticate, test and manage your routes. The format looks a little like this:

```yaml
  /posts/{srcKey}:
    get:
      tags:
        - Posts
      summary: Get post source
      description: Returns the markdown source for a post
      security:
        - cognitojwt-bearer-token-authorizer: []
      parameters:
        - name: srcKey
          in: path
          required: true
          schema:
            type: string
      responses:
        200:
          description: OK
          content:
            application/json:
              schema:
                type: string
```

This is implemented as a function on the router class, and can be exposed through a route. My implementation is not complete - most notably, my implementation does not report the "schema" for routes yet. A schema is a representation of the data types the route expects. More on that later.

But it does produce valid OpenAPI 3.0.3 yaml that can be viewed and edited in a tool such as [SwaggerEditor](https://editor-next.swagger.io/). This has really helped me visualise my routes - and if I ever add image support to Cantilever, I'll post a screenshot here!

The OpenAPI specification allows you to "tag" routes into groups, so my implementation automatically creates tags based on the `group("/path")` function. The specification also allows for summary and descriptive text to be added, examples, external links and more. I've only implemented some of these - summary and description, so far. These are added in the route declaration.

```kotlin
group("/posts", Spec.Tag(name = "Posts", description = "Create, update and manage blog posts")) {
  auth(cognitoJWTAuthorizer) {
    get("", postController::getPosts, Spec.PathItem("Get posts", "Returns a list of all posts"))
    get(
       "/$SRCKEY",
       postController::loadMarkdownSource,
       Spec.PathItem("Get post source", "Returns the markdown source for a post")
       )
    }
}
```

By adding `Spec.Tag` and `Spec.PathItem` declarations to the route, I can add summaries and descriptions. I could extend this in future to support other OpenAPI features such as examples. i am happy that this is all working now, and I've found it useful. But I do find the `Spec` items to be very noisy, hiding the core details of the route (the method, path and function) in a sea of narrative text. I'll need to think about this. Options include moving the `Spec` into an annotation (`@Spec("Get posts","Returns a list...")`) but I don't think that would help much.

#### Specification update 04/12/2023

I've used extension functions allow a slightly cleaner presentation of specifications, moving them to the end of the route definition. Like this:

```kotlin
get("/$SRCKEY", postController::loadMarkdownSource)
  .spec(Spec.PathItem("Get post source","Returns the markdown source for a post")
```

In IntelliJ IDEA at least, this looks better and somehow less crowded.

### Schemas

A key part of the OpenAPI specification is declaring the types of object that the route handles. In my case, I have various classes which define pages, posts and so on. When the backend sends information about a Post object to the front end, it sends a serialized JSON string of this Kotlin class:

```kotlin
@APISchema
class PostNodeRestDTO(
    val srcKey: SrcKey,
    val title: String,
    val templateKey: String,
    val date: LocalDate,
    val slug: String,
    val body: String,
    val attributes: Map<String, String> = emptyMap()
)
```

This can be represented as an OpenAPI Schema definition Yaml object like this:

```yaml
org.liamjd.cantilever.models.rest.PostNodeRestDTO:
      type: object
      properties:
        srcKey:
          type: string
        title:
          type: string
        templateKey:
          type: string
        date:
          type: object
          description: object(LocalDate)
        slug:
          type: string
        body:
          type: string
        attributes:
          type: object
          description: object(Map<String, String>)
```

There's some errors here - I don't correctly represent the Kotlin `LocalDate` class, so for now I'm just adding a description field to the yaml output. Nor do I handle collections - the `attributes` property could be shown as a javascript object array, I think. But it's a good start and I think sufficient for my purposes.

So how do I generate these schema definitions? How does my router know what the content and structure of the `PostNodeRestDTO` class? Well, firstly, I annotate the class with a custom annotation, `@APISchema`. Normally I would use Kotlin's runtime reflection mechanism to find all classes with this annotation, and then iterate over the properties. I really didn't want to add runtime reflection though - it's slow, it's large, and is ill-suited to AWS Lambda functions.

Instead, I use [Kotlin Signal Processing](https://kotlinlang.org/docs/ksp-overview.html), a compile-time code reflection and code generation tool. At compile time, KSP scans for `@APISchema` classes, and writes their details to a yaml file. Through some rather hacky gradle scripting, that yaml file is copied to the API router project for inclusion in the JAR file which is uploaded to AWS Lambda. Then at runtime, my OpenAPI function quickly loads that yaml and inserts it into the OpenAPI response.

I'm a bit shocked that I managed to get this all to work. Shocked, but pleased.

I've more work to do - it's a bit hacky, a bit hard-coded, and crucially I need to 'wire up' the route definitions to there schema definitions in the OpenAPI yaml output.

But I am pleased I have been able to implement this feature. It's helping me rationalise my routes, and I've learned a lot in the process.

