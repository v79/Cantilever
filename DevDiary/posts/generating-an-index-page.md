---
title: Generating an index page
template: post
date: 2023-02-27
slug: -posts-generating-an-index-page
---
Something has gone terribly wrong with this source file... I can't remember what I wrote.

A website needs an index page. Previously, in _bascule_, I wrote a specific function just for generating the index page, but for _Cantilever_ I wanted something a bit more generic. I make a distinction between _posts_ - which are blog-type entries, usually with a date, growing over time - and _pages_, which are much more static things - think an "about us" or "biography" page.

marker. Each section must have a name (`#section-one`) for to be rendered.

With in place, the application will apply the Handlebars renderer to the template file, plugging in all the values. A simplified template might look like this:

```handlebars
<html>
_ head stuff here_
<title>{{title}}</title>
<body>
<div class="container">{{{ section-one }}}</div>
<aside class="column2">{{{ section-two }}}</aside>
</body>
</html>
```
This is now working - and indeed the [Cantilevers homepage](https://www.cantilevers.org/) has been built using this approach. It is generic, and not specific to index.html - well, almost.

In order to display the home page list of posts, the `index.html.hbs` template needs to have access to the `structure.json` file, which contains a complete list of all the posts on the website. This is different from how _bascule_ did it, which would essentially recompile the project model in memory to generate the index page. Further work on this is required, but for now, it works.
