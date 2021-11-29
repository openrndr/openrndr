# OPENRNDR 0.4

[![Download](https://maven-badges.herokuapp.com/maven-central/org.openrndr/openrndr/badge.svg)](https://mvnrepository.com/artifact/org.openrndr/openrndr-core)
![Build status](https://github.com/openrndr/openrndr/actions/workflows/build-on-commit.yml/badge.svg)

A Kotlin/JVM and Kotlin/JS library for creative coding, real-time and interactive graphics. Can currently be used on Windows, macOS and Linux/x64 to create standalone graphical applications.

Basics and use are further explained in the [OPENRNDR guide](https://guide.openrndr.org).

## Repository structure

| module              | description          |
----------------------|-----------------------
| [openrndr-animatable](openrndr-animatable) | Tooling for interactive animations |
| [openrndr-application](openrndr-application) | Application and Program classes |
| [openrndr-binpack](openrndr-binpack) | Binpacking algorithm used for texture atlasses |
| [openrndr-color](openrndr-color) | Color spaces |
| [openrndr-dds](openrndr-dds) | DirectDraw Surface file (.dds) loader |
| [openrndr-demos](openrndr-demos) | A collection of small in-repository demos |
| [openrndr-draw](openrndr-draw) | Drawing primitives |
| [openrndr-event](openrndr-event) | Event classes |
| [openrndr-extensions](openrndr-extensions) | Built-in OPENRNDR extensions |
| [openrndr-filter](openrndr-filter)| Built-in filters |
| [openrndr-js](openrndr-js) | Kotlin/JS specific modules |
| [openrndr-jvm](openrndr-jvm) | Kotlin/JVM specific modules |
| [openrndr-math](openrndr-math) | Math functions and classes |
| [openrndr-nullgl](openrndr-nullgl) | Mock graphics back-end |
| [openrndr-shape](openrndr-shape) | Classes and functions for working with 2D shapes |
| [openrndr-svg](openrndr-svg) | Loading and saving SVG |
| [openrndr-utils](openrndr-utils) | Assorted utilities |

## Using OPENRNDR

You are advised to use the [OPENRNDR template](https://github.com/openrndr/openrndr-template) which provides a quick start to using the library.

OPENRNDR's Javascript/WebGL is still experimental and under development. However, if you feel like trying it you should use the 
[OPENRNDR JS template](https://github.com/openrndr/openrndr-js-template).


## Building OPENRNDR

On a system that has JDK 1.8.x or more recent installed one can run the following commands from a terminal:

```sh
cd <path-to-checkout>
./gradlew build
```

This should start the build process, which will take some time to complete.

Note that OPENRNDR does not depend on anything that is not on Maven Central, builds should be easy and predictable.

## Installing OPENRNDR as Maven artifacts

In order to use the OPENRNDR build from your applications one has to install OPENRNDR's Maven artifacts in the local Maven repository.

```sh
./gradlew -Prelease.version=0.5.1-SNAPSHOT publishToMavenLocal
```

## Community

Visit the [OPENRNDR website](https://openrndr.org) for the latest news on OPENRNDR, showcases and events 

Join us on the [OPENRNDR forum](https://openrndr.discourse.group) for questions, tutorials and showcases.

Reach us more directly on the [OPENRNDR Slack](https://join.slack.com/t/openrndr/shared_invite/zt-avkbk0as-AZEsN7kb4UNIpfmYfbAemw).
