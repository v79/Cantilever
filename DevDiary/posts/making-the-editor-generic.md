---
title: Making the editor generic
templateKey: sources/templates/post.html.hbs
date: 2023-04-10
slug: making-the-editor-generic
---
I am continuing to improve the interface to handle Pages and Posts. It's slow work, and not very interesting, but it is necessary. Until I have a user interface for all these elements, I am relying on manually making HTTP REST calls to trigger regeneration of content.

In fact, I am realising that I am missing several key HTTP routes - and that my current route structure may not be the best. In trying to plan the routes, I find myself thinking I need a swagger hub type tool. But that's another distraction, another blind alley to lead myself down.

I've realised I've been using verbs in my routes, like `/posts/save/`, which is not idiomatic REST. Instead of PUT `/posts/save/` it should just be POST `/posts/` (or better still, POST `/post` for a new item, and PUT `/post/{srcKey}` to update an existing one.)

I'll get there, but for now it's a bit of a mixture of styles.

These decisions, and working on the UI, means that progress has been very slow recently.
