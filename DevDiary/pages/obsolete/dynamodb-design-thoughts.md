---
title: DynamoDB Design Thoughts
templateKey: sources/templates/about.html.hbs
--- #body
I am exploring using DynamoDB as a replacement for pages.json, posts.json and so on. I am trying to understand the DynamoDB approach to documents and indices.

It seems that you really need to think hard about your *Local Secondary Indices* when you create the table, as you cannot create new LSIs once the table has been created. You can create *Global Indices* after the table has been created, though that essentially as a new 'column' to the database. You can't turn an existing 'column' into an index, as there is no guarantee that that 'column' would exist for all records.

Attributes that I'll need:

- **key**: the s3 object key
- **type**: page, post, folder, template, media etc
- lastUpdatedDate: maybe, I could just get this from the S3 object
- **date**: this is required for posts, but optional for pages and null for all others?

Attributes I might want:

- For posts:
  - next: the key of the next post
  - prev: the key of the previous post
- For pages:
  - isStart: indicates that this page is the 'index' for a given folder
  - parent: the key of the parent folder
- For templates:
  - sectionsNames: list of defined sections in the template

I have assumed that the primary key will be the (s3 object) **key**, with a secondary sorting/range index of the **type**. But given that the S3 object key is globally unique, that might not be the right choice? I will want a *Local Secondary Index* of the **date** attribute, so that I can sort by and query on date ranges. I think.

I need to decide what sort of queries or scans, and other operations, I'll need to do.

Operations:

- `addItem(key, type, date?, otherAttributes[])` - add a new page, post, etc
- `getItem(key)` - get all the attributes for a known, given object
- `getAll(type)` - get all pages, all posts etc
- `getAll(type, limit)` - get the `limit` most recent items - may actually just be `getPostsAfter(date)`or `getPostsAfter(date, limit)`?
- `updateItem(key, type, date? otherAttributes[])` - update an existing page, post, etc
- `deleteItem(key)`
- *anything else?*

The problem I am trying to solve: when rending a post, how do I know what it's previous and next posts are? If those are attributes of the post, then how are they updated? It's almost a linked list:

```json
{ "posts": [ {"key": "a-post", "date": "5000", "next": "another-post", "prev": ""}, {"key: "another-post", "date": "5500", "next": "", "prev": "a-post" } ] }
```

So the big challenge is - how do I maintain these next/previous links when adding, deleting and updating posts?