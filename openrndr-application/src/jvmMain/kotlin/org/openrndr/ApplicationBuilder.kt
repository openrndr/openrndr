package org.openrndr

import org.openrndr.exceptions.installUncaughtExceptionHandler
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.ArrayList
import java.lang.management.ManagementFactory

private fun restartJVM(): Boolean {
    // based on http://www.java-gaming.org/topics/starting-jvm-on-mac-with-xstartonfirstthread-programmatically/37697/view.html

    val osName = System.getProperty("os.name")

    // if not a mac return false
    if (!osName.startsWith("Mac") && !osName.startsWith("Darwin")) {
        return false
    }
    // get current jvm process pid
    val pid =
        ManagementFactory.getRuntimeMXBean().name.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
    // get environment variable on whether XstartOnFirstThread is enabled
    val env = System.getenv("JAVA_STARTED_ON_FIRST_THREAD_$pid")

    // if environment variable is "1" then XstartOnFirstThread is enabled
    if (env != null && env == "1") {
        return false
    }
    System.err.println("Warning: Running on macOS without -XstartOnFirstThread JVM argument. Restarting JVM with -XstartOnFirstThread.")

    // restart jvm with -XstartOnFirstThread
    val separator = System.getProperty("file.separator") ?: error("file.separator not set")
    val classpath = System.getProperty("java.class.path") ?: error("java.class.path not set")

    val mainClass = System.getenv("JAVA_MAIN_CLASS_$pid") ?: System.getProperty("sun.java.command")
    ?: error("JAVA_MAIN_CLASS_$pid and sun.java.command not set")
    val jvmPath = System.getProperty("java.home") + separator + "bin" + separator + "java"

    val inputArguments = ManagementFactory.getRuntimeMXBean().inputArguments
    val jvmArgs = ArrayList<String>()

    jvmArgs.add(jvmPath)
    jvmArgs.add("-XstartOnFirstThread")
    jvmArgs.addAll(inputArguments)
    jvmArgs.add("-cp")
    jvmArgs.add(classpath)
    jvmArgs.add(mainClass)

    // if you don't need console output, just enable these two lines
    // and delete bits after it. This JVM will then terminate.
    //ProcessBuilder processBuilder = new ProcessBuilder(jvmArgs);
    //processBuilder.start();

    try {
        val processBuilder = ProcessBuilder(jvmArgs)
        processBuilder.redirectErrorStream(true)
        val process = processBuilder.start()

        val `is` = process.inputStream
        val isr = InputStreamReader(`is`)
        val br = BufferedReader(isr)

        while (true) {
            val inline = br.readLine()
            if (inline == null)
                break
            else {
                println(inline)
            }
        }
        process.waitFor()
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return true
}

/**
 * Creates and runs a synchronous OPENRNDR application using the provided [ApplicationBuilder].
 * @see <a href="https://guide.openrndr.org/">the OPENRNDR guide</a>
 */
actual fun application(build: ApplicationBuilder.() -> Unit) {
    if (!restartJVM()) {
        installUncaughtExceptionHandler()
        val result: Application
        ApplicationBuilderJVM().apply {
            build()
            result = applicationBase.build(this.program, this.configuration)
            result.run()
        }


    } else {
        error("unreachable?")
    }
}

/**
 * Creates and runs an asynchronous OPENRNDR application using the provided [ApplicationBuilder].
 * @see <a href="https://guide.openrndr.org/">the OPENRNDR guide</a>
 */
actual suspend fun applicationAsync(build: ApplicationBuilder.() -> Unit) {
    throw NotImplementedError("Asynchronous application is unsupported, use application()")
}

@Suppress("DeprecatedCallableAddReplaceWith")
class ApplicationBuilderJVM : ApplicationBuilder() {
    override val configuration = Configuration()
    override var program: Program = Program()
    override val applicationBase: ApplicationBase = ApplicationBase.initialize()
    override val displays by lazy { applicationBase.displays }

    override fun configure(init: Configuration.() -> Unit) {
        configuration.init()
    }

    override fun program(init: suspend Program.() -> Unit): Program {
        program = object : Program() {
            override suspend fun setup() {
                init()
            }
        }
        return program
    }

    fun run() : Application {
        val result = applicationBase.build(this.program, this.configuration)
        result.run()
        return result
    }

    @Deprecated("Cannot construct application in an application block.", level = DeprecationLevel.ERROR)
    override fun application(build: ApplicationBuilder.() -> Unit): Nothing =
        error("Cannot construct application in an application block.")

    @Deprecated("Cannot construct application in an application block.", level = DeprecationLevel.ERROR)
    override fun applicationAsync(build: ApplicationBuilder.() -> Unit): Nothing =
        error("Cannot construct application in an application block.")

    @Deprecated("Cannot construct program in a program block.", level = DeprecationLevel.ERROR)
    override fun Program.program(init: Program.() -> Unit): Nothing =
        error("Cannot construct program in a program block.")
}
