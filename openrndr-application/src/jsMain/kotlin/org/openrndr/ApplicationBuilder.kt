package org.openrndr

/**
 * Creates and runs a synchronous OPENRNDR application using the provided [ApplicationBuilder].
 * @see <a href="https://guide.openrndr.org/">the OPENRNDR guide</a>
 */
actual fun application(build: ApplicationBuilder.() -> Unit){
    throw NotImplementedError("Synchronous application is unsupported, use applicationAsync()")
}

/**
 * Creates and runs an asynchronous OPENRNDR application using the provided [ApplicationBuilder].
 * @see <a href="https://guide.openrndr.org/">the OPENRNDR guide</a>
 */
actual suspend fun applicationAsync(build: ApplicationBuilder.() -> Unit) {
    ApplicationBuilderJS().apply {
        build()
        applicationBase.build(program, configuration).runAsync()
    }
}

@Suppress("DeprecatedCallableAddReplaceWith")
class ApplicationBuilderJS internal constructor() : ApplicationBuilder() {
    override val configuration = Configuration()
    override var program: Program = ProgramImplementation()
    override val applicationBase = applicationBaseFunc?.invoke() ?: error("applicationFunc not set")
    override val displays: List<Display> = emptyList()

    override fun configure(init: Configuration.() -> Unit) {
        configuration.init()
    }

    override fun program(init: suspend Program.() -> Unit) : Program {
        program = object : ProgramImplementation() {
            override suspend fun setup() {
                init()
            }
        }
        return program
    }

    @Deprecated("Cannot construct application in an application block.", level = DeprecationLevel.ERROR)
    override fun application(build: ApplicationBuilder.() -> Unit): Nothing = error("Cannot construct application in an application block.")

    @Deprecated("Cannot construct application in an application block.", level = DeprecationLevel.ERROR)
    override fun applicationAsync(build: ApplicationBuilder.() -> Unit): Nothing = error("Cannot construct application in an application block.")

    @Deprecated("Cannot construct program in a program block.", level = DeprecationLevel.ERROR)
    override fun Program.program(init: Program.() -> Unit): Nothing = error("Cannot construct program in a program block.")
}
