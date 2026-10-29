@file:OptIn(ExperimentalWasmJsInterop::class)

import kotlinx.coroutines.test.runTest
import org.openrndr.fontdriver.harfbuzzjs.FontDriverHarfBuzzJs
import org.openrndr.fontdriver.harfbuzzjs.loadHarfBuzzModule
import kotlin.test.Test
import kotlin.test.assertNotNull

/** the root of the repository, null when the tests do not run in Node.js */
internal fun repositoryRoot(): String? = repositoryRootVariable()?.toString()

private fun repositoryRootVariable(): JsString? =
    js("(globalThis.process && globalThis.process.env && globalThis.process.env.OPENRNDR_ROOT) || null")

class TestHarfBuzzLoading {
    @Test
    fun loadsHarfBuzz() = runTest {
        assertNotNull(loadHarfBuzzModule())
        assertNotNull(FontDriverHarfBuzzJs.create())
    }
}
