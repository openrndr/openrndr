# Contributing to OPENRNDR

## I have an issue or request

Before you file a new issue, take a quick look through
the [existing issues](https://github.com/openrndr/openrndr/issues) to make sure it isn't a known
problem. If you can't find anything relevant to your situation, just file a new issue and fill out
the issue form.

## I want to contribute code

While building the project is fairly trivial (detailed in the README),
more [complex build setups](https://github.com/openrndr/openrndr/wiki/Building-OPENRNDR) are
possible to create even faster feedback loops as you'll be able to test your own changes to OPENRNDR
on your [openrndr-template](https://github.com/openrndr/openrndr-template).

Feel free to ask for more advice
on [our forums or Slack](https://github.com/openrndr/openrndr#community).

### Style guide

The style guide is fairly minimal, but it helps to spell it out regardless:

* **Code**: There currently isn't a strict style guide on the code, just try to follow the IntelliJ
  default formatting for Kotlin.
* **Commit messages**: It's preferable to make your commit messages relevant to the changes you
  make. Although it's not unusual for the commits to be squashed when merging a pull request.

## I want to contribute to the documentation

There are various places where you can contribute without writing code. It will be greatly
appreciated by others trying to learn about OPENRNDR.

### Guide

The [guide](https://guide.openrndr.org/) is the first contact with OPENRNDR for most users.
[Learn how to work on the guide](https://github.com/openrndr/openrndr-guide/blob/dev/contributing.md).

### API page

The [API page](https://api.openrndr.org/) needs some love too. The content is automatically
extracted from comments written in OPENRNDR's source code. It goes like this:

1. Fork the [OPENRNDR repo](https://github.com/openrndr/openrndr/), then clone your fork (so you
   have a copy on your computer) and get familiar with OPENRNDR.
2. Find an undocumented section at https://api.openrndr.org you want to explain.
3. Find the corresponding Kotlin file in your cloned repo and add missing comments. Read about
   the [suggested style](https://developers.google.com/style).
4. Generate the API website locally to verify your changes look correct by running the following
   commant: `./gradlew dokkaHtmlMultiModule -Dorg.gradle.jvmargs=-Xmx1536M`. This will create the
   html documentation under `build/dokka/htmlMultiModule/`.
5. Open the `build/dokka/htmlMultiModule/index.html` in your web browser. If something looks off
   tweak your comments.
6. To continue improving the API go back to step 3, otherwise send a Pull Requests from your fork.

## I want to contribute demos

Small programs can help others understand how features works. There are various locations where one
can find such demos:

- [Core OPENRNDR demos](https://github.com/openrndr/openrndr/tree/master/openrndr-demos/src/main/kotlin) (
  new location)
- [Core OPENRNDR demos](https://github.com/openrndr/orx/tree/master/openrndr-demos/src/demo/kotlin) (
  old location)
- orx demos. They are found in various subfolders corresponding each orx, for
  example [this one](https://github.com/openrndr/orx/tree/master/orx-jumpflood/src/demo/kotlin) for
  orx-jumpflood.

Ideally demos are small, limited in scope and present a new aspect or usage of a core feature or
orx. Feel free to send pull requests with your demos.
