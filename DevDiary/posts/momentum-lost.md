---
title: Momentum lost
templateKey: sources/templates/post.html.hbs post
date: 2023-05-13
slug: momentum-lost
---
Life gets in the way of progress, sometimes, and I haven't touched this project in several weeks. Opening it up this morning, I find that I don't even know what to work on.

So after playing around with the editor for a while, I see that I cannot edit the content of `Pages` (though thankfully `Posts` work).

Pages may have several sections, so I am rendering them in a svelte `#each` block:

```
{#each [...metadata.sections] as [key, body]}
	<AccordionItem open>
			<span slot="header">{key}</span>
			<textarea
					bind:value={body}
					name="markdown-{key}"
					id="markdown-{key}"
					class="textarea-lg mt-1 block h-[500px] w-full rounded-md focus:border-indigo-500 focus:ring-indigo-500"
					placeholder="Markdown goes here" />
	</AccordionItem>
{/each}
```
They render fine. But they are not editable, and I think I have encountered [this bug](https://github.com/sveltejs/svelte/issues/6860) - though perhaps it's just a known, accepted issue with no fix.

All of which is fine, except... I've lost momentum. I barely remember how to use svelte. Have I encountered a similiar issue before? I don't know.
