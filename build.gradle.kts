plugins {
    alias(libs.plugins.nebula.release)
    alias(libs.plugins.nmcp)
    id("org.openrndr.convention.dokka")
    id("com.adarshr.test-logger") version "4.0.0"
    `version-catalog`
    `maven-publish`
}

repositories {
    mavenCentral()
}

group = "org.openrndr"
nmcpAggregation {
    centralPortal {
        username.set(findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME"))
        password.set(findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD"))

        // publish manually from the portal
        publishingType = "USER_MANAGED"
    }
}

// Projects published to Maven Central. Each of these applies the `com.gradleup.nmcp` plugin,
// either through the publish convention plugins or directly in its build file.
dependencies {
    nmcpAggregation(project(":openrndr-animatable"))
    nmcpAggregation(project(":openrndr-application"))
    nmcpAggregation(project(":openrndr-binpack"))
    nmcpAggregation(project(":openrndr-color"))
    nmcpAggregation(project(":openrndr-dds"))
    nmcpAggregation(project(":openrndr-dependency-catalog"))
    nmcpAggregation(project(":openrndr-draw"))
    nmcpAggregation(project(":openrndr-event"))
    nmcpAggregation(project(":openrndr-extensions"))
    nmcpAggregation(project(":openrndr-filter"))
    nmcpAggregation(project(":openrndr-gl-common"))
    nmcpAggregation(project(":openrndr-js:openrndr-image-driver-browser"))
    nmcpAggregation(project(":openrndr-js:openrndr-webgl"))
    nmcpAggregation(project(":openrndr-jvm:openrndr-application-egl"))
    nmcpAggregation(project(":openrndr-jvm:openrndr-application-glfw"))
    nmcpAggregation(project(":openrndr-jvm:openrndr-application-sdl"))
    nmcpAggregation(project(":openrndr-jvm:openrndr-dialogs"))
    nmcpAggregation(project(":openrndr-jvm:openrndr-ffmpeg"))
    nmcpAggregation(project(":openrndr-jvm:openrndr-fontdriver-stb"))
    nmcpAggregation(project(":openrndr-jvm:openrndr-gl3"))
    nmcpAggregation(project(":openrndr-jvm:openrndr-gl3-support"))
    nmcpAggregation(project(":openrndr-jvm:openrndr-imagedriver-stb"))
    nmcpAggregation(project(":openrndr-jvm:openrndr-openal"))
    nmcpAggregation(project(":openrndr-kartifex"))
    nmcpAggregation(project(":openrndr-ktessellation"))
    nmcpAggregation(project(":openrndr-math"))
    nmcpAggregation(project(":openrndr-module-catalog"))
    nmcpAggregation(project(":openrndr-nullgl"))
    nmcpAggregation(project(":openrndr-platform"))
    nmcpAggregation(project(":openrndr-shape"))
    nmcpAggregation(project(":openrndr-utils"))
}

// Check that the list above matches the projects that are actually published. A project that
// applies `maven-publish` but is not listed would silently be left out of the Maven Central upload.
// Any mismatch fails the build.
gradle.projectsEvaluated {
    val registered = configurations["nmcpAggregation"].dependencies.withType<ProjectDependency>().map { it.path }.toSet()
    val problems = mutableListOf<String>()
    subprojects.forEach { p ->
        val publishes = p.plugins.hasPlugin("maven-publish")
        if (publishes && p.path !in registered) {
            problems += "${p.path} applies maven-publish but is not listed in nmcpAggregation"
        }
        if (p.path in registered && !p.plugins.hasPlugin("com.gradleup.nmcp")) {
            problems += "${p.path} is listed in nmcpAggregation but does not apply com.gradleup.nmcp"
        }
    }
    (registered - subprojects.map { it.path }.toSet()).forEach { problems += "$it is listed in nmcpAggregation but does not exist" }

    if (problems.isNotEmpty()) {
        throw GradleException("Maven Central publication list in build.gradle.kts is out of date:\n" + problems.joinToString("\n") { "  - $it" })
    }
}

subprojects {
    tasks.register<DependencyReportTask>("dependenciesAll") {
        group = HelpTasksPlugin.HELP_GROUP
        description = "Displays all dependencies, including subprojects."
    }
}

dependencies {
    dokka(project(":openrndr-animatable:"))
    dokka(project(":openrndr-application:"))
    dokka(project(":openrndr-binpack:"))
    dokka(project(":openrndr-color:"))
    dokka(project(":openrndr-dds:"))
    dokka(project(":openrndr-jvm:openrndr-dialogs:"))
    dokka(project(":openrndr-draw:"))
    dokka(project(":openrndr-event:"))
    dokka(project(":openrndr-extensions:"))
    dokka(project(":openrndr-jvm:openrndr-ffmpeg:"))
    dokka(project(":openrndr-filter:"))
    dokka(project(":openrndr-gl-common:"))
    dokka(project(":openrndr-jvm:openrndr-gl3:"))
    dokka(project(":openrndr-kartifex:"))
    dokka(project(":openrndr-ktessellation:"))
    dokka(project(":openrndr-math:"))
    dokka(project(":openrndr-nullgl:"))
    dokka(project(":openrndr-jvm:openrndr-openal:"))
    dokka(project(":openrndr-platform:"))
    dokka(project(":openrndr-shape:"))
    dokka(project(":openrndr-utils:"))
    dokka(project(":openrndr-js:openrndr-webgl:"))
}

abstract class VersionPrinterFlowAction : FlowAction<VersionPrinterFlowAction.Parameters> {
    interface Parameters : FlowParameters {
        @get:Input
        val version: Property<String>
    }

    override fun execute(parameters: Parameters) {
        println("\n")
        println("openrndr = \"${parameters.version.get()}\"")
    }
}

// FlowScope.always() is the configuration-cache-compatible replacement for
// the deprecated `gradle.buildFinished` hook: it fires once, at the very end
// of every build (cache hit or not), regardless of build outcome.
abstract class VersionPrinterPlugin : Plugin<Project> {
    @get:Inject
    abstract val flowScope: FlowScope

    override fun apply(project: Project) {
        flowScope.always(VersionPrinterFlowAction::class.java) {
            parameters.version.set(project.provider { project.version.toString() })
        }
    }
}

apply<VersionPrinterPlugin>()
