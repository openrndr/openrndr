plugins {
    org.openrndr.convention.`kotlin-multiplatform`
    org.openrndr.convention.`publish-multiplatform`
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":openrndr-math"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotest.assertions)
                implementation(project(":openrndr-shape"))
            }
        }
    }
}