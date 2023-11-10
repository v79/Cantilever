---
title: Navigation Between Pages and Posts
templateKey: sources/templates/post.html.hbs post
date: 2023-10-20
slug: basic-pagination
---
This one has been a lot more work than I first anticipated. I have been adding navigation and hierarchy to posts and pages. First, I added next, previous, first and list links for posts - this was relatively straightforward, and has been in place for a while now.

There are special variables you can add to the post template - `@first, @prev, @next` and `@last`, which allow posts to link to each other.

Adding hierarchy - a folder structure - to Pages has been much more complicated, and has lead to a lot of rework of the underlying model for Pages.

Previously, there was just a flat list of pages, but in order to add a structure, I've had to add the concept of Folders. All content is saved in AWS S3 buckets, where, technically speaking, there are no folders - objects simply have a unique key and if that key includes the '/', you can pretend they are organised into folders.

My implementation is pretty rudimentary, but I think it is enough to give a flavour of page navigation and a folder structure. I would like to be able to add help variables such as `@children` to list all the child pages of a page (though that is tricky without first defining a 'start' or 'root' Page for any given folder). `@siblings` may also be possible, though perhaps less useful?
