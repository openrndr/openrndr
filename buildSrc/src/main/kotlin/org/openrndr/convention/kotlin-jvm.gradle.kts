package org.openrndr.convention

import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val libs = the<LibrariesForLibs>()

plugins {
    java
    kotlin("jvm")
    id("maven-publish")
    id("org.openrndr.convention.dokka")
}

repositories {
    mavenCentral()
}

group = "org.openrndr"

dependencies {
    components {
        for (module in LwjglModules.all) {
            withModule<LwjglRule_gradle.LwjglRule>("org.lwjgl:$module")
        }
    }
    implementation(libs.kotlin.logging)
    implementation(libs.kotlin.stdlib)
    testImplementation(libs.kotlin.test)
}

kotlin {
    jvmToolchain {
        this as JavaToolchainSpec
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
    }
}

addHostMachineAttributesToConfiguration("runtimeClasspath")
addHostMachineAttributesToConfiguration("testRuntimeClasspath")

tasks {
    test {
        useJUnitPlatform()
    }
    @Suppress("UNUSED_VARIABLE")
    val javadoc by getting(Javadoc::class) {
        options {
            this as StandardJavadocDocletOptions
            addBooleanOption("Xdoclint:none", true)
        }
    }
    withType<KotlinCompile>() {
        kotlinOptions.apiVersion = libs.versions.kotlinApi.get()
    }
}