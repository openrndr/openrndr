import org.openrndr.application
import org.openrndr.dialogs.saveFileDialog

fun main() {
    application {
        program {

            val extensions = listOf(
                "Images" to listOf("png", "jpg"),
                "Vector graphics" to listOf("svg")

            )

            saveFileDialog(suggestedFilename = "henk", supportedExtensions = extensions) {
                println(it)
            }
        }
    }
}