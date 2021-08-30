package org.openrndr

@ApplicationDslMarker
class ApplicationBuilder internal constructor(){
    internal val configuration = Configuration()
    var program: Program = Program()

    fun configure(init: Configuration.() -> Unit) {
        configuration.init()
    }

    fun program(init: suspend Program.() -> Unit) {
        program = object : Program() {
            override suspend fun setup() {
                init()
            }
        }
    }
}

/**
 * Runs [programFunction] as an application using [configuration], this provides a more functional flavored way of
 * writing applications.
 */
expect fun application(build: ApplicationBuilder.() -> Unit)


/**
 * Runs [programFunction] as an application using [configuration], this provides a more functional flavored way of
 * writing applications.
 */
expect suspend fun applicationAsync(build: ApplicationBuilder.() -> Unit)

