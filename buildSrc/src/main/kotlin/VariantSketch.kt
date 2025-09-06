import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.jvm.tasks.Jar
import org.gradle.api.attributes.Attribute
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.support.delegates.ProjectDelegate

fun ProjectDelegate.createBlarp() {
//    val platformMain = project.sourceSets.create("$os${arch.capitalize()}Main")
//    tasks.register<Jar>(platformMain.jarTaskName, Jar::class.java) {
//        archiveClassifier.set("$os-$arch")
//    }
//    val main = sourceSets.getByName("main")

}

fun blarp() = "blarp"
fun createPlatformVariant(sourceSets: SourceSetContainer,
                          tasks: TaskContainer,
                          configurations: ConfigurationContainer,
                          objects: ObjectFactory,
//                          components: ComponentContainer,
                          project: org.gradle.api.Project,
                          os: String, arch: String) {

    //val sourceSets = SourceSetContainer.all
    val platformMain = sourceSets.create("$os${arch.capitalize()}Main")
    tasks.register<Jar>(platformMain.jarTaskName, Jar::class.java) {
        archiveClassifier.set("$os-$arch")
    }
    val main = sourceSets.getByName("main")


    main.apply {
//        dependencies {
//            implementation("bla")
//        }
    }

    val platformMainApiElements = configurations.create(platformMain.apiElementsConfigurationName) {
        isCanBeResolved = false
        isCanBeConsumed = true

        val osAttribute = Attribute.of("org.gradle.native.operatingSystem", String::class.java)
        val archAttribute = Attribute.of("org.gradle.native.architecture", String::class.java)
        val typeAttribute = Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java)
        val environmentAttribute = Attribute.of("org.gradle.jvm.environment", String::class.java)


        attributes {
//            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_API))
//            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
//            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
//            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
//            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
            attribute(osAttribute, os)
            attribute(archAttribute, arch)
            attribute(typeAttribute, "jvm")
            attribute(environmentAttribute, "standard-jvm")
        }
        outgoing.artifact(tasks.named(main.jarTaskName))
        outgoing.artifact(tasks.named(platformMain.jarTaskName))
    }

    val platformMainRuntimeElements = configurations.create(platformMain.runtimeElementsConfigurationName) {
        isCanBeResolved = false
        isCanBeConsumed = true

        val osAttribute = Attribute.of("org.gradle.native.operatingSystem", String::class.java)
        val archAttribute = Attribute.of("org.gradle.native.architecture", String::class.java)
        val typeAttribute = Attribute.of("org.jetbrains.kotlin.platform.type", String::class.java)
        val environmentAttribute = Attribute.of("org.gradle.jvm.environment", String::class.java)

        extendsFrom(configurations.getByName(platformMain.implementationConfigurationName))

        attributes {
//            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
//            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
//            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
//            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
//            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))

            attribute(osAttribute, os)
            attribute(archAttribute, arch)
            attribute(typeAttribute, "jvm")
            attribute(environmentAttribute, "standard-jvm")
        }
        outgoing.artifact(tasks.named(platformMain.jarTaskName))
    }

    /*
    val javaComponent = components["java"] as AdhocComponentWithVariants
    javaComponent.apply {
    }
    javaComponent.addVariantsFromConfiguration(platformMainApiElements) {}
    javaComponent.addVariantsFromConfiguration(platformMainRuntimeElements) {}
*/
}
