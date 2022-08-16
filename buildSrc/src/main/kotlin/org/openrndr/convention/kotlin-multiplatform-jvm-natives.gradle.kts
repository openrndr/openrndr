package org.openrndr.convention

import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinDependencyHandler
import org.openrndr.convention.LwjglRule_gradle.*

plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
}

kotlin {
    for ((targetName, os, arch) in openrndrJvmNativeVariants) {
        jvm(targetName) {
            attributes.attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named(os))
            attributes.attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named(arch))
        }
        configurations[targetName + "ApiElements"].attributes {
            attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named(os))
            attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named(arch))
        }
        configurations[targetName + "RuntimeElements"].attributes {
            attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named(os))
            attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named(arch))
        }
    }
}