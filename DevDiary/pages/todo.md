---
title: Todo
templateKey: sources/templates/about.html.hbs
--- #body
## Current Branch

A new web front end, replacing *Flowbite* with *Skeleton*.

I should completely rewrite this TODO page. And I should commit to keeping it more up-to-date.

## Priority Features

- Static assets, CSS, images
- Supporting multiple projects and multiple destination buckets
- API route to clear out the `generatedFiles` folder

## Further down the line

- Monitoring generation progress
- Clearing cache/generated fragments

### Ideas and nice-to-haves

Ultimately, I would like to be able to separate the 'cantilever' application (front-and-backend) with this cantilevers.org blogging website.

## Bugs/Known Issues

- Starting to track bugs on [github issues](https://github.com/v79/Cantilever/issues)
- ~~The short form of API routing does not include the necessary CORS domain headers. Need to find a way to inject that into the root of the API.~~
- ~~The URL for a page seems to include the .md file extension - it should not - review all file name handling across templates, posts and pages as it's very inconsistent - *work in progress~~*
- There's a massive number of TODO hacks in place trying to make it all work. Introducing Pages really buggered things up.
- If I take too long to edit a page, the authentication token can expire and the only way to get a new one is log out and back in. Need to implement a refresh mechanism!