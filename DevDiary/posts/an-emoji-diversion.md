---
title: An Emoji Diversion
templateKey: sources/templates/post.html.hbs post
date: 2023-03-20
slug: an-emoji-diversion
---
I've been using an emoji character in my [todo](/todo) page, a brightly-coloured 'tick' character to indicate when a task has been completed. It turns out, this is a bad idea because its causing a few bytes of my output file to be lost for each tick on the page.

I noticed that the end of my _todo_ page was being oddly truncated - a behaviour I couldn't replicate anywhere else, until I honed in on these ticks. They are multi-glyph characters, combining a basic tick plus a color modifier.

A more common example is the use of emoji variants to [display people emoji with different skin tones](https://medium.com/bobble-engineering/emojis-from-a-programmers-eye-ca65dc2acef0).

If I type the "dark skinned man" emoji - 👨🏿 - the last few characters of this blog post will just disappe

## Fixed!

The cause has been found, and as usual, it was my fault.

When writing an object to AWS S3, you must specify the number of bytes you are writing for the object. For simple text strings, the number of bytes is the number of characters, and so I was passing `content.length`. That mostly works. But these complex emoji are multi-byte characters, so the actual number of bytes required is greater than the length of the string in characters.
