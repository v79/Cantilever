---
title: Multiple website support
templateKey: sources/templates/post.html.hbs
date: 2024-02-11
slug: multiple-website-support
---
Cantilever can only build the Cantilever devlog website. The eventual goal is for the project to be more generic than that, so that it can build multiple websites. If I am ever to truly retire *bascule*, then Cantilever needs to be able to build the https://www.liamjd.org/ blog as well.

I need to add multi-site support to Cantilever, and this will be the biggest change yet. There's a lot to do:

- each project will get its own folder in the sources bucket
- each project needs a unique name and guid
- add permissions to a project in Cognito
- add permissions checks at login and across the project
- add a UI to switch between projects

The real challenge will be in publishing to the destination bucket:

- currently each website is in its own bucket
- how would I grant Cantilever write permission to multiple buckets - especially if those buckets are not configured during CDK deployment?
- or can I put all the generated websites into the same bucket, and have cloudfront or a lambda@edge function
- a temporary fix may be to generate to /project-id/generated/... and then separately copy the content to the true destination bucket

I am worried that Cognito does not give me the control I need. I had assumed that I could add a custom attribute to my cognito user, listing the IDs of the projects it can access. But I've been reading that Cognito does not really support that sort of behaviour, and I'm sure it's not super-secure either (AWS Cognito does not seem to get a lot of love from the community, but I'm not considering switching to another auth solution). I can probably use Cognito Groups to fudge it.

How will I give Cantilever write access to other buckets that are created outside of the CDK deployment process? Indeed, where will I record that a particular cantilever project is associated with a particular bucket? That information will probably need to go into `cantilever.yaml`. What else goes there? I certainly don't want to store actual secrets and keys in this yaml file.

I'm getting myself quite twisted into knots trying to design the api for authenticating and role checks.

I want something like:

```kotlin
auth(cognitoJWTAuthorizer,setOf("admin","creator")) {
  put("/posts/new", posts::new)
}
```

Which would allow the API to check that the user is authorized (has the valid JWT token), and can perform the action (has been granted one of the roles listed ("admin" or "creator"). But I want to do this in such a way that I don't need to polute my general `Authorizer`interface with stuff which may not be relevant. I wonder how Spring and Ktor do it?

Adding more into this routing API is getting challenging - because it's making the routes unreadable, especially with the OpenAPI specifications. For example:

```kotlin
 auth(cognitoJWTAuthorizer, setOf("admin")) {
   post(
     "/save",
     pageController::saveMarkdownPageSource,
   ).supplies(setOf(MimeType.plainText)).spec(
     Spec.PathItem("Save page", "Save markdown page source")
   )
}
```

There is a lot going on here and the key elements **GET** and **/save** are getting lost in the noise.

