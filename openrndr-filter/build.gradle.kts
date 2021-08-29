import Openrndr_embed_shaders_gradle.EmbedShadersTask

plugins {
    kotlin("multiplatform")
    id("openrndr.embed-shaders")
}

val kotlinxSerializationVersion: String by rootProject.extra
val kotestVersion: String by rootProject.extra
val junitJupiterVersion: String by rootProject.extra
val kotlinLoggingVersion: String by rootProject.extra
val kotlinApiVersion: String by rootProject.extra
val kotlinJvmTarget: String by rootProject.extra

val embedShaders = tasks.register<EmbedShadersTask>("embedShaders") {
    inputDir.set(file("$projectDir/src/shaders/glsl"))
    outputDir.set(file("$buildDir/generated/shaderKotlin"))

    defaultPackage.set("org.openrndr.filter")
    defaultVisibility.set("")
    namePrefix.set("filter_")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = kotlinJvmTarget
            kotlinOptions.apiVersion = kotlinApiVersion
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js(IR) {
        browser()
        nodejs()
    }

    sourceSets {
        val shaderGlsl by creating {
            this.kotlin.srcDir("$projectDir/src/shaders/glsl")
        }

        val shaderKotlin by creating {
            this.kotlin.srcDir("$projectDir/build/generated/shaderKotlin")
        }

        shaderKotlin.dependsOn(shaderGlsl)

        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(project(":openrndr-draw"))
                implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
                api(shaderKotlin.kotlin)
            }
             dependsOn(shaderKotlin)

        }

        @Suppress("UNUSED_VARIABLE")
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation("io.kotest:kotest-assertions-core:$kotestVersion")
            }
        }
    }
}
tasks.getByName("compileKotlinJvm").dependsOn("embedShaders")
tasks.getByName("compileKotlinJs").dependsOn("embedShaders")
tasks.all {
    if (this.name == "transformShaderGlslDependenciesMetadata") {
        this.mustRunAfter("embedShaders")
    }
    if (this.name == "transformCommonMainDependenciesMetadata") {
        this.mustRunAfter("embedShaders")
    }
}