---
title: Redesign Thinking
templateKey: sources/templates/about.html.hbs
--- #body
My current design isn't going to work. Can I think of a new approach?

### Problem statement:

- In order to build post navigation, I need to have a complete view of all the posts (when parsing the handlebars templates).
- New posts can be created in the web UI, but could also appear as part of a file upload event to S3.
  - It is possible to upload multiple files at once
- This is currently recorded in `posts.json`

### Options

* Always regenerate `posts.json` on page save or file upload, prior to handlebars template processing
  * *Will not work for multiple file upload events*
  * *Is this 'wasteful'?*
* Use a database instead of posts.json, updating it for every save or upload event
  * Then query the database for every template processing operation, i.e. querying the db to get the next, previous, posts
  * *Lots more operations but could be very flexible*
* Fully separate and break the markdown -> handlebars pipeline.
  * Full page generation would only happen on some other trigger. Easy to arrange in the web UI, but how would I trigger it in a "headless", file-upload scenario? Either I don't bother, or I have a different file-upload event to trigger it, or maybe a timer or queue event?
    * From experience, when multiple files are uploaded at once, one 'instance' of the lambda is created for every ~ 10 files. I could trigger the `posts.json` regeneration once those 10 files are processed, but might get messy on a bulk upload.
  * I'll need to rework the SQS message passing but that may not be a bad thing.
  * Flow becomes: markdown file saved OR markdown file upload -> to markdown queue -> markdown 'fragments' generated. Then either manual trigger of handlebars parsing OR triggered by completion of markdown generation?