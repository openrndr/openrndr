package org.openrndr.convention

@CacheableRule
abstract class LwjglRule : ComponentMetadataRule {

    @get:Inject
    abstract val objects: ObjectFactory

    override fun execute(context: ComponentMetadataContext) {
        context.details.withVariant("runtime") {
            attributes {
                attributes.attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named("none"))
                attributes.attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named("none"))
            }
        }
        for (nativeVariant in openrndrJvmNativeVariants) {
            val lwjglTargetName = nativeVariant.mapToLwjglTargetName()
            context.details.addVariant("$lwjglTargetName-runtime", "runtime") {
                attributes {
                    attributes.attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named(nativeVariant.os))
                    attributes.attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named(nativeVariant.arch))
                }
                withFiles {
                    addFile("${context.details.id.name}-${context.details.id.version}-$lwjglTargetName.jar")
                }
            }

        }
    }
}