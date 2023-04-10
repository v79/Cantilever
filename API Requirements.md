# API Requirement

Work-in progress for the defined API routes

`/warm`
- GET - trigger a dummy request to keep the AWS Lambda function warmed up

`/showAllRoutes`
- GET - show a list of all the declared API routes

`/structure`
- GET - get the structure.json file
- `/rebuild`
  - GET - rebuild the structure.json file
- `/addSource`
  - POST - _not implemented_

`/project`
- `/posts`
  - GET - get a list of all the Posts
  - `/rebuild`
    - POST - rebuild the list of all the Posts
- `/pages`
- GET - get a list of all the Pages
  - `/rebuild`
  - POST - rebuild the list of all the Pages
  - `/load/{srcKey}`
  - GET - _not implemented_
  - `/save`
  - POST - _not implemented_
- `/templates`
    - GET - get a list of all the Templates
    - `/rebuild`
        - POST - rebuild the list of all the Templates

`/posts`
- `/load/{srcKey}`
- GET - load the specified Post
- `/preview/{srcKey}`
- GET - _not implemented_
- `/save`
- POST - save the Post file
- `/{srcKey}`
- DELETE - delete the Post file

`/generate`
- `/post/{srcKey}`
- PUT - regenerate the HTML for the given post
- `/page/{srcKey}`
- PUT - regenerate the HTML for the given Page
- `/template/{srcKey}`
- PUT - regenerate the HTML for all Pages with the given template


## Possible others

- `/cache` - DELETE fragments and temp files for posts, pages, etc