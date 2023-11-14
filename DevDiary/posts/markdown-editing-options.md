---
title: Markdown editing
templateKey: sources/templates/post.html.hbs
date: 2023-08-19
slug: markdown-editing-options
---
I've long been looking for a simple markdown editing tool to integrate into this application. There are many Javascript-based markdown editors out there, some very sophisticated indeed. Some even cost money. I was looking for a fairly basic editor which has a built-in preview of the formatted text.

One which would render **bold** text as I type the asterix characters, giving me a a WYSIWYG-lite editing experience.

And crucially, I needed an editing component which integrates nicely with svelte. My research came up with three options:

* [ByteMD](https://bytemd.js.org/playground/) - This editor was buggy, and I had to make build configuration changes for it even to run. When I attempted to install a basic plugin, it stopped working entirely.
* [Milkdown](https://milkdown.dev/playground) - This one looked nice - but really doesn't play nice with svelte. It does not support the key svelte binding mechanism.
* [Vditor](https://github.com/Vanessa219/vditor) - I was in the deep depths of the internet when I found this one - largely because it's a Chinese-language project. But it integrates well with svelte, it supports most the features I need, and is reasonably configurable. If it had a plain-text (no markup) view it would be even better, but for now, this is the editor I am choosing for the project. There is [English language documentation]([https://](https://github.com/Vanessa219/vditor/blob/master/README_en_US.md)) available.

As *Vditor* does not support a raw editing mode, I will try putting a raw textarea below this editor, for now. *Update:* it didn't work. Still, I've got something better than I had before.

### Page editing

Editing Posts is fine. Pages are a little more complicated, as they may be comprised of a number of different 'fragments'. I've made the corresponding changes to the page editing interface, but something odd is happening when I try to type in the editor. A few characters appear... then they disappear and the cursor jumps to the top of the page. Perhaps the `Viditor` component does not like appearing on a page multiple times? That could be a problem...

I now think that's fixed, by 'bubbling up' the update event and handling it in the caller/parent.

The other major problem with page editing is that I can no longer create new pages, because I need to choose a template when creating a new page and there's no UI for this.

