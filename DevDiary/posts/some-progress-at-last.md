---
title: Some progress at last
templateKey: sources/templates/post.html.hbs
date: 2023-08-11
slug: some-progress-at-last
---
Over the past few days I've pushed through some of the blockers. I was working on this project so infrequently, I had to remind myself where I was each time I started. I was working on editing Templates, and triggering regeneration of pages and posts when a template is changed.

I don't automatically regenerate all pages/posts when a template is saved; instead, I've added a Regenerate button.

That's now working. But my code is a hodgepodge of hacks and TODO statements. I will need to stop and take stock of the project, and work on clearing some of this technical debt. I'm tempted to start working on an Android mobile interface for this, or even a Windows desktop application. But that would be a distraction from the more meaningful work.

I am also, sadly, having some second thoughts about the project architecture. I increasingly feel that the no-database approach is going to become a weakness, not a strength. If I did put a database underneath this project, it would be in the cloud, of course, and would probably be a NoSQL document database. That does feel like admitting defeat. However, the text _json_ cache files I generate to manage lists of pages, posts, templates, and eventually media, are essentially a flat-file database with none of the benefits of a database.

Until I come to some decision on that, I'm going to try to work my way through the many, many `TODO` statements in my code...
