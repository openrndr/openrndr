package org.openrndr

import io.github.oshai.kotlinlogging.KotlinLogging
import org.openrndr.exceptions.installUncaughtExceptionHandler
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.management.ManagementFactory
import java.nio.file.FileSystems
import kotlin.jvm.optionals.getOrNull

private val logger = KotlinLogging.logger {}

/**
 * Parses a command-line style input string into a list of arguments.
 * The input may include quoted sections and escaped characters.
 *
 * @param input the command-line style input string containing arguments, spaces, quotes, and escape sequences
 * @return a list of parsed arguments as strings
 */
private fun parseCommandLineArgs(input: String): List<String> {
    val args = mutableListOf<String>()
    val currentArg = StringBuilder()
    var inQuotes = false
    var quoteChar: Char? = null
    var i = 0

    while (i < input.length) {
        val char = input[i]

        when {
            // Handle quote characters
            (char == '"' || char == '\'') && !inQuotes -> {
                inQuotes = true
                quoteChar = char
            }

            char == quoteChar && inQuotes -> {
                inQuotes = false
                quoteChar = null
            }
            // Handle escaped characters
            char == '\\' && i + 1 < input.length -> {
                val nextChar = input[i + 1]
                if (nextChar == '"' || nextChar == '\'' || nextChar == '\\') {
                    currentArg.append(nextChar)
                    i++ // Skip the next character
                } else {
                    currentArg.append(char)
                }
            }
            // Handle spaces (argument separators)
            char.isWhitespace() && !inQuotes -> {
                if (currentArg.isNotEmpty()) {
                    args.add(currentArg.toString())
                    currentArg.clear()
                }
            }
            // Regular character
            else -> {
                currentArg.append(char)
            }
        }
        i++
    }

    // Add the last argument if there's one
    if (currentArg.isNotEmpty()) {
        args.add(currentArg.toString())
    }

    return args
}

/**
 * Restarts the JVM on macOS with the `-XstartOnFirstThread` flag if certain conditions are met.
 *
 * This method performs the following checks:
 * 1. Ensures the current system is macOS or Darwin-based.
 * 2. Verifies if the JVM is already started with the `-XstartOnFirstThread` flag.
 * 3. Detects whether the JVM is launched by Gradle or has a debugger/profiler attached.
 * 4. Checks the value of `ApplicationConfiguration.restartJvmOnThread0`.
 *
 * If all conditions are met, the JVM is restarted with the required settings.
 *
 * @return `true` if the JVM was successfully restarted, or `false` if the conditions for restarting were not met.
 */
private fun restartJVM(): Boolean {
    if (!ApplicationConfiguration.checkThread0) {
        return false
    }

    // based on http://www.java-gaming.org/topics/starting-jvm-on-mac-with-xstartonfirstthread-programmatically/37697/view.html
    val osName = System.getProperty("os.name")

    // if not a mac return false
    if (!osName.startsWith("Mac") && !osName.startsWith("Darwin")) {
        return false
    }
    val bean = ManagementFactory.getRuntimeMXBean()

    // get current jvm process pid
    val pid =
        bean.name.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
    // get environment variable on whether XstartOnFirstThread is enabled
    val env = System.getenv("JAVA_STARTED_ON_FIRST_THREAD_$pid")

    // if environment variable is "1" then XstartOnFirstThread is enabled
    if (env != null && env == "1") {
        return false
    }

    // Detect if the program is launched by Gradle
    val processHandle = ProcessHandle.current()
    val parentProcess = processHandle.parent().getOrNull()
    if (parentProcess != null) {
        if (parentProcess.info().commandLine().isPresent) {
            val command = parentProcess.info().commandLine().get()
            if (command.contains("GradleDaemon")) {
                logger.warn { "Detected Gradle daemon parent process. Running without specifying -XstartOnFirstThread can cause issues. " }
            }
        }
    }

    // Detect if a debugger is attached
    if (bean.inputArguments.any { it.contains("jdwp") }) {
        error("Running on macOS with a debugger attached, but not on the first thread. The process is stopped to prevent the debugger from losing the process. Run with -XstartOnFirstThread to continue.")
    }
    // Detect if a profiler is attached
    if (bean.inputArguments.any { it.contains("jfrsync=profile") }) {
        error("Running on macOS with a profiler attached, but not on the first thread. The process is stopped to prevent the profiler from losing the process. Run with -XstartOnFirstThread to continue.")
    }
    if (!ApplicationConfiguration.restartJvmOnThread0) {
        error("Running on macOS but not on the first thread and ApplicationConfiguration.restartJvmOnThread0 is set to false. Run with -XstartOnFirstThread to continue.")
    }

    // restart jvm with -XstartOnFirstThread
    val separator = FileSystems.getDefault().separator ?: error("file.separator not set")
    val classpath = System.getProperty("java.class.path") ?: error("java.class.path not set")

    val mainClassAndArguments = System.getenv("JAVA_MAIN_CLASS_$pid") ?: System.getProperty("sun.java.command")
    ?: error("JAVA_MAIN_CLASS_$pid and sun.java.command not set")
    val jvmPath = System.getProperty("java.home") + separator + "bin" + separator + "java"

    val inputArguments = ManagementFactory.getRuntimeMXBean().inputArguments
    val jvmArgs = ArrayList<String>()

    jvmArgs.add(jvmPath)
    jvmArgs.add("-XstartOnFirstThread")
    jvmArgs.addAll(inputArguments)
    jvmArgs.add("-cp")
    jvmArgs.add(classpath)
    jvmArgs.addAll(parseCommandLineArgs(mainClassAndArguments))

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

actual fun application(build: ApplicationBuilder.() -> Unit) {
    if (!restartJVM()) {
        installUncaughtExceptionHandler()
        val result: Application
        ApplicationBuilderJVM().apply {
            build()
            result = applicationBase.build(this.program, this.configuration)
            result.run()
        }
    }
}

@Suppress("DeprecatedCallableAddReplaceWith")
class ApplicationBuilderJVM : ApplicationBuilder() {
    override val configuration = Configuration()
    override var program: Program = ProgramImplementation()
    override val applicationBase: ApplicationBase = ApplicationBase.initialize()
    override val displays by lazy { applicationBase.displays }

    override fun configure(init: Configuration.() -> Unit) {
        configuration.init()
    }

    override fun program(init: suspend Program.() -> Unit): Program {
        program = object : ProgramImplementation() {
            override suspend fun setup() {
                init()
            }
        }
        return program
    }

    fun run(): Application {
        val result = applicationBase.build(this.program, this.configuration)
        result.run()
        return result
    }

    @Deprecated("Cannot construct application in an application block.", level = DeprecationLevel.ERROR)
    override fun application(build: ApplicationBuilder.() -> Unit): Nothing =
        error("Cannot construct application in an application block.")

    @Deprecated("Cannot construct program in a program block.", level = DeprecationLevel.ERROR)
    override fun Program.program(init: Program.() -> Unit): Nothing =
        error("Cannot construct program in a program block.")
}
