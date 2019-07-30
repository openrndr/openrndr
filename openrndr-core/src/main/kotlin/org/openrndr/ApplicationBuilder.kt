package org.openrndr

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
    val pid = ManagementFactory.getRuntimeMXBean().name.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
    // get environment variable on whether XstartOnFirstThread is enabled
    val env = System.getenv("JAVA_STARTED_ON_FIRST_THREAD_$pid")

    // if environment variable is "1" then XstartOnFirstThread is enabled
    if (env != null && env == "1") {
        return false
    }
    System.err.println("Warning: Running on macOS without -XstartOnFirstThread JVM argument. Restarting JVM with -XstartOnFirstThread.")

    // restart jvm with -XstartOnFirstThread
    val separator = System.getProperty("file.separator")
    val classpath = System.getProperty("java.class.path")
    val mainClass = System.getenv("JAVA_MAIN_CLASS_$pid")
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
        var line: String

        while(true) {
            val line = br.readLine()
            if (line == null)
                break
            else {
                println(line)
            }
        }
        process.waitFor()
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return true
}

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
    if (!restartJVM()) {
        val applicationBuilder = ApplicationBuilder().apply { build() }
        application(applicationBuilder.program, applicationBuilder.configuration)
    }
}