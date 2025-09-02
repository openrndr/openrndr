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

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.logging)
    testImplementation(libs.kotlin.test)
    testRuntimeOnly(libs.slf4j.simple)
}

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

val macosArm64Main = sourceSets.create("macosArm64Main")
tasks.register<Jar>(macosArm64Main.jarTaskName) {
    archiveClassifier.set("macos-arm64")
}
val main = sourceSets.getByName("main")
val macosArm64MainApiElements = configurations.create(macosArm64Main.apiElementsConfigurationName) {
    isCanBeResolved = false
    isCanBeConsumed = true

    val osAttribute = Attribute.of("org.gradle.native.operatingSystem", String::class.java)
    val archAttribute = Attribute.of("org.gradle.native.architecture", String::class.java)
    val typeAttribute = Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java)
    val environmentAttribute = Attribute.of("org.gradle.jvm.environment", String::class.java)


    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_API))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        attribute(osAttribute, "macos")
        attribute(archAttribute, "aarch64")
        attribute(typeAttribute, "jvm")
        attribute(environmentAttribute, "standard-jvm")
    }
    outgoing.artifact(tasks.named(main.jarTaskName))
    outgoing.artifact(tasks.named(macosArm64Main.jarTaskName))
}

val macosArm64MainRuntimeElements = configurations.create(macosArm64Main.runtimeElementsConfigurationName) {
    isCanBeResolved = false
    isCanBeConsumed = true

    val osAttribute = Attribute.of("org.gradle.native.operatingSystem", String::class.java)
    val archAttribute = Attribute.of("org.gradle.native.architecture", String::class.java)
    val typeAttribute = Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java)
    val environmentAttribute = Attribute.of("org.gradle.jvm.environment", String::class.java)

    extendsFrom(configurations.getByName(macosArm64Main.implementationConfigurationName))

    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))

        attribute(osAttribute, "macos")
        attribute(archAttribute, "aarch64")
        attribute(typeAttribute, "jvm")
        attribute(environmentAttribute, "standard-jvm")
    }
    outgoing.artifact(tasks.named(macosArm64Main.jarTaskName))
}

java {
    targetCompatibility = JavaVersion.VERSION_17
}
val javaComponent = components["java"] as AdhocComponentWithVariants
    javaComponent.apply {
}
javaComponent.addVariantsFromConfiguration(macosArm64MainApiElements) {}
javaComponent.addVariantsFromConfiguration(macosArm64MainRuntimeElements) {}