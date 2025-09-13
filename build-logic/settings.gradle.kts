//rootProject.name = "build-logic"

include("openrndr-convention", "openrndr-variant-plugin")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}