package org.openrndr

import org.openrndr.draw.Drawer

@DslMarker
annotation class ExtensionDslMarker

/**
 * Represents the various stages of an extension's lifecycle in the application.
 *
 * SETUP: This stage is used to initialize and configure the extension before any rendering starts.
 * BEFORE_DRAW: This stage occurs just before the drawing of a frame begins, allowing extensions to perform pre-drawing operations.
 * AFTER_DRAW: This stage occurs immediately after the frame is drawn, allowing extensions to perform post-drawing operations.
 */
enum class ExtensionStage {
    /**
     * Indicates the setup stage in an extension's lifecycle.
     *
     * This stage is used for initializing and configuring the extension
     * before any rendering begins.
     */
    SETUP,
    /**
     * Represents the stage in an extension's lifecycle that occurs just before the drawing of a frame begins.
     *
     * This stage allows extensions to perform operations or updates that need to be completed right before rendering starts.
     */
    BEFORE_DRAW,
    /**
     * Represents the stage in an extension's lifecycle that occurs immediately after the frame is drawn.
     *
     * This stage allows extensions to perform operations or cleanup tasks that need to be executed
     * post-rendering, such as resource management or updating states that depend on the completed frame.
     */
    AFTER_DRAW
}

/**
 * Represents a host for managing and executing extensions within a program.
 * This interface provides methods for adding, configuring, and executing extensions
 * at different stages of the program's lifecycle.
 */
interface ExtensionHost : InputEvents {
    /**
     * A mutable list of extensions associated with the host.
     * Extensions allow adding custom behavior or extending the functionality
     * of the framework at various lifecycle stages such as setup, before draw, and after draw.
     */
    val extensions: MutableList<Extension>

    /**
     * Adds an extension to the host and returns the extension instance.
     * The extension allows customization or extension of the host's functionality
     * at various lifecycle stages such as setup, before draw, and after draw.
     *
     * @param extension The extension instance to be added to the host.
     * @return The same extension instance that was added.
     */
    fun <T : Extension> extend(extension: T): T


    /**
     * Adds an extension to the host and applies a configuration block to it. This method allows customization
     * or extension of the host's functionality by invoking a user-defined configuration on the provided extension.
     *
     * @param extension The extension instance to be added and configured.
     * @param configure A lambda function defining the configuration to be applied to the extension.
     * @return The configured extension instance.
     */
    fun <T : Extension> extend(extension: T, configure: T.() -> Unit): T

    /**
     * Adds an extension to the program that executes a user-defined block of code at a specified extension stage.
     * The execution stage can be `SETUP`, `BEFORE_DRAW`, or `AFTER_DRAW`. This method prevents nesting of `extend` calls.
     *
     * @param stage The stage in the program's lifecycle where the extension will be executed.
     * @param userDraw The block of code to be executed as part of the extension.
     */
    fun extend(stage: ExtensionStage = ExtensionStage.BEFORE_DRAW, userDraw: Program.() -> Unit)

    val program: Program
}


/**
 * Represents an interface for creating extensions to enhance the functionality of a host program.
 * Extensions allow developers to hook into the lifecycle of the program, enabling custom behavior
 * during setup, drawing, and shutdown phases.
 */
@ExtensionDslMarker
interface Extension {
    var enabled: Boolean

    /**
     * Allows an extension to perform its setup process with the given [Program] instance.
     *
     * This method is intended to provide a way for extensions to configure themselves or
     * interact with the program during its initialization phase.
     *
     * @param program the program instance associated with this setup process, used to configure
     * or initialize the extension.
     */
    fun setup(program: Program) {}

    /**
     * Invoked before the drawing phase of the program's lifecycle, allowing the extension to
     * perform custom operations or adjustments prior to rendering.
     *
     * @param drawer the drawer instance used to render graphical elements.
     * @param program the program instance associated with the current drawing cycle.
     */
    fun beforeDraw(drawer: Drawer, program: Program) {}

    /**
     * Invoked after the drawing phase of the program's lifecycle, allowing the extension to execute
     * any custom logic or operations needed post-rendering.
     *
     * @param drawer the drawer instance used to perform rendering operations during the drawing cycle.
     * @param program the program instance associated with the current drawing lifecycle.
     */
    fun afterDraw(drawer: Drawer, program: Program) {}


    /**
     * Allows the extension to perform clean-up or resource deallocation tasks during
     * the shutdown phase of the program's lifecycle.
     *
     * This method is called when the program is ending, enabling the extension to
     * release resources or save the necessary state before termination.
     *
     * @param program the program instance associated with this shutdown process, used
     * to finalize and clean up any resources or state related to the extension.
     */
    fun shutdown(program: Program) {}
}
