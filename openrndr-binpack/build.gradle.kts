plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
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
                api(project(":openrndr-shape"))
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.kotest)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmMain by getting

        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(kotlin("test-junit5"))
                runtimeOnly(libs.bundles.jupiter)
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jsMain by getting

        @Suppress("UNUSED_VARIABLE")
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
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