package org.openrndr.convention

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("multiplatform")
}
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    js {
        browser()
        nodejs()
//        compilerOptions {
//            sourceMap = true
//            sourceMapEmbedSources = org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_ALWAYS
//        }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }
    sourceSets {
        create("webTest") {

        }
        create("webMain") {
            dependencies {
                implementation(libs.findLibrary("kotlin-logging").get())
            }
        }
    }
}

tasks.withType<KotlinCompilationTask<*>> {
    compilerOptions {
        apiVersion.set(
            org.jetbrains.kotlin.gradle.dsl.KotlinVersion.valueOf(
                "KOTLIN_${
                    libs.findVersion("kotlinApi").get().displayName.replace(
                        ".",
                        "_"
                    )
                }"
            )
        )
        languageVersion.set(
            KotlinVersion.valueOf(
                "KOTLIN_${
                    libs.findVersion("kotlinLanguage").get().displayName.replace(".", "_")
                }"
            )
        )
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}
