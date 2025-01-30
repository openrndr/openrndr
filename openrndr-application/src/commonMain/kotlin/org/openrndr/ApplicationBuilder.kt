package org.openrndr

/**
 * Abstract class for constructing and configuring an application. The `ApplicationBuilder` serves as the entry point
 * for setting up the application's program, configuration, and other components.
 *
 * This class provides an API for customizing various settings, linking programs, and managing displays. It also includes
 * methods that should not be called explicitly, marked as deprecated with error level, to prevent unintended behavior.
 */
abstract class ApplicationBuilder {
    abstract val configuration: Configuration
    abstract var program: Program

    abstract val applicationBase: ApplicationBase

    /**
     * Configures the application using the provided configuration block.
     *
     * Allows customization of various application settings by applying the desired
     * configurations to the [Configuration] instance.
     *
     * @param init A lambda function that operates on an instance of [Configuration].
     *             This lambda is used to define the application's configuration settings.
     */
    abstract fun configure(init: Configuration.() -> Unit)

    /**
     * Initializes and runs a program within the application.
     *
     * The provided `init` block is executed in the context of the `Program` instance,
     * allowing for configuration and definition of the program's behavior, including
     * setup, input handling, rendering, and other lifecycle-related operations.
     *
     * @param init A suspendable lambda that operates on the `Program` instance.
     *             This lambda is used to define the behavior and structure of the program.
     * @return The configured `Program` instance.
     */
    abstract fun program(init: suspend Program.() -> Unit): Program

    /**
     * A list of displays available to the application.
     *
     * Each `Display` represents a specific screen or monitor that can be used for rendering
     * or displaying application content. This property allows access to relevant display information
     * such as dimensions, positions, and display scaling. The list may include all displays connected
     * to the system depending on the graphics backend.
     */
    abstract val displays: List<Display>

    @Deprecated("Cannot construct application in an application block.", level = DeprecationLevel.ERROR)
    abstract fun application(build: ApplicationBuilder.() -> Unit): Nothing

    @Deprecated("Cannot construct program in a program block.", level = DeprecationLevel.ERROR)
    abstract fun Program.program(init: Program.() -> Unit): Nothing
}

/**
 * Constructs and configures an application using the provided builder block.
 *
 * This function serves as an entry point for defining the application's setup,
 * including its program behavior, configuration, and other components, using
 * the DSL provided by the [ApplicationBuilder].
 *
 * @param build A lambda function with a receiver of type [ApplicationBuilder].
 *              It is used to configure the application's properties and behavior.
 */
expect fun application(build: ApplicationBuilder.() -> Unit)

