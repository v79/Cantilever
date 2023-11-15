---
title: About Cantilever
templateKey: sources/templates/about.html.hbs
#siteName: Cantilever
#author: Liam Davison
--- #body
**Cantilever** (or possibly _Cantilevers_), is an attempt to build a static website generator in the mold of _jeykll_, but running entirely on serverless infrastructure in the cloud. It was born from my frustrations with my earlier attempt at a static generator, [Bascule](https://www.liamjd.org/bascule.html), which I have used to build my own website.

One of the implementation principles of the project is that website can be rebuilt from just the source files - the handlebars templates, and the markdown files. There is no co-ordinating database. There are project models generated as json files, but they are not editable and can be rebuilt at any time from the sources.

Whether this principle is a sound one is open to debate. _Bascule_ had to run without a database, but _Cantilever_ could make use of a cloud database, perhaps a document DB. But, for now, I will stick to the principle.