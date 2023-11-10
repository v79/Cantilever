---
title: Typescript, Types, Classes and Interfaces - A Struggle
templateKey: sources/templates/post.html.hbs post
date: 2023-04-05
slug: typescript-types-classes-and-interfaces-a-struggle
---
I haven't been working on the project for a while - after a busy few days of work, I've taken some annual leave and enjoyed a trip to London. London can be a fun place to visit, but I couldn't possibly live there. Too busy, too expensive, too much time spent underground. It was almost a shock to return to my quiet Edinburgh village though - perhaps too quiet?

## Typescript frustrations

The last thing I tried to do with _Cantilever_ was to implement the `Page` list UI, similar to the `Post` list UI. So similar, in fact, that I wanted to make the list component generic enough to handle `Page`, `Post` and `Template`. To that end I made some changes to the underlying typescript model, to extract out some common elements.

For instance, a `Post` will have a `title`, `srcKey`, `templateKey`, `url`, `lastUpdated`, and a `date`. A `Page` is very similiar - it has the same values, but no `date`, and two additional values which are unique to a `Page`.

So I tried to create a set of Typescript types which would reflect this:

```typescript
export interface MarkdownItem {
	title: string;
	srcKey: string;
	templateKey: string;
	url: string;
	lastUpdated: Date;
}

export interface Post extends MarkdownItem {
	date: Date;
}

export interface Page extends MarkdownItem {
	attributeKeys: Set<string>;
	sectionKeys: Set<string>;
}
```

Superficially, that worked. But when I tried to use them in my `sveltekit` components, they didn't work as I had hoped. For instance, in my Page or Post list, I'd like to show the date property. Pages don't have dates, but they do have a "last updated" date value I could use instead. So my hope was to write something like this:

```typescript
function getDate(item: MarkdownItem): Date {
		if (item instanceof Post) return item.date;
		if (item instanceof Page) return item.lastUpdated;
		return new Date();
	}
```

No go. I can't do this type of type-checking in Typescript. In a _real_ strongly typed language like Java or Kotlin, it would be simple and obvious to do this. But not in Typescript.

I need to learn more about this weekly typed type-system, or I need to accept lots of duplicate code in my project where I am handling similiar but distinct things like `Page` and `Post`.

## Update

I was able to solve the typing issue with javascript classes and interfaces; I suppose it makes sense but I had been burnt by classes before and I instinctively avoid them.
