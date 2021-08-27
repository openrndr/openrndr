import mu.KotlinLogging
import org.openrndr.*
import org.openrndr.extensions.Screenshots
import java.io.File
import java.util.*

/**
 * A demonstration of the produce assets event framework.
 *
 *  The framework consists of
 *  - [ProduceAssetsEvent],
 *  - [AssetMetadata],
 *  - [Program.requestAssets]
 *  - [Program.produceAssets]
 *  - [Program.assetMetadata]
 *  - [Program.assetProperties]
 *
 */

/**
 * UUIDNamer is here for demonstration purposes only. It creates a unique UUID based name everytime the
 * [Program.produceAssets] event is triggered
 */
class UUIDNamer : Extension {
    override var enabled = true
    override fun setup(program: Program) {
        val oldMetadataFunction = program.assetMetadata

        program.assetMetadata = {
            val hash = UUID.randomUUID().toString()
            program.assetProperties["hash"] = hash
            val oldMetadata = oldMetadataFunction()
            AssetMetadata(oldMetadata.programName, "$hash-${oldMetadata.assetBaseName}", program.assetProperties)
        }
        program.keyboard.keyDown.listen {
            if (it.key == KEY_SPACEBAR) {
                program.produceAssets.trigger(ProduceAssetsEvent(this, program, program.assetMetadata()))
            }
        }
    }
}

/**
 * Parameter saver listens to the [Program.produceAssets] event and saves all values in [AssetMetadata.assetProperties]
 * to a txt file.
 */
class ParameterSaver : Extension {
    override var enabled: Boolean = true
    override fun setup(program: Program) {
        program.produceAssets.listen {
            val valueText = it.assetMetadata.assetProperties.map { "${it.key}:${it.value}" }.joinToString("\n")
            val output = "${it.assetMetadata.assetBaseName}.txt"
            logger.info { "writing to $output" }
            File(output).writeText(valueText)
        }
    }

    companion object {
        val logger = KotlinLogging.logger { }
    }
}

fun main() = applicationSynchronous {
    program {
        extend(UUIDNamer())
        extend(ParameterSaver())
        extend(Screenshots()) {
            // here we tell [Screenshots] not to listen to key down events, since this is already done in HashArchiver
            // however, we could also remove the keyDown event listener in HashArchiver and remove the line below.
            listenToKeyDownEvent = false
        }
        extend {
            program.assetProperties["x"] = mouse.position.x.toString()
            program.assetProperties["y"] = mouse.position.y.toString()
            drawer.circle(mouse.position, 40.0)
        }
    }
}