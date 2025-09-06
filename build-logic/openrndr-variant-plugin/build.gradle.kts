plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}


gradlePlugin {
    plugins {
        create("openrndrVariants") {
            id = "openrndr-variant"
            implementationClass = "org.openrndr.variant.plugin.VariantPlugin"
        }
    }
}