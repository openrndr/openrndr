package org.openrndr

class AndroidApplicationBuilder {
    val configuration = Configuration()
    internal var programFactory: (() -> ProgramImplementation)? = null

    // Keep the JVM syntax: program { … }
    fun program(block: ProgramImplementation.() -> Unit) {
        programFactory = {
            object : ProgramImplementation() {
                override suspend fun setup() {
                    // Users can mark background, add resources, etc. in the block.
                    // If they call extend { … } here, ProgramImplementation handles it.
                }
            }.apply(block)
        }
    }

    fun build(): Pair<Configuration, ProgramImplementation> {
        val program = requireNotNull(programFactory?.invoke()) {
            "You must call program { … } inside application { … }"
        }
        return configuration to program
    }
}

expect fun androidApplication(block: AndroidApplicationBuilder.() -> Unit)