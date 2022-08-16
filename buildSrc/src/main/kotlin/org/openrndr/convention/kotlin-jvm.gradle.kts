package org.openrndr.convention

import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val libs = the<LibrariesForLibs>()

plugins {
    java
    kotlin("jvm")
    id("maven-publish")
}

repositories {
    mavenCentral()
}

group = "org.openrndr"

dependencies {
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

tasks {
    @Suppress("UNUSED_VARIABLE")
    val test by getting(Test::class) {
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