package org.openrndr

/**
 * Runs [programFunction] as an application using [configuration], this provides a more functional flavored way of
 * writing applications.
 */
actual fun application(build: ApplicationBuilder.() -> Unit) {
    throw NotImplementedError("Synchronous application is unsupported, use applicationAsync()")
}

/**
 * Runs [programFunction] as an application using [configuration], this provides a more functional flavored way of
 * writing applications.
 */
actual suspend fun applicationAsync(build: ApplicationBuilder.() -> Unit) {
    ApplicationBuilder().apply {
        build()
        application.runAsync(program, configuration)
    }
}

@ApplicationDslMarker
actual class ApplicationBuilder internal actual constructor(){
    internal actual val configuration = Configuration()
    actual var program: Program = Program()
    actual val application = applicationFunc?.invoke(program, configuration) ?: error("applicationFunc not set")

    actual fun configure(init: Configuration.() -> Unit) {
        configuration.init()
    }

    actual fun program(init: suspend Program.() -> Unit) {
        program = object : Program() {
            override suspend fun setup() {
                init()
            }
        }
    }
}
