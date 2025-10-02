plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.kotlin-multiplatform-js")
    id("org.openrndr.convention.publish-multiplatform")
}

kotlin {
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
        val commonMain by getting
        val commonJvmMain by getting
//
//        val commonJvmMain by creating {
//            dependsOn(commonMain)
//            kotlin.srcDirs("src/commonJvmMain")
//
//        }
//
        val androidMain by getting {
            dependsOn(commonJvmMain)
        }
//
//        val jvmMain by getting {
//            dependsOn(commonJvmMain)
//        }
    }
}