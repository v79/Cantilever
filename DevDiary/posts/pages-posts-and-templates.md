---
title: Pages, posts and templates
templateKey: sources/templates/post.html.hbs post
date: 2023-03-25
slug: pages-posts-and-templates
---
I have completed the first pass of all the backend work required to support `pages`, `posts`, and `templates`. There are now API routes to regenerate a given post (`/generate/post/<blog-post-key>.md`), a given page (`/generate/page/<page-key>.md`), all pages (`/generate/page/*`) and all pages with a given template (`/generate/template/<template-key>`).

Although I haven't covered every possible action, these main ones will allow me to work on the front end for pages and templates.

A reminder, **Cantilever** makes a distinction between a `post` - a dated blog entry, like a diary entry - and a `page` - a more static, undated piece of content, such as "about us" or "contact us" page. And crucially, the website home page is a `page`.

There is no UI for creating or editing pages yet, so that may be my next task. As usual, I seem to be trying to avoid front-end work...

I also want to create a UI for creating and editing `templates`, which are html/handlebars files used to render the final output.
