---
title: Generating an index page
templateKey: sources/templates/post.html.hbs post
date: 2023-02-27
slug: posts-generating-an-index-page
---
Something has gone terribly wrong with this source file... I can't remember what I wrote.

A website needs an index page. Previously, in _bascule_, I wrote a specific function just for generating the index page, but for _Cantilever_ I wanted something a bit more generic. I make a distinction between _posts_ - which are blog-type entries, usually with a date, growing over time - and _pages_, which are much more static things - think an "about us" or "biography" page.

Whereas a _post_ will really just contain one block of content, a _page_ may be split into several parts. So I need a way of representing this in a markdown file. I've decided on a syntax which slightly abuses markdown, but is largely working. Here is an example:

```markdown
---
templateKey: sources/templates/post.html.hbs page
#author: Liam
--- #section-one
This is the body of the page, the main section if you well.
--- #section-two
This could be an aside, or a list of links, or any other valid HTML block.
```

I've used the `---` marker to split the page into sections, and each section must have a name (`#section-one`) for to be rendered. A page (or a post, or any other file) must have a _template_ in the first, unnamed section. After which you can add custom attributes, identified with the '#' marker, which will be sent directly to the Handlebars model.

With in place, the application will apply the Handlebars renderer to the template file, plugging in all the values. A simplified template might look like this:

```handlebars
<html>
_ head stuff here_
<title>{{title}} by {{author}}</title>
<body>
<div class="container">{{{ section-one }}}</div>
<aside class="column2">{{{ section-two }}}</aside>
</body>
</html>
```
This is now working - and indeed the [Cantilevers homepage](https://www.cantilevers.org/) has been built using this approach. It is generic, and not specific to index.html - well, almost.

In order to display the home page list of posts, the `index.html.hbs` template needs to have access to the `structure.json` file, which contains a complete list of all the posts on the website. This is different from how _bascule_ did it, which would essentially recompile the project model in memory to generate the index page. Further work on this is required, but for now, it works.

## And the terribly wrong thing...?

All those triple-dash separators inside the sample markdown block where playing havoc with my metadata parser. I may need to consider a different separator. Something weird. Maybe ▶�