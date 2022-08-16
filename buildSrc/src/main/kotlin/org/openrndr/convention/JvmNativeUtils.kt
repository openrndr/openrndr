package org.openrndr.convention

import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyArtifact
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import org.gradle.nativeplatform.MachineArchitecture
import org.gradle.nativeplatform.OperatingSystemFamily
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinDependencyHandler

data class JvmNativeVariant(val targetName: String, val os: String, val arch: String)

val currentOperatingSystemName: String = DefaultNativePlatform.getCurrentOperatingSystem().toFamilyName()
val currentArchitectureName: String = DefaultNativePlatform.getCurrentArchitecture().name

/**
 * [See for details](https://docs.gradle.org/current/userguide/component_metadata_rules.html#adding_variants_for_native_jars)
 */
fun Project.addHostMachineAttributesToConfiguration(configurationName: String) {
    configurations[configurationName].attributes {
        attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named(currentOperatingSystemName))
        attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named(currentArchitectureName))
    }
}

val openrndrJvmNativeVariants: List<JvmNativeVariant>
    get() = listOf(
        JvmNativeVariant("natives-linux-arm64", OperatingSystemFamily.LINUX, "arm64"),
        JvmNativeVariant("natives-linux-x64", OperatingSystemFamily.LINUX, "x86-64"),
        JvmNativeVariant("natives-macos-arm64", OperatingSystemFamily.MACOS, "arm64"),
        JvmNativeVariant("natives-macos", OperatingSystemFamily.MACOS, "x86-64"),
        JvmNativeVariant("natives-windows", OperatingSystemFamily.WINDOWS, "x86-64")
    )

/**
 * This is for when you want to preserve a dependency resolved with
 * Gradle Module Metadata but still want resolved classifier in the published POM.
 */
fun <T> KotlinDependencyHandler.addNativeRuntimeOnly(dependency: Provider<T>, targetClassifier: String) {
    val configurationName = (this as DefaultKotlinDependencyHandler).parent.runtimeOnlyConfigurationName
    project.dependencies.addProvider<T, ExternalModuleDependency>(configurationName, dependency) {
        artifact {
            classifier = targetClassifier
            extension = DependencyArtifact.DEFAULT_TYPE
        }
    }
}