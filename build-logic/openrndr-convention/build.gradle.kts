plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()

}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    implementation(project(":openrndr-variant-plugin"))
    implementation(libs.findLibrary("kotlin-gradle-plugin").get())
    implementation(libs.findLibrary("dokka-gradle-plugin").get())
    implementation(libs.findLibrary("android-library-gradle-plugin").get())
}