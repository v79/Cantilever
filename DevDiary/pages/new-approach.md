---
title: New Approach
templateKey: sources/templates/about.html.hbs
--- #body
All markdown content is a **fragment**:

- Each fragment is in a `.md` file, which does not contain any frontispiece metadata
- Each fragment will have a corresponding `.yaml` metadata file **OR** a corresponding entry in a database, ~~probably a AWS DynamoDB serverless document database~~. (The `.md` content could be in the DB too...).
  - I've investigated DynamoDB and I don't think it's the right fit for me - experimenting with the service, trying to build a model that would represent my content, felt tricky. I had naively assumed that a schemaless DB would be easier to get started with, but I think the need to think about partition keys and sort indices up front makes them less flexible than SQL, at least with my current level of (in-)experience. I found the API tricky to follow.
- There's no inherent different between a `page` and a `post`, but the template may apply different rules and metadata requirements.

As all markdown content is a fragment, there is no *presumption* that a markdown file corresponds to a final generated web page. So how to define a page (or blog post?). The front-end will always present a "New page" option. Only pages will have a corresponding `.yaml` metadata. This means that common shared fragments do not have metadata. How do we keep track of those?

Fragments are automatically converted to HTML chunks and stored in S3 `generated` folder. But we don't automatically render the template to the final HTML file.

Expected metadata:


| Name        | Example              | Notes                                                                        |
| ----------- | -------------------- | ---------------------------------------------------------------------------- |
| title       | This is my blog post |                                                                              |
| lastUpdated |                      | automatically generated based on the file                                    |
| date        | 04/11/2023           | format will need to be defined in the project settings                       |
| uri         | /my-blog-post        | optional, overrides the template-driven default                              |
| isStart     | true                 | indicates that this page should be the index.html for a URI path ending in / |
| isPage      | true                 | indicates that this fragment is the primary content for a page?              |

Continue to use Handlebars templates for rendering final output. As with markdown, there will be no metadata embedded in the template, there will be separate `template.html.hbs` and `template.html.yaml` files.

The page model for the template will consist of the page metadata, and the bodies for all the associated fragments. Template metadata will specific the (default) URL generation scheme for final output.

`pages.json` will be regenerated with every file upload/new page creation, but it won't do it by scanning all the files. Instead, I will just fetch the file, deserialize it, insert the new entry (or update existing one) and then serialize it again. `pages.json` will be a complete record of all items, not just pages, and each entry will have attributes for `next`, `prev`, `parent` and so on. These may be null/empty strings. When I insert or update, I'll be sure to create and update these links. It could be a LinkedList or Map or SortedHashSet data structure?