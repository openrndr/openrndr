package org.openrndr.convention

import com.sun.tools.javac.resources.javac
import gradle.kotlin.dsl.accessors._7010e7c128b6cdcc181cc0ba88f68500.publishing
import gradle.kotlin.dsl.accessors._d2503ffd2871bce8a27840559be7f55b.implementation
import gradle.kotlin.dsl.accessors._d2503ffd2871bce8a27840559be7f55b.java
import gradle.kotlin.dsl.accessors._d2503ffd2871bce8a27840559be7f55b.testImplementation
import gradle.kotlin.dsl.accessors._d2503ffd2871bce8a27840559be7f55b.testRuntimeOnly
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val libs = the<LibrariesForLibs>()

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

group = "org.openrndr"
objects
dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.logging)
    testImplementation(libs.kotlin.test)
    testRuntimeOnly(libs.slf4j.simple)
}
sourceSets
tasks {
    val test by getting(Test::class) {
        useJUnitPlatform()
        testLogging.exceptionFormat = TestExceptionFormat.FULL
        allJvmArgs = allJvmArgs + "-Dorg.openrndr.gl3.skip_glfw_termination"
    }

    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.valueOf("JVM_${libs.versions.jvmTarget.get()}"))
            freeCompilerArgs.add("-Xexpect-actual-classes")
            freeCompilerArgs.add("-Xjdk-release=${libs.versions.jvmTarget.get()}")
            apiVersion.set(KotlinVersion.valueOf("KOTLIN_${libs.versions.kotlinApi.get().replace(".", "_")}"))
            languageVersion.set(KotlinVersion.valueOf("KOTLIN_${libs.versions.kotlinLanguage.get().replace(".", "_")}"))
        }
    }
}

components.getByName("java") {}

java {
    targetCompatibility = JavaVersion.VERSION_17
}
