import org.jetbrains.dokka.gradle.DokkaTaskPartial
import java.net.URL

plugins {
    kotlin("multiplatform") version "1.5.31"
    id("org.jetbrains.dokka") version "1.5.30"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.5.31"
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    linuxX64("linux")
    js {
        browser()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
            }
        }
    }
}

// used by submodules
project.ext.set("kotlinApiVersion", "1.5")
project.ext.set("kotlinJvmTarget", "1.8")
project.ext.set("kotlinLanguageVersion", "1.5")
project.ext.set("kotlinVersion", "1.5.31")
project.ext.set("kotlinLoggingVersion", "2.0.6")
project.ext.set("kotlinxSerializationVersion", "1.2.1")
project.ext.set("kotlinxCoroutinesVersion", "1.5.0")
project.ext.set("lwjglVersion", "3.2.3")
project.ext.set("javacpp_version", "1.5.4")
project.ext.set("ffmpeg_version", "4.3.1-${project.ext.get("javacpp_version")}")
project.ext.set("spekVersion", "2.0.15")
project.ext.set("kluentVersion", "1.65")
project.ext.set("jsoupVersion", "1.13.1")
project.ext.set("kotestVersion", "4.4.3")
project.ext.set("junitJupiterVersion", "5.7.1")

// See https://kotlin.github.io/dokka/1.5.30/user_guide/gradle/usage/
tasks.dokkaHtml {
    val ignore = listOf(
        ".*gl3.*",
        ".*generated.*",
        ".*internal*",
        ".*artifex*",
        "io\\.lacuna.*"
    )
    println("ignore: $ignore")
    moduleName.set("openrndr")
    dokkaSourceSets {
        configureEach {
            skipDeprecated.set(true)
            skipEmptyPackages.set(true)
            includes.from(files("Module.md"))

            ignore.forEach {
                perPackageOption {
                    matchingRegex.set(it)
                    suppress.set(true)
                }
            }

            // sourceLink to be removed?
            val sourceSetName = name
            println(sourceSetName)

            sourceLink {
                localDirectory.set(file("$sourceSetName/src/commonMain/kotlin"))
                remoteUrl.set(
                    URL(
                        "https://github.com/openrndr/openrndr/blob/master/src/main/kotlin"
                    )
                )
                remoteLineSuffix.set("#L")
            }
        }
    }
}


listOf(
    "openrndr-animatable",
    "openrndr-draw",
    "openrndr-dds",
    "openrndr-filter",
    "openrndr-core",
    "openrndr-math",
    "openrndr-color",
    "openrndr-event",
    "openrndr-shape",
    "openrndr-utils",
).forEach {
    project(":$it") {
        apply(plugin = "org.jetbrains.dokka")
        tasks.withType<DokkaTaskPartial>().configureEach {
            dokkaSourceSets {
                configureEach {
                    includes.from("/home/funpro/OR/openrndr/Module.md")

                    sourceRoots.firstOrNull()?.also { path ->
                        val relPath = rootProject.projectDir.toURI()
                            .relativize(path.toURI())
                        val github = URL(
                            "https://github.com/openrndr/" +
                                    "openrndr/blob/master/$relPath"
                        )

                        sourceLink {
                            localDirectory.set(path)
                            remoteUrl.set(github)
                            remoteLineSuffix.set("#L")
                        }

                    }
                }
            }
        }
    }
}

allprojects {
    group = "org.openrndr"
    repositories {
        mavenCentral()
    }
}

val multiplatformModules = listOf(
    "openrndr-application",
    "openrndr-filter",
    "openrndr-event",
    "openrndr-math",
    "openrndr-color",
    "openrndr-utils",
    "openrndr-shape",
    "openrndr-binpack",
    "openrndr-animatable",
    "openrndr-draw",
    "openrndr-webgl",
    "openrndr-extensions",
    "openrndr-dds",
    "openrndr-openal",
    "openrndr-gl3",
//        "openrndr-tessellation",
    "openrndr-svg",
    "openrndr-kartifex"
)
