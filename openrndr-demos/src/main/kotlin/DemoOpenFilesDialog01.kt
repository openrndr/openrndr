import org.openrndr.application
import org.openrndr.dialogs.openFilesDialog

fun main() {
    application {
        program {
            openFilesDialog {
                println("opening ${it.joinToString(", ") { it.path }}")
            }
        }
    }
}