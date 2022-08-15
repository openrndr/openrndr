import kotlinx.coroutines.runBlocking
import org.openrndr.Configuration
import org.openrndr.Program
import org.openrndr.internal.gl3.ApplicationGLFWGL3

internal fun Program.initializeGLFWGL3Application(): Program {
    ApplicationGLFWGL3(this, Configuration()).also {
        runBlocking { it.setup() }
        it.preloop()
    }
    return this
}