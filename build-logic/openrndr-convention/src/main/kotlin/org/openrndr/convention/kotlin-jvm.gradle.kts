package org.openrndr.convention

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.gradle.api.JavaVersion


fun arch(arch: String = System.getProperty("os.arch")): String {
    return when (arch) {
        "x86-64", "x86_64", "amd64" -> "x86-64"
        "arm64", "aarch64" -> "aarch64"
        else -> error("unsupported arch $arch")
    }
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    // TODO remove this when LWJGL 3.4.0 is released
    maven("https://central.sonatype.com/repository/maven-snapshots")
}

group = "org.openrndr"

dependencies {
    implementation(libs.findLibrary("kotlin-stdlib").get())
    implementation(libs.findLibrary("kotlin-logging").get())
    testImplementation(libs.findLibrary("kotlin.test").get())
    testRuntimeOnly(libs.findLibrary("slf4j-simple").get())
}

tasks {
    val test by getting(Test::class) {
        useJUnitPlatform()
        testLogging.exceptionFormat = TestExceptionFormat.FULL
        allJvmArgs = allJvmArgs + "-Dorg.openrndr.gl3.skip_glfw_termination"
    }

    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.valueOf("JVM_${libs.findVersion("jvmTarget").get().displayName}"))
            freeCompilerArgs.add("-Xexpect-actual-classes")
            freeCompilerArgs.add("-Xjdk-release=${libs.findVersion("jvmTarget").get().displayName}")

            apiVersion.set(KotlinVersion.valueOf("KOTLIN_${libs.findVersion("kotlinApi").get().displayName.replace(".", "_")}"))
            languageVersion.set(KotlinVersion.valueOf("KOTLIN_${libs.findVersion("kotlinLanguage").get().displayName.replace(".", "_")}"))
        }
    }
}

java {
    targetCompatibility = JavaVersion.valueOf("VERSION_${libs.findVersion("jvmTarget").get().displayName.replace(".", "_")}")
}
//val demo: SourceSet by sourceSets.creating {
//}
val demo = project.sourceSets.create("demo")
val currentOperatingSystemName: String = DefaultNativePlatform.getCurrentOperatingSystem().toFamilyName()
val currentArchitectureName: String = arch()


configurations.matching {
    it.name.endsWith("runtimeClasspath", ignoreCase = true)
}.configureEach {
    attributes {
        attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named(currentOperatingSystemName))
        attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named(currentArchitectureName))
    }
}

