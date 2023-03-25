---
title: Nearly self-hosting
template: post
date: 2023-02-25
slug: nearly-self-hosting
---
_Cantilever_ is nearly at the stage where I can use it to create blog posts, like this one. The application can:

- load markdown files
- save changes
- create and save new markdown files
- delete markdown files

I'll soon be able to return to some backend development - specifically, I need to be able to create an `index.html` file from the list of posts, and for that I'll need much more robust handling of page templates.

In the meantime, I'm continuing to improve the web front-end, and my understanding of the _sveltekit_ framework. I want to have a "spinner" display during long-running tasks, such as in the initial page load while the AWS lambda function warms up. It is working in the simplest cases, but not in the useful cases.

I am also learning how to split my svelte project into different components - there are a lot of components!
