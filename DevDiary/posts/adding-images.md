---
title: Adding Images
templateKey: sources/templates/post.html.hbs
date: 2023-12-05
slug: adding-images
---
This has been a long time coming. I need to add images (and other media files) to the project, so that I can generate a more interesting website.

Here are my first thoughts on how this could work:

Images will be uploaded to the source bucket (to `/sources/images/`). Details will be recorded in `/generated/metadata.json` (or maybe a specific `media.json` file). The images will then be converted/rescaled according to rules defined in `cantilever.yaml `- specifically, new versions created according to the dimensions defined the "imageResolutions" project metadata. I.E. given an upload file of "my-cat.jpg", and imageResolutions of "small": "128x128" and "large": "640x", then new versions will be saved as "my-cat-small.jpg" and "my-cat-large.jpg" in the generated/images folder.

Probably also just do a straight copy of the file to `generated/images`, so that it can be used as-is.

Ideally, I would only copy the required image resolutions to the destination web bucket. How might I do that?

1. File upload handler receives JPG image
2. Send an imageMessage to an SQS Queue
3. Queue receives the message and triggers ImageProcessor, which resizes the images and saves the various versions

At no point does Cantilever scan the markdown source code to determine which images are being requested. The only point this could happen is at the MarkdownProcessor stage, when the markdown source is converted to HTML fragments and stored in `generated/htmlFragments`.

Perhaps flexmark can help me identify the `![...](/image.jpg)` references at the phrase of converting markdown to HTML?

In my `convertMDToHTML()` function, I can ask the parser to return a list of image urls, like this:

```kotlin
 val document: Node = parser.parse(mdSource)
    document.descendants.filterIsInstanceTo(mutableListOf<Image>()).forEach {
        println("Image: ${it.url}")
    }
```

So it does seem to be possible. Is it the right thing to do? What do I do with this list? Return it to the MarkdownProcessorHandler to trigger the copy of the appropriate image files from `generated/images` to the destination bucket? Will this be safe?

> What happens if the image resolution names in `cantilever.yaml` were to change...? I'll park that thought and ignore it for now; there is probably a need for marking the project as "dirty" and requiring a full rescan and rebuild.

I can imagine that there will be other uses for `document.descendants.filterIsInstance` in the future, so I will rework `convertMDToHTML()` into a class and functions that will return much more information about the generated HTML. Then the MarkdownProcessor class can perform some actions based on the results - like copying images from `generated/images` to the destination bucket.

### Image Processing Library

I'm going to start with just the core Java AWT image functionality, but if this doesn't work I'll need a library. The AWT routines are considered slow, but given the context, that might not matter.

