---
title: A New Metadata Model
templateKey: sources/templates/post.html.hbs
date: 2023-11-10
slug: new-metadata-model
---
I've gone round the houses on this, but I think I have finally settled on a new metadata model to replace `posts.json`, `pages.json` and `templates.json`. All three will be combined in a new `metadata.json` file which can be rebuilt by scanning all the source files and templates. But it can also respond to adding, deleting and updating existing items without needing a full rescan.

At the backend, I've tried to rationalise the number of same-but-different classes which represent a Page, Post or Template. But for a variety of reasons, there are still some near-duplicates.

The primary benefit of this new approach is that I can calculate the `@next` and `@prev` links for blog posts when the markdown files are saved, rather than waiting until template processing and hoping that the old `posts.json` file was up-to-date (it usually wasn't). I haven't quite hooked this up to HTML generation though, so links will be broken right now.

I am now working my way through the project, deprecating the old classes, methods and APIs, writing new ones, and updating the front-end code to match.

It's a lot of work. But I am making progress.
