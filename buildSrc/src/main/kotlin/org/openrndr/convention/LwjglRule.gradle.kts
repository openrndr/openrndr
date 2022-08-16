package org.openrndr.convention

@CacheableRule
abstract class LwjglRule : ComponentMetadataRule {

    @get:Inject
    abstract val objects: ObjectFactory

    override fun execute(context: ComponentMetadataContext) = context.details.run {
        if (id.group != "org.lwjgl") return
        if (id.name == "lwjgl-egl") return
        withVariant("runtime") {
            attributes {
                attributes.attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named("none"))
                attributes.attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named("none"))
            }
        }
        for (nativeVariant in openrndrJvmNativeVariants) {
            val lwjglTargetName = nativeVariant.mapToLwjglTargetName()
            addVariant("$lwjglTargetName-runtime", "runtime") {
                attributes {
                    attributes.attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named(nativeVariant.os))
                    attributes.attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named(nativeVariant.arch))
                }
                withFiles {
                    addFile("${id.name}-${id.version}-$lwjglTargetName.jar")
                }
            }

        }
    }
}