package org.openrndr.internal.glfw

import org.lwjgl.glfw.GLFW.glfwGetCurrentContext
import org.openrndr.draw.Session
import org.openrndr.internal.ResourceThread
import org.openrndr.internal.gl3.DriverGL3
import org.openrndr.internal.gl3.DriverVersionGL

class DriverGLGLFW(versionGL: DriverVersionGL) : DriverGL3(versionGL) {
    override val contextID: Long
        get() {
            return glfwGetCurrentContext()
        }

    override fun createResourceThread(session: Session?, f: () -> Unit): ResourceThread {
        return ResourceThreadGLFW.create(f)
    }

}