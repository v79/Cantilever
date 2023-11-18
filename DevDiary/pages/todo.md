---
title: Todo
templateKey: sources/templates/about.html.hbs
#siteName: Cantilever
#author: Liam Davison
--- #body
## Current Branch

I have started work on adding navigation elements - to create next and previous links between posts, and later I'll add more page-specific elements such as links to parent pages, children, and siblings.

But to support navigation in pages, I need to resolve some long-standing issues with URL calculation and decide how I present a folder structure. And I'm really not sure how to handle this.

## Priority Features

- Static assets, CSS, images
- Editing interface for pages and templates
  - _Learn about svelte routing? Or stick to SPA?_
- API route to clear out the `generatedFiles` folder

## Further down the line

- Monitoring generation progress
- Improvements to the editing interface
- Clearing cache/generated fragments

### Ideas and nice-to-haves

Ultimately, I would like to be able to separate the 'cantilever' application (front-and-backend) with this cantilevers.org blogging website.

## Historical branches

- **index-page**
  - ✔️ Creating index.html and general support for static pages
  - ✔️ API routes to regenerate named pages
  - ✔️ API routes to regenerate pages based on templates
    - ✔️ This is missing a "page structure" file, the overall project map for pages.
  - ✔️ API routes to regenerate posts

## Bugs/Known Issues

- Starting to track bugs on [github issues](https://github.com/v79/Cantilever/issues)
- The short form of API routing does not include the necessary CORS domain headers. Need to find a way to inject that into the root of the API.
- The URL for a page seems to include the .md file extension - it should not - review all file name handling across templates, posts and pages as it's very inconsistent - *work in progress*
- There's a massive number of TODO hacks in place trying to make it all work. Introducing Pages really buggered things up.
- If I take too long to edit a page, the authentication token can expire and the only way to get a new one is log out and back in. Need to implement a refresh mechanism!