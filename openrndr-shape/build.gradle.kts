plugins {
    kotlin("multiplatform")
}

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

    // native part switched off for now as it's quite unstable at the beginning on 2021
    /*
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    @Suppress("UNUSED_VARIABLE")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }
     */

    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                api(project(":openrndr-math"))
                api(project(":openrndr-color"))
                api(project(":openrndr-utils"))
                implementation(project(":openrndr-kartifex"))
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmMain by getting {
            dependencies {
                implementation(project(":openrndr-jvm:openrndr-tessellation"))
            }
        }

        // native part switched off for now as it's quite unstable at the beginning on 2021
        /*
            @Suppress("UNUSED_VARIABLE")
            val nativeMain by getting
            @Suppress("UNUSED_VARIABLE")
            val nativeTest by getting
         */
    }
}