---
title: Tracking progress
templateKey: sources/templates/post.html.hbs
date: 2023-03-11
slug: tracking-progress
---
There's a lot to do, and I have tried a couple of ways of keeping track of what to do next. I tend to do what interests me at the time, so the project is a little unstructured. I'm still working on the page vs post distinction, and am making good progress. I have decided to change my internal project model - I had a file called `structure.json` which kept track of all the posts in the project. My initial thought was to extend this to include pages, and templates, etc, but I have decided instead to keep them separated. `structure.json` is now `posts.json`, and there will be corresponding files for pages, templates and other elements over time.

There is a todo list, in a now - next - future kinda way - at [cantilevers.org](https://www.cantilevers.org/todo).

In the meantime, I am facing an annoying issue with HTTP PUT events and API Gateway swallowing the Content-Length header. I chose to require the Content-Length header when a PUT or POST request has no body - it felt like a useful 'sense check' to ensure the API is being used correctly.
