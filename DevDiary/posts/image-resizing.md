---
title: Image Resizing
templateKey: sources/templates/post.html.hbs
date: 2023-12-21
slug: image-resizing
---
I have been working on my image processing functionality. It's taken a while, and a few stressful moments, but the system now handles image uploads correctly.

Images can be uploaded to the `sources/images` destination in S3. When that happens, a lambda function is called to generate different versions of the image according to the resolutions defined in the project `cantilever.yaml` file.

For each resolution, a new image is saved in `/sources/generated/images/::name::/resolution` - e.g a picture called "my-photo.jpg" and two image resolution definitions of "square-100x100" and "tall-100x200" will be saved to files `sources/generated/images/my-photo/square.jpg` and `sources/generated/images/my-photo/tall.jpg`.

My next step will be to add these images to `metadata.json` (or possibly a separate`images.json` file). Logically, it goes in metadata, as a new ContentNode object. But I somehow I feel images should be separate.

- I'll start with images being another ContentNode, recorded in metadata.json, but can always refactor if that doesn't work out.


### Previewing Images

For my editing environment, I want to be able to display a grid of all the images available. The images are uploaded to the source bucket, and resized versions are put into the `/generated/images` folder in the source bucket. But they are not available in the editor bucket, so I cannot just use a standard HTML `img` tag to render the image or thumbnail.

I'll need to write an API to fetch the image blobs from the source bucket. I'll make it async; it will be a good learning exercise if nothing else!

