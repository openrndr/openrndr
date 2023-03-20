plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    maven(url="https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.dokka.gradle.plugin)
    // https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}