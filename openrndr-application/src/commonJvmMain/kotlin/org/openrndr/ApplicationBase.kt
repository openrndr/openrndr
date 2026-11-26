package org.openrndr

import io.github.oshai.kotlinlogging.KotlinLogging


private val logger = KotlinLogging.logger {}

/**
 * This is accessible before finalizing the application in ApplicationBuilder.
 */
actual abstract class ApplicationBase {
    companion object {
        private const val AWT_BASE = "org.openrndr.awt.ApplicationBaseAWT"

        private fun available(className: String) = try {
            ApplicationBase::class.java.classLoader.loadClass(className)
            true
        } catch (e: ClassNotFoundException) {
            false
        }

        /**
         * Is the AWT application back-end selected, with the `org.openrndr.application` property or as the only
         * back-end available?
         */
        internal fun awtSelected(): Boolean = when (System.getProperty("org.openrndr.application")?.uppercase()) {
            "AWT" -> true
            null, "" -> available(AWT_BASE) &&
                    !available("org.openrndr.internal.gl3.ApplicationBaseGLFWGL3") &&
                    !available("org.openrndr.application.sdl.ApplicationBaseSDL")
            else -> false
        }

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
            val cl = ApplicationBase::class.java.classLoader

            val glfwBase = try {
                cl.loadClass("org.openrndr.internal.gl3.ApplicationBaseGLFWGL3")
            } catch (e: ClassNotFoundException) {
                null
            }
            val sdlBase = try {
                cl.loadClass("org.openrndr.application.sdl.ApplicationBaseSDL")
            } catch (e: ClassNotFoundException) {
                null
            }

            if (glfwBase != null && sdlBase != null) {
                logger.warn { "Both GLFW and SDL application backends are available" }
            }


            return when (val applicationProperty: String? = System.getProperty("org.openrndr.application")) {
                null, "" -> {
                    val c = try {
                        cl.loadClass("org.openrndr.internal.gl3.ApplicationBaseGLFWGL3")
                    } catch (e: ClassNotFoundException) {
                        null
                    } ?: try {
                        cl.loadClass("org.openrndr.internal.gles3.ApplicationBaseGLFWGLES3")
                    } catch (e: ClassNotFoundException) {
                        null
                    } ?: try {
                        cl.loadClass("org.openrndr.application.sdl.ApplicationBaseSDL")
                    } catch (e: ClassNotFoundException) {
                        null
                    } ?: try {
                        cl.loadClass(AWT_BASE)
                    } catch (e: ClassNotFoundException) {
                        null
                    }
                    c ?: error("No application backend not available")
                }

                "GLFW" -> {
                    val c = try {
                        cl.loadClass("org.openrndr.internal.gl3.ApplicationBaseGLFWGL3")
                    } catch (e: ClassNotFoundException) {
                        null
                    } ?: try {
                        cl.loadClass("org.openrndr.internal.gles3.ApplicationBaseGLFWGLES3")
                    } catch (e: ClassNotFoundException) {
                        null
                    }
                    c ?: error("GLFW application backend not available")
                }

                "SDL" -> {
                    val c = try {
                        cl.loadClass("org.openrndr.application.sdl.ApplicationBaseSDL")
                    } catch (e: ClassNotFoundException) {
                        null
                    }
                    c ?: error("SDL application backend not available")
                }

                "AWT" -> try {
                    cl.loadClass(AWT_BASE)
                } catch (e: ClassNotFoundException) {
                    error("AWT application backend not available")
                }

                "EGL" -> ApplicationBase::class.java.classLoader.loadClass("org.openrndr.internal.gl3.ApplicationBaseEGLGL3")
                else -> throw IllegalArgumentException("Unknown value '${applicationProperty}' provided for org.openrndr.application")
            }
        }
    }

    abstract val displays: List<Display>
    actual abstract fun build(program: Program, configuration: Configuration): Application
}