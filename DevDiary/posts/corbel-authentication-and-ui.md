---
title: Jetpack Compose Theming Woes
templateKey: sources/templates/post.html.hbs
date: 2023-09-10
slug: corbel-authentication-and-ui
---
Last time, I was struggling with building a basic authentication flow for my **Corbel** desktop application. I got to work, after a few false starts, and it has been a useful learning experience.

So with authentication working, I returned to the _Jetpack Compose_ UI for the project. That lead me down a very frustrating and pointless path of learning about theming and components in Jetpack Compose which has left me feeling pretty disheartened.

Google have implemented a theme for Compose called _Material3_, which is very clever and pretty and looks good on a mobile phone. It is fairly straightforward to customise the colours, and it hooks into Android's dynamic colour system called *Material You*.

Unfortunately, to my mind, _Material3_ is not well suited to desktop apps, especially 'productivity' apps of the kind I hope _Corbel_ to become. So I started to look for alternative Jetpack Compose themes, beyond Material3.

There are, it seems, a total of three such projects on the internet:

- [Jewel, by Jetbrains](https://github.com/JetBrains/jewel) - an attempt to create the look-and-feel of the Jetbrains IDEs. This looks promising, though they admit very incomplete and under heavy development. It has a suitably corporate, enterprise-application feel to it. But it is very hard to customise - even changing the primary button colour has proven to be too big a challenge. _Jewel_ exists to suit Jetbrains' needs alone.
- [aurora](https://github.com/kirill-grouchnikov/aurora) - another promising project, with lots of desktop-grade components, and a theming system with many built-in colour schemes. I checked out the project, but it didn't even build for me. I also found the naming to be weird... Instead of a `Checkbox` you create a `CheckboxProjection` and then call `.project()` to create the box. The [Content/Presentation/Projection](https://github.com/kirill-grouchnikov/aurora/blob/icicle/docs/component/ComponentProjections.md) model approach does not feel very 'Compose' to me.
- [compose-macos-theme](https://github.com/Chozzle/compose-macos-theme) - as its name suggests, an attempt to create a macOS theme for compose. The project hasn't been updated in over a year, and I'm really not an Apple user so the theme is not right for me.

### Why so hard?

So why so few theming projects for Compose? In the early 2000s theming was everywhere in desktop computers - from _WinApp_ to _Windows Media Player_, from every Linux window manager and toolkit, to commercial products like _Windows Blinds_, custom theming was ubiquitous. Personalisation was key.

Of course, that faded, as all fads do, and now the modern sensibility is flat user interfaces, no more skeuomorphism, and a few sensible-by-default configuration options. There's a light theme and a dark theme, and Windows and macOS allow you to choose a core colour that the rest of the UI is automatically themed and coloured appropriately. That's OK. But Jetback Compose does not hook into the native UI frameworks, and so there's no integration with a native Windows (or macOS, or Linux) look-and-feel. There's no automatic pick-up of that native core colour, for sure, and _Jewel_ certainly makes it hard for me to change a colour too.

Back to the question in hand - why so few theming projects for Compose? Well, a compose theme is not merely a set of colour and font choices, no matter how Google likes to present it. A Compose theme is a complete implementation of a set of components - text fields, buttons, menus, tab bars and so on. The _Material3_ `FilledButton` component is unique to Material3 - you cannot use it in _Jewel_ or _Aurora_, it simply does not exist there. So these look-and-feel projects have to provide complete implementations of all the core desktop widgets. They may all be based on an underlying Compose `Button` component, but there is a lot of work to create a coherent 'desktop' theme. It is not a matter of chosing some colours, fonts, and border radii.

(This relates to why there's no official `Table` component in any Jetpack Compose theme either - a modern table is a complicated, flexible beast, Google clearly feel it is not a priority for Android, and nobody else has committed the effort to building one.)

I do not expect Jetpack Compose to provide a fully native UI experience - as others have argued, native OS UI experiences are very hard to recreate, and there's broad acceptance of web-inspired and non-native UI on desktop apps now. But I would argue that *Material3* is an inherently mobile UI experience, and does not suit corporate, business desktop application experiences.

### So what to do?

Perhaps this detour down desktop app development has been a mistake. There is still a huge amount to do for _Cantilever_ proper - not least to start implementing image, CSS and other assets into the project. Right now, there is no support for any of these. I can't even show any of these themes on pages generated by Cantilever.

I do feel, however, that I'd have a more coherent editing experience on a desktop app than the current web application, and my frustrations with web dev will remain. I need to make a decision - *Material3*, _Jewel_, or abandon the desktop for now?
