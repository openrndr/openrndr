[![Build Status](https://travis-ci.org/openrndr/openrndr.svg?branch=master)](https://travis-ci.org/openrndr/openrndr)
[![Download](https://api.bintray.com/packages/openrndr/openrndr/openrndr/images/download.svg) ](https://bintray.com/openrndr/openrndr/openrndr/_latestVersion)

# OPENRNDR

A Kotlin/JVM library for creative coding, real-time and interactive graphics. Can currently be used on Windows, macOS and Linux/x64 to create stand alone graphical applications.

# Usage

A very basic OPENRNDR application looks like this:

```kotlin
import org.openrndr.Application
import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.draw.loadImage

/**
 * This is a basic example that shows how to load and draw images
 */
class Image001 : Program() {
    override fun setup() {
        val image = loadImage("file:data/images/test_pattern.png")
        extend {
            drawer.image(image)
        }
    }
}

fun main() = Application.run(Image001(), Configuration())
```
Please have a look at our [application template](https://github.com/openrndr/openrndr-gradle-template) and our [tutorial repository](https://github.com/openrndr/openrndr-tutorials) for more usage examples.

Basics and use are further explained in the [OPENRNDR guide](http://guide.openrndr.org) and more project information can be found on our [website](http://openrndr.org) 
