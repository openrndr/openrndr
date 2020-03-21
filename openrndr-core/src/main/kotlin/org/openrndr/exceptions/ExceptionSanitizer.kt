package org.openrndr.exceptions

import mu.KotlinLogging
import java.lang.Thread.UncaughtExceptionHandler

private val logger = KotlinLogging.logger(name = "─".repeat(30))


private class SanitizingUncaughtExceptionHandler : UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) { // Write the custom logic here
        findUserCause(e)
    }
}

fun installUncaughtExceptionHandler() {
    if (System.getProperty("org.openrndr.exceptions") != "JVM") {
        Thread.setDefaultUncaughtExceptionHandler(SanitizingUncaughtExceptionHandler());
    }
}


private fun color(r: Int, g:Int, b:Int):String {
    return String.format("\u001b[38;2;%d;%d;%dm", r,g,b)
}

private fun colorReset(): String {
    return "\u001b[0m"
}

fun findUserCause(throwable: Throwable) {


    logger.error(throwable) { "${throwable::class.simpleName}: ${throwable.message}"}
    logger.info("Set -Dorg.openrndr.exceptions=JVM for convential exception-handling")

    val bestSolution = throwable.stackTrace.reversed().indexOfLast { !(it.className.contains("org.openrndr")) }

    System.err.println()
    System.err.println()
    System.err.println("${color(0x7f,0x7f,0x7f)}╭ Attempting to find user cause: ${colorReset()}")
    System.err.println("${color(0x7f,0x7f,0x7f)}│${colorReset()}")
    throwable.stackTrace.reversed().forEachIndexed { index, it ->
        if (!it.className.contains("org.openrndr") && (it.lineNumber>=0)) {
            if (index == bestSolution) {
                System.err.println("${color(0x7f, 0x7f, 0x7f)}├─ ${color(0xff, 0xc0, 0xcb)}${it.className.replace("$",".")}.${it.methodName}(${it.fileName}:${it.lineNumber})${colorReset()}")
            } else {
                System.err.println("${color(0x7f, 0x7f, 0x7f)}├─ ${color(0x7f, 0x7f, 0x7f)}${it.className.replace("$",".")}.${it.methodName}(${it.fileName}:${it.lineNumber})${colorReset()}")
            }
        } else {
            System.err.println("${color(0x7f, 0x7f,0x7f)}│  ${color(0x4f,0x4f,0x4f)}${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})${colorReset()}")
        }
    }
    System.err.println("${color(0x7f,0x7f,0x7f)}│${colorReset()}")
    System.err.println("${color(0x7f, 0x7f, 0x7f)}↑ ${throwable.localizedMessage}${colorReset()}")

    if (throwable is NoSuchMethodError || throwable is ClassNotFoundException)  {
        if (throwable.message?.contains("org.openrndr") == true) {
            System.err.println()
            logger.error {
                "You are likely using incompatible versions of OPENRNDR, ORX and Panel. Fix imports and make sure to clean and rebuild your project."
            }
            System.err.println()
            System.err.println()
        }
    }
}