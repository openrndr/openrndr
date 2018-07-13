package org.openrndr

import org.amshove.kluent.`should be`
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

object TestExtensions : Spek({

    class TE : Extension {
        override var enabled: Boolean = false
    }

    describe("Extensions") {
        it("should support enable") {
            val e = TE()
            e.enabled `should be` false
            e.enable()
            e.enabled `should be` true
        }
        it("should support disable") {
            val e = TE()
            e.enabled `should be` false
            e.disable()
            e.enabled `should be` false
        }
        it("should support toggle") {
            val e = TE()
            e.enabled `should be` false
            e.toggle()
            e.enabled `should be` true
        }
    }

})
