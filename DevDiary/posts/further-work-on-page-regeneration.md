---
title: Further work on page regeneration
templateKey: sources/templates/post.html.hbs
date: 2023-03-01
slug: further-work-on-page-regeneration
---
I think I have the basics working for triggering page regeneration without having to (re-)upload the markdown file to the S3 bucket. There is now an API as previously described:

```
put("/generate/page/{srcKey}")
```

You can specify the wildcard `*` and the application will trigger regeneration of all the markdown page files. I've only got one in my test project so far, but it seems to work!
I have chosen the HTTP verb "PUT" for this action, as it does trigger an action on the server which changes the state (because files are recreated and possibly changed). "GET" did not seem appropriate in this case.

I had to change my router function to accept PUT and POST requests with no body, a scenario I had thought wasn't allowed. I've decided that all PUT and POST requests which do not contain a body must have the `Content-Length` header set with a value of 0.
