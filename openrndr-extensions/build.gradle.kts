plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

val kotlinxSerializationVersion: String by rootProject.extra
val kotestVersion: String by rootProject.extra
val junitJupiterVersion: String by rootProject.extra
val kotlinLogginVersion: String by rootProject.extra
val kotlinApiVersion: String by rootProject.extra
val kotlinJvmTarget: String by rootProject.extra
val kotlinLoggingVersion: String by rootProject.extra

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
                implementation(project(":openrndr-application"))
                implementation(project(":openrndr-draw"))
//                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
                implementation("io.kotest:kotest-assertions-core:$kotestVersion")
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmMain by getting {
            dependencies {
                implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(kotlin("test-junit5"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
                runtimeOnly("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

            }
        }

//        @Suppress("UNUSED_VARIABLE")
//        val jsMain by getting
//        @Suppress("UNUSED_VARIABLE")
//        val jsTest by getting {
//            dependencies {
//                implementation(kotlin("test-js"))
//            }
//        }

        // native part switched off for now as it's quite unstable at the beginning on 2021
        /*
            @Suppress("UNUSED_VARIABLE")
            val nativeMain by getting
            @Suppress("UNUSED_VARIABLE")
            val nativeTest by getting
         */
    }

}
