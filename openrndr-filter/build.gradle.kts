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


        val shaderKotlin by creating {
            this.kotlin.srcDir("$projectDir/build/generated/shaderKotlin")

        }

        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                implementation(project(":openrndr-draw"))
                implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
                api(shaderKotlin.kotlin)
            }
            this.dependsOn(shaderKotlin)
        }
        //commonMain.dependsOn(shaderKotlin)

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

val embedShaders = tasks.register<EmbedShadersTask>("embedShaders") {
    inputDir.set(file("$projectDir/src/shaders/glsl"))
    outputDir.set(file("$buildDir/generated/shaderKotlin"))
    defaultPackage.set("org.openrndr.filter")
    defaultVisibility.set("")
    namePrefix.set("filter_")
}.get()

//sourceSets.getByName("shaderKotlin").com()//    (embedShaders)
//tasks.getByName("compileKotlinJvm").dependsOn(embedShaders)
//tasks.getByName("compileKotlinJs").dependsOn(embedShaders)
//tasks.getByName("compileKotlinMetadata").dependsOn(embedShaders)
//tasks.getByName("jvmSourcesJar").dependsOn(embedShaders)
//tasks.getByName("sourcesJar").dependsOn(embedShaders)
//tasks.getByName("jsMainClasses").dependsOn(embedShaders)
//tasks.getByName("jvmMainClasses").dependsOn(embedShaders)
