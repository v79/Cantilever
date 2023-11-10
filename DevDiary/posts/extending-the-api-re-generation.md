---
title: Extending the API - (Re)generation
templateKey: sources/templates/post.html.hbs post
date: 2023-02-28
slug: extending-the-api-re-generation
---
Testing _Cantilevers_ is quite a bit of manual work. My two approaches to testing conversion of a post into an HTML page are: use the website to create or edit a post, and save it; or copy a file from my computer to the S3 source bucket.

This works, but is rather slow. A better approach would be to trigger a regeneration of an existing file just by calling an API - and I could trigger this through a button on the website, or a through a REST call in IntelliJ IDEA. To do this, I've done a little refactoring of the application to introduce a common AWS SQS service, and then I will write a route along the lines of `PUT https://api.cantilevers.org/generate/page/{pageId}`. That route will simply create a message in the appriate SQS queue to start the markdown to HTML conversation, and then hence the handlebars template processing.

This will then allow me to trigger complete regeneration - perhaps by calling `/generate/page/*`. Furthermore, once I have a route for templates, I could call `/generate/template/post.hbs`, and my controller could look for all posts with the template name 'post`, and trigger regeneration of each post in turn.
