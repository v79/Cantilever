---
title: CSS support added
templateKey: sources/templates/post.html.hbs
date: 2023-09-24
slug: css-support-added
---
I have implemented CSS static file mechanism. Now, when a CSS file is uploaded to the `sources/static` folder, it is processed as a handlebars file and written to the `css` folder in the destination website bucket.

I have decided not to implement an editing interface for CSS files yet, as I haven't decided how and where to do it. It could be part of the larger assets management system I still have to build, or it could be a small extension to the existing *templates* editing page (because CSS and the handlebars HTML templates will likely be edited together).

Now that I have CSS support, I should actually build a theme for my generated website (at [cantilevers.org](https://www.cantilevers.org/)). But maybe that's a job for another day... I wonder if there's a pleasant-enough one-file CSS template I could grab?

Next up - navigation and pagination. I need to be able to create links between posts and pages. I'm going add new model attributes such as `@prev`, `@next` and `@parent`.
