---
title: Image Testing
templateKey: sources/templates/post.html.hbs
date: 2023-12-30
slug: image-testing
---
The past month has been focused on adding image uploads to Cantilever - uploads, resizing, and most recently a gallery view of available images.

But these images cannot actually be displayed in a post or a page. It's time to change that.

The markdown instruction to display an image is quite simple:

```markdown
![Alt text](/image/path/image.jpg "Image description")
```

Simple enough. But when an image is uploaded, it is stored in the `sources` S3 bucket - not the website bucket, where it's really needed. One option would simply be to copy all the uploaded images from the source to the destination, but I'd like to be a little more clever than that.

When an image is uploaded, multiple versions of are created, according to the resolutions defined in the project. I would like only the images actually referred to in a markdown file to be copied over, so I'm going to try to intercept the markdown processing process and work out what images, at what resolutions, are required for each page or post.

If this works, then a picture of a bridge will appear below!

![AI generated image](/images/Oilpainting-Bridge/big-square.jpg "Generative AI created this picture of a bridge for me")

One open question: who is responsible for copying files to the destination bucket? The analysis has to happen in the `MarkdownProcessor` lambda, but until now, it has not had write-access to the destination bucket. Perhaps I need a new SQS message type - and other queue?

I quite like keeping the MarkdownProcessor pure - so I'll try to pass the image copying responsibility to the `ImageProcessor` lambda (it will, of course, need access to both source and destination buckets).

Further image tests will be on the [dedicated image testing page](/image-testing/). Well, they would be, but I've found a bunch of bugs unrelated to images and I think it's time to close the image upload branch, work on some bug fixes, and return to images at another time.
