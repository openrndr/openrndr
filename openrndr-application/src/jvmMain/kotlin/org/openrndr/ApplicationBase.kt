package org.openrndr

import io.github.oshai.kotlinlogging.KotlinLogging


private val logger = KotlinLogging.logger {}

/**
 * This is accessible before finalizing the application in ApplicationBuilder.
 */
actual abstract class ApplicationBase {
    companion object {
        fun initialize(): ApplicationBase {
            val applicationBaseClass = loadApplicationBase()
            return applicationBaseClass.declaredConstructors[0].newInstance() as ApplicationBase
        }

        private fun loadApplicationBase(): Class<*> {
            try {
                val c =
                    ApplicationBase::class.java.classLoader.loadClass("org.openrndr.internal.nullgl.ApplicationBaseNullGL")
                logger.debug { "NullGL found" }
                return c
            } catch (e: ClassNotFoundException) {
                logger.debug { "NullGL not found" }
            }

            return when (val applicationProperty: String? = System.getProperty("org.openrndr.application")) {
                null, "", "GLFW" -> {
                    val cl = ApplicationBase::class.java.classLoader

                    val c = try { cl.loadClass("org.openrndr.internal.gl3.ApplicationBaseGLFWGL3") } catch (e:ClassNotFoundException) { null } ?:
                    try { cl.loadClass("org.openrndr.internal.gles3.ApplicationBaseGLFWGLES3") } catch (e:ClassNotFoundException) { null }

                    c!!
                }
                "EGL" -> ApplicationBase::class.java.classLoader.loadClass("org.openrndr.internal.gl3.ApplicationBaseEGLGL3")
                else -> throw IllegalArgumentException("Unknown value '${applicationProperty}' provided for org.openrndr.application")
            }
        }
    }

    abstract val displays: List<Display>
    actual abstract fun build(program: Program, configuration: Configuration): Application
}