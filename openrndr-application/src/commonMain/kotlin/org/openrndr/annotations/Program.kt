package org.openrndr.annotations

/**
 * Annotation to mark functions as entry points for programs
 *
 * Functions annotated with `@Program` act as uniquely identifiable entry points for
 * different programs within the module. These functions are typically defined in the
 * default package and are used to specify individual demos or applications within the
 * OPENRNDR framework.
 *
 * The `title` parameter provides a descriptive name for the program and is used for
 * identification purposes when generating program indices or running the application.
 *
 * Usage example:
 * ```
 * @Program("Descriptive Title")
 * fun ApplicationBuilder.sampleProgram() {
 *     return program {
 *          // Program implementation here
 *     }
 * }
 * ```
 *
 * @param title Descriptive name of the annotated program function.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Program(val title: String)