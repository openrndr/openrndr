plugins {
    id("org.openrndr.convention.kotlin-multiplatform")
    id("org.openrndr.convention.kotlin-multiplatform-js")
    id("org.openrndr.convention.publish-multiplatform")
}

val embedShaders = tasks.register<EmbedShadersTask>("embedShaders") {
    inputDir.set(file("$projectDir/src/shaders/glsl"))
    outputDir.set(file("${layout.buildDirectory.get()}/generated/shaderKotlin"))

    defaultPackage.set("org.openrndr.filter")
    defaultVisibility.set("")
    namePrefix.set("filter_")
}.get()

kotlin {
    kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(embedShaders.outputDir)
    sourceSets {

        val commonMain by getting {
            dependencies {
                implementation(project(":openrndr-draw"))
            }
        }
    }
}