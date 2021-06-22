# OPENRNDR 0.4 (development branch)

A Kotlin/JVM and Kotlin/JS library for creative coding, real-time and interactive graphics. Can currently be used on Windows, macOS and Linux/x64 to create stand alone graphical applications.

Basics and use are further explained in the [OPENRNDR guide](https://guide.openrndr.org).

## Community

Visit the [OPENRNDR website](https://openrndr.org) for the latest news on OPENRNDR, showcases and events 

Join us on the [OPENRNDR forum](https://openrndr.discourse.group) for questions, tutorials and showcases.

.. or if you prefer a more direct and chatty way of conversation talk to us the [OPENRNDR Slack](https://communityinviter.com/apps/openrndr/openrndr)

## Using OPENRNDR

You are adivsed to use the [OPENRNDR template](https://github.com/openrndr/openrndr-template) which provides a quick start to using the library.

Instructions for using the Kotlin/JS version of OPENRNDR are missing. Some examples will be published soon.

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
