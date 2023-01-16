# API Requirements

## Basic Operations

`/structure`
- GET - _get current project structure json_
- PUT - _update project structure json file_

`/structure/rebuild`
- GET - _force a rebuild of the entire structure json file by rescanning all source md files_

`/structure/update`
- PATCH (or PUT) - _modify part of the structure json file_ **to what end?**

`/post`
- PUT - _create a markdown source file_

`/post/{key}`
- GET - _load the source markdown file given by the key_
- DELETE - _delete source file from the project_
- PUT (or PATCH) - _update the markdown file with the given key_

`/post/preview/{key}`
- GET - _generate and load an HTML preview of the given markdown file_

`/templates`
- GET - _get a list of all handlebars templates_
- PUT - _create a handlebar template_

`/templates/{key}`
- GET - _load the handlebar template file given by the key_
- DELETE - _delete the handlebar file given by the key_

`/publish`
- POST - _force a regeneration of all content_


## Media operations

`/media`
- GET - _get complete list of media files_
- PUT - _upload a new media file_

`/post/{key}/media`
- GET - _get list of media associated with this source file_ **maybe structure just provides this**
