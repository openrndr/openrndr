plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.kotlin-multiplatform-js")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {

    jvm {
        compilations {
            create("demo")
        }
    }

    applyDefaultHierarchyTemplate { // or .custom depending on your setup
        common {
            group("commonJvm") {
                withJvm()
                group("jvm") { withJvm() }
                group("android") { withAndroidTarget() }
            }
        }
    }

    sourceSets {
        getByName("commonTest") {
            dependencies {
                implementation(libs.kotest.assertions)
            }
        }

        getByName("commonMain") {
            dependencies {
                api(project(":openrndr-math"))
                api(project(":openrndr-color"))
                api(project(":openrndr-shape"))
                api(project(":openrndr-event"))
                implementation(project(":openrndr-utils"))
                implementation(project(":openrndr-platform"))
                implementation(libs.kotlin.coroutines)
                implementation(libs.kotlin.logging)
            }
        }
        val commonJvmMain by getting


        if (platformConfiguration.android) {
            val androidMain by getting {
                dependsOn(commonJvmMain)
            }
        }
        getByName("webMain") {
            dependencies {
                implementation(libs.kotlin.js)
                implementation(libs.kotlin.browser)
            }
        }
        val jvmDemo by getting {
            dependencies {
                implementation(project(":openrndr-application"))
                implementation(project(":openrndr-math"))
                implementation(project(":openrndr-utils"))
                implementation(project(":openrndr-jvm:openrndr-fontdriver-freetype"))
                implementation(project(":openrndr-jvm:openrndr-textshapingdriver-harfbuzz"))
                runtimeOnly(project(":openrndr-jvm:openrndr-application-glfw"))
                runtimeOnly(project(":openrndr-jvm:openrndr-gl3"))
                dependsOn(commonJvmMain)
                runtimeOnly(libs.slf4j.simple)
            }
        }

    }
}

kotlin {
    jvm().mainRun {
        classpath(kotlin.jvm().compilations.getByName("demo").output.allOutputs)
        classpath(kotlin.jvm().compilations.getByName("demo").configurations.runtimeDependencyConfiguration!!)
    }
}

tasks.withType<JavaExec>().matching { it.name == "jvmRun" }.configureEach { workingDir = rootDir }
