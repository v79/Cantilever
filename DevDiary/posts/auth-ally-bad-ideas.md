---
title: Auth-ally bad ideas
templateKey: sources/templates/post.html.hbs
date: 2024-02-16
slug: auth-ally-bad-ideas
---
While working on the design for multi-website support, I went down a bit of a rabbit hole trying to my Cantilever both multi-project and multi-user. I looked at ways of extending the authorization mechanism to handle permissions checks and roles and who knows what else.

This was a distraction - it will be a long time, probably never - before I will need to support multiple users in this tool. So I am going to focus on just multi-project support. This is needed because I currently own and maintain three websites. Two were built with *bascule*: my personal blog at [www.liamjd.org](https://www.liamjd.org) and a music blog I created for my dad during lockdown, [The Right Notes](https://www.therightnotes.org). And of course, the very meta [cantilevers.org website about the creation of the cantilevers website-creation website](https://www.cantilevers.org/). That's this blog, in case you were lost!

So no more experiments in authentication, but for reference, here are some of my earlier thoughts:

---



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


---

This is something I would like to return to eventually - learning more about authorization, security, RBAC and more is never a bad thing.
