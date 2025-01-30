package org.openrndr

/**
 * Abstract class for constructing and configuring an application. The `ApplicationBuilder` serves as the entry point
 * for setting up the application's program, configuration, and other components.
 *
 * This class provides an API for customizing various settings, linking programs, and managing displays. It also includes
 * methods that should not be called explicitly, marked as deprecated with error level, to prevent unintended behavior.
 */
abstract class ApplicationBuilder {
    /**
     * Represents the configuration settings for initializing and building the application.
     *
     * Provides a set of configurable properties that influence the behavior and appearance
     * of the application, including window dimensions, fullscreen mode, display options,
     * cursor visibility, and rendering behavior.
     *
     * This property is abstract and must be implemented to supply the desired [Configuration]
     * instance, which can be further customized by using related configuration methods.
     */
    abstract val configuration: Configuration
    /**
     * Represents the abstract program instance associated with the application being built.
     *
     * This variable allows access and manipulation of the program's configuration and lifecycle.
     * The program instance provides functionalities such as handling input events, managing extensions,
     * controlling the application clock, and rendering graphics. It serves as the centerpiece of the
     * application, combining various elements and enabling the execution of a custom program structure.
     *
     * Implementing this property in a subclass allows defining the specific `Program` instance
     * with its unique setup, event handling, rendering logic, and other properties.
     */
    abstract var program: Program

    /**
     * Represents the fundamental base of an application within the [ApplicationBuilder].
     *
     * This property provides access to an instance of [ApplicationBase], which serves as a core
     * class containing functionality to build the final application structure.
     *
     * It is accessible before the application's construction is finalized, enabling interaction
     * with and configuration of application elements during the setup phase.
     */
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

