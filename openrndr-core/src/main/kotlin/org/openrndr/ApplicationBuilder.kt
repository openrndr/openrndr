package org.openrndr

@ApplicationDslMarker
class ApplicationBuilder internal constructor(){
    internal val configuration = Configuration()
     var program: Program = Program()

    fun configure(init: Configuration.() -> Unit) {
        configuration.init()
    }

    fun program(init: Program.() -> Unit) {
        program = object : Program() {
            override fun setup() {
                init()
            }
        }
    }

    @JvmName("initCustom")
    inline fun <reified P:Program> program(program:P, crossinline init: P.() -> Unit) {
        this.program = program
        program.launch {
            program.init()
        }
    }
}

/**
 * Runs [programFunction] as an application using [configuration], this provides a more functional flavored way of
 * writing applications.
 */
fun application(build: ApplicationBuilder.() -> Unit) {
    val applicationBuilder = ApplicationBuilder().apply { build() }
    application(applicationBuilder.program, applicationBuilder.configuration)
}