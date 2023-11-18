---
title: Page Model
templateKey: sources/templates/about.html.hbs
--- #body
There are a number of values which are always available to page and post templates. Some are defined by the project, some are specific to the page type (page or post), and some are calculated.

It is important to understand the difference between **Pages** and **Posts**.

- **Posts** are traditional blog posts - they always have a date, they are sequential, you can navigate between them with next and previous links.
- **Pages** are more like static content. The have fixed URLs, there will likely be far fewer Pages than Posts. Pages don't have a previous-next relationship to each other, but can be organised into folders.

Some values are prefixed with the `@` symbol - these allow you to link between pages, posts, and other navigation elements.

### Project Elements

The following are defined in `cantilever.yaml` and are prefixed with `project` (e.g. to display the author name, type `{{ project.author }}`.


| Key            | Notes                                             |
| :------------- | ------------------------------------------------- |
| projectName    |                                                   |
| author         |                                                   |
| dateFormat     | Default date format for project, e.g. dd/MM/yyyy  |
| dateTimeFormat | Default date & time format, e.g. HH:mm dd/MM/yyyy |

### Common Elements

These elements are common to `Pages` and `Posts`.


| Key   | Notes                                                                 |
| ----- | --------------------------------------------------------------------- |
| title | Every page or post will have a title                                  |
| URL   | Calculated URL, more accurately the URI which follows the domain name |

### Post Elements

These elements are specific to `Posts`.


| Key    | Notes                                                                                                                                                             |
| ------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| body   | The complete content of the blog post as rendered HTML. Include it with the triple-brace`{{{ body }}}` notation.                                                  |
| date   | All Posts will have a date.                                                                                                                                       |
| @prev  | Returns the previous post to this one, i.e. post that precedes this.For instance, in the`posts` template, use this to link to the previous post:`{{@prev.title}}` |
| @next  | Returns the next post which follows this one                                                                                                                      |
| @first | Returns the very first post in the project                                                                                                                        |
| @last  | Returns the latest, most recent post in the project                                                                                                               |

### Page Elements

These elements are specific to `Pages`.


| Key          | Notes                                                                                                                               |
| ------------ | ----------------------------------------------------------------------------------------------------------------------------------- |
| key          | Raw file name for the page                                                                                                          |
| lastModified | Date and time the page source file was updated                                                                                      |
| pages        | **NOT IMPLEMENTED** A tree structure which will allow you to access other pages and their metadata (but notbodycontent)             |
| posts        | **NOT IMPLEMENTED** A list of all the Posts in the project, so that you can access their titles and metadata (but not body content) |

Helper Functions

There are a handful of functions that let you control or modify how content is rendered. These are implemented as Handlebars functions.


| Function  | Purpose                                                        | Example                            | Result                                                |
| --------- | -------------------------------------------------------------- | ---------------------------------- | ----------------------------------------------------- |
| upper     | Convert to upper-case                                          | `{{ upper project.siteName }}`     | CANTILEVER                                            |
| slugify   | Convert a string into something safe for a URL                 | `{{ slugify title }}`              | "Project Model" becomes "project-model"               |
| localDate | Format a date as a string                                      | ` {{ localDate lastUpdatedDate }}` | "2023-10-21T07:23:23:8+8207519Z" becomes "21/10/2023" |
| take      | Take the first_n_ elements from a list and loop to render them | `{{ take posts "5" }}`             | Loop through the first five most recent posts         |
|           |                                                                |                                    |                                                       |