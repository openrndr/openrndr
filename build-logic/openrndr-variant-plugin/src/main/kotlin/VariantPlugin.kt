package org.openrndr.variant.plugin

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.jvm.JvmComponentDependencies
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import javax.inject.Inject

fun arch(arch: String = System.getProperty("os.arch")): String {
    return when (arch) {
        "x86-64", "x86_64", "amd64" -> "x86-64"
        "arm64", "aarch64" -> "aarch64"
        else -> error("unsupported arch $arch")
    }
}

abstract class VariantContainer @Inject constructor(
    @Inject val tasks: TaskContainer,
    val apiElements: Configuration,

    val runtimeElements: Configuration,
    val sourceSet: SourceSet
) {

    @Nested
    abstract fun getDependencies(): JvmComponentDependencies

    fun Dependency.withClassifier(classifier: String): String {
        return "$group:$name:$version:$classifier"
    }

    /**
     * Setup dependencies for this variant.
     */
    fun dependencies(action: Action<in JvmComponentDependencies>) {
        action.execute(getDependencies())
    }

    /**
     * Specify that this variant comes with a resource bundle.
     */
    fun jar(action: Action<Unit>) {
        sourceSet.resources.srcDirs.add(sourceSet.java.srcDirs.first().parentFile.resolve("resources"))
        sourceSet.resources.includes.add("**/*.*")
        tasks.named<Jar>(sourceSet.jarTaskName).configure {
            include("**/*.*")
            dependsOn(tasks.named<ProcessResources>(sourceSet.processResourcesTaskName))
            manifest {
                //this.attributes()
            }
            this.from(sourceSet.resources.srcDirs)
        }
        runtimeElements.outgoing.artifact(tasks.named(sourceSet.jarTaskName))
        action.execute(Unit)
    }
}

abstract class VariantExtension(
    @Inject val objectFactory: ObjectFactory,
    @Inject val project: Project
) {

    fun platform(os: String, arch: String, f: VariantContainer.() -> Unit) {
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)

        val sourceSetArch = arch.replace("-", "_")
        val nameMain = "${os}${sourceSetArch.capitalize()}Main"
        val platformMain = sourceSets.create(nameMain)
        val tasks = project.tasks
        tasks.register(platformMain.jarTaskName, Jar::class.java) {
            archiveClassifier.set("$os-$arch")
        }

        val configurations = project.configurations
        val objects = project.objects

        val main = sourceSets.getByName("main")
        val mainApi = configurations.getByName(main.apiElementsConfigurationName)
        val mainRuntimeOnly = configurations.getByName(main.runtimeElementsConfigurationName)

        mainApi.attributes {
            val osAttribute = Attribute.of("org.gradle.native.operatingSystem", String::class.java)
            attribute(osAttribute, "do_not_use_me")
        }

        val platformMainRuntimeElements = configurations.create(platformMain.runtimeElementsConfigurationName) {
            extendsFrom(mainRuntimeOnly, mainApi)
            isCanBeResolved = false
            isCanBeConsumed = true
            val osAttribute = Attribute.of("org.gradle.native.operatingSystem", String::class.java)
            val archAttribute = Attribute.of("org.gradle.native.architecture", String::class.java)
            val typeAttribute = Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java)
            val environmentAttribute = Attribute.of("org.gradle.jvm.environment", String::class.java)

            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
                attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
                attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
                attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))

                attribute(osAttribute, os)
                attribute(archAttribute, arch)
                attribute(typeAttribute, "jvm")
                attribute(environmentAttribute, "standard-jvm")
            }
            outgoing.artifact(tasks.named(main.jarTaskName))
            outgoing.artifact(tasks.named(platformMain.jarTaskName))
        }

        val javaComponent = project.components.getByName("java") as AdhocComponentWithVariants
        javaComponent.addVariantsFromConfiguration(platformMainRuntimeElements) {
            platformMain.runtimeClasspath.files.add(platformMain.resources.srcDirs.first())
        }

        val variantContainer = objectFactory.newInstance(
            VariantContainer::class.java,
            platformMainRuntimeElements,
            platformMainRuntimeElements,
            platformMain
        )
        variantContainer.f()

        platformMainRuntimeElements.dependencies.addAll(variantContainer.getDependencies().runtimeOnly.dependencies.get())

        /*
        Setup dependencies for current platform. This will make in-module tests and demos work.
         */
        val currentOperatingSystemName: String = DefaultNativePlatform.getCurrentOperatingSystem().toFamilyName()
        val currentArchitectureName: String = arch()

        //println("${System.getProperty("os.name")} ${System.getProperty("os.arch")}")
        println("current operating system name: $currentOperatingSystemName $currentArchitectureName")
        if (currentOperatingSystemName == os && currentArchitectureName == arch) {
            project.dependencies {
                add("testRuntimeOnly", platformMain.output)
                add("demoRuntimeOnly", platformMain.output)
                for (i in platformMainRuntimeElements.dependencies) {
                    add("testRuntimeOnly", i)
                    add("demoRuntimeOnly", i)
                }
            }
        }
    }
}

class VariantPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val project = target
        project.extensions.create("variants", VariantExtension::class.java)
    }
}