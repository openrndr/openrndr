plugins {
    //kotlin("jvm")
    java
}
val kotlinLanguageVersion: String by rootProject.extra
val kotlinApiVersion: String by rootProject.extra

dependencies {
    implementation(project(":openrndr-math"))
}



//kotlin {
//    jvm {
//        compilations.all {
//            kotlinOptions.jvmTarget = "1.8"
//            kotlinOptions.apiVersion = kotlinApiVersion
//            kotlinOptions.languageVersion = kotlinLanguageVersion
//        }
//        testRuns["test"].executionTask.configure {
//            useJUnitPlatform()
//            exclude("**/*.class")
//        }
//    }
//    sourceSets {
//        val jvmMain by getting {
//            dependencies {
//                implementation(project(":openrndr-math"))
//            }
//        }
//    }
//}
