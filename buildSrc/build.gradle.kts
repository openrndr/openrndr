plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    // TODO: Replace this with `libs.kotlin.gradle.plugin`
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10")
    // https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}