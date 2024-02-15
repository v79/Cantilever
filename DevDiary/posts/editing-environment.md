---
title: Editing Environment
templateKey: sources/templates/post.html.hbs
date: 2024-01-21
slug: editing-environment
---
I have been rewriting my web editing front-end. I have decided to switch from the [Flowbite](https://flowbite-svelte.com/) UI framework to [Skeleton](https://www.skeleton.dev/), as I was not very satisfied with either the look-and-feel nor the functionality of Flowbite.

My first thought was to do a gradual transition, slowly replacing Flowbite with Skeleton components, but I was advised that that would be tricky. So I have embraced this as an opportunity to take all I have learned writing _sveltekit_ code, and start a new web front end project.

The upside is I have a much better-structured, more consistent code base to develop with. The downside it's taken me a week to be able to create, load, and save these posts. I haven't started with pages, templates, images or project configuration. I'm confident that I will be able to restore these reasonably quickly. I've still got the original code for reference, I'm writing code more quickly - and Github Copilot has been surprisingly effective.

I haven't settled on a new visual theme, but _Skeleton_ has a theme configuration tool which should make this a little easier.

I've also been doing a bit of sveltekit refactoring.

There is a lot to do, so I am tracking progress [on github issue 74](https://github.com/v79/Cantilever/issues/74). Of course, while fixing many things, I am also introducing new bugs!


