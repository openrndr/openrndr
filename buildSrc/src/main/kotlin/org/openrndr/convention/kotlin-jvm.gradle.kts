package org.openrndr.convention

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val libs = the<LibrariesForLibs>()

plugins {
    java
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

group = "org.openrndr"

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.logging)
    testImplementation(libs.kotlin.test)
    testRuntimeOnly(libs.slf4j.simple)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

tasks {
    @Suppress("UNUSED_VARIABLE")
    val test by getting(Test::class) {
        useJUnitPlatform()
        testLogging.exceptionFormat = TestExceptionFormat.FULL
        allJvmArgs = allJvmArgs + "-Dorg.openrndr.gl3.skip_glfw_termination"
    }

    withType<KotlinCompile>() {
        kotlinOptions.apiVersion = libs.versions.kotlinApi.get()
        kotlinOptions.languageVersion = libs.versions.kotlinLanguage.get()
        kotlinOptions.freeCompilerArgs += "-Xexpect-actual-classes"
    }
}