plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

val kotlinxSerializationVersion: String by rootProject.extra
val kotlinxCoroutinesVersion: String by rootProject.extra
val kotestVersion: String by rootProject.extra
val junitJupiterVersion: String by rootProject.extra
val kotlinApiVersion: String by rootProject.extra
val kotlinJvmTarget: String by rootProject.extra
val kotlinLoggingVersion: String by rootProject.extra
val kluentVersion: String by rootProject.extra

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

    sourceSets {
        @Suppress("UNUSED_VARIABLE")
        val commonMain by getting {
            dependencies {
                api(project(":openrndr-math"))
                api(project(":openrndr-draw"))
                api(project(":openrndr-animatable"))
                implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation("io.kotest:kotest-assertions-core:$kotestVersion")
            }
        }

        @Suppress("UNUSED_VARIABLE")
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
            }

        }

        @Suppress("UNUSED_VARIABLE")
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(kotlin("test-junit5"))
                implementation("org.amshove.kluent:kluent:$kluentVersion")
                runtimeOnly("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
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
    }
}