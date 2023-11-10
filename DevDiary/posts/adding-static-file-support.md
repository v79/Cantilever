---
title: Adding static file support
templateKey: sources/templates/post.html.hbs post
date: 2023-09-18
slug: adding-static-file-support
---
For far too long this project could only produce the simplest, plainest HTML files. There was no support for adding CSS styles to the project - or indeed images or any other 'static' file content.

I have finally started work on this feature. CSS files should be uploaded to the 'sources/statics/' folder and they will be copied (or transformed?) to a 'css' folder on the destination bucket.

Editing CSS files is a long way away, and not in scope for a while. I must keep reminding myself of this.

I could process the CSS files as handlebars templates, so I could parameterise some of the CSS in the project definition file. Not sure how useful that would be. Perhaps it could be used to specify a colour theme at build time? E.g.

*In yaml:*

```yaml
attributes:
    cssColor: #ff0000;
```

*In styles.css:*

```css
h1 {
  color: {{{attributes.cssColor}}}
}
```

Even if the use-case is niche, it shouldn't be hard to implement and may have some value.
