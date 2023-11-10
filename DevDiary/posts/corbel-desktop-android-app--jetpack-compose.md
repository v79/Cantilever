---
title: Corbel Desktop and Android App - Jetpack Compose
templateKey: sources/templates/post.html.hbs
date: 2023-08-26
slug: corbel-desktop-android-app--jetpack-compose
---
I'm not a UI guy. I find it very difficult to translate my mental map of a user interface into HTML and CSS. And in truth, I don't have much of a mental map to begin with.

I was getting frustrated with web development, so I decided to start work on a desktop and android app to access **Cantilever**, that I have decided to call **Corbel**. It has been a... frustrating... week.

I've taken a Jetpack Compose Multiplatform template, which in theory allows me to write a desktop, Android and iOS application all within the same codebase in Kotlin. I don't care about iOS, and Android isn't my priority, so I've only been building the desktop component.

It has been a slog. I have all all the challenges of UI development, multiplied with my unfamiliarity with the reactive, Compose, UI framework. I don't think well in a reactive way, my mind is still quite imperative. I struggle to keep track of all these _mutableStateOf_ and _remember_ and callbacks. The sample projects are either so simple as to do nothing, or so cleverly engineered that I can't make sense of what's going on.

My priority has been to build the simplest UI that will let me log in, or authenticate, with **Cantilever**. The UI is done (it's not done, but it exists and it works). So how to authenticate?

I had assumed I could use the amazon AWS Cognito java APIs to log in, but that's proving very difficult. It's no longer considered enough to send a username and password. In fact, AWS makes it difficult, perhaps impossible,  to even enable that as an option. I discovered something called SRP, a sort-of password-less authentication protocol, but it's not simple, there's no AWS API available, and I think it relies of users registering with the app, not just logging in - not something I want to consider with Corbel.

So I'm a bit stuck. My only thought now is to perform the web-based token-based authentication that **Cantilever** currently used. I'd make an HTTPS request in Corbel (via Ktor client?), then launch the Cognito hosted UI in a web browser... somehow?

Auth is hard.

UI is hard.
