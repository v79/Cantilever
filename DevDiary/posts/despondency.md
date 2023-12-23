---
title: Despondency - project rethink
templateKey: sources/templates/post.html.hbs
date: 2023-10-25
slug: despondency
---
I don't think _Cantilevers_ is going to work, not in its current incarnation. I've made a good amount of progress, and this website is self-hosting, built using Cantilevers. I have a web UI to create and edit pages, posts and templates. I can trigger regeneration of the website. The _infrastructure_ of it, running in the AWS cloud, does what I have asked it to.

But there are some fundamental flaws in my design. Flaws I was vaguely aware of right at the start, but I chose to ignore them while I built and learned what I could.

The current application design is:

- A markdown file us uploaded to S3. It is analysed and a message is put on an SQS queue for conversation to HTML.
- Posts are converted to HTML and stored in S3. Pages contain multiple sections, and they are individually converted and stored.
- Then a new message is put on an SQS queue. The HTML fragments are combined with a Handlebars template, and the final web page is written to the website S3 bucket.

There were some design principles for _Cantilever_ which haven't really held up well.

I wanted everything to be defined solely by the markdown files, a little bit of yaml, and the handlebars templates. There was to be no database underlying the project.

This didn't really work out. In order to present the web UI, and the list of files, I introduced a `JSON` file for each of the files - pages, posts and templates. As someone pointed out to me, while that isn't a SQL relational database, it is, ultimately, a database nonetheless.

Cantilever can recreate the `JSON` files at any point - they don't need to persist - but they are needed, first for the UI, but more crucially, for navigation.

Without `posts.json`, there's no way for Cantilevers to order posts, to calculate "previous" and "next". (My previous project, _Bascule_, rebuilt the website on every run, building an in-memory model to track the relationship between posts. [Bascule](https://www.liamjd.org/bascule.html) did also have a persistent JSON model, but it was more of a cache, to skip recalculation of pages that haven't been changed.)

_Cantilevers_ is a little different. Right now, there's no way to rebuild the entire website. When a new file is created (or just uploaded), it is instantly parsed, transformed to HTML, rendered through its template, and uploaded to the website. In that scenario, how is _Cantilever_ to know what post comes before, and what comes after, the new file? Well, _Cantilever_ loads `posts.json`. Unfortunately, I don't regenerate `posts.json` before parsing the markdown file, so `posts.json` is out-of-date.

I could regenerate it, before parsing the markdown. But Cantilever has another behaviour that makes this tricky - it's asynchronous. It's possible to upload 10 files at once, and they will be queued for generation. There's nothing deterministic or ordered in that queue. I like that behaviour, but it's incompatible with the need to know the entire project structure to determine navigation links. The other option, long in the back of my mind, is to make page navigation a javascript function running in the browser. It would still, ultimately, need to make a call to the server to determine what post is before or next, which seems rather wasteful of network traffic for a "static" site generator.

I'm not sure what approach to take. More and more, I feel that having a persistent database (SQL or NoSQL) is necessary. That database could be updated with every file upload. And I'll have to postpone the final generation (fragment + handlebars templates) until the database is updated and all the markdown files have been processed.

> It's quite a big rework, and it pulls me away from the design goal of "everything is defined by markdown and yaml".

