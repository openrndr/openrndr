package org.openrndr.draw

import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.matchers.shouldBe
import org.openrndr.math.IntVector2
import org.openrndr.math.IntVector3
import kotlin.test.Test

class TestComputeShader {

    @Test
    fun testComputeShader2DExecuteSize() {

        computeShader2DExecuteSize(
            workGroupSize = IntVector3(8, 8, 1),
            dataSize = IntVector2(639, 480),
        ) shouldBe IntVector3(80, 60, 1)

        computeShader2DExecuteSize(
            workGroupSize = IntVector3(8, 8, 1),
            dataSize = IntVector2(640, 480),
        ) shouldBe IntVector3(80, 60, 1)

        computeShader2DExecuteSize(
            workGroupSize = IntVector3(8, 8, 1),
            dataSize = IntVector2(641, 480)
        ) shouldBe IntVector3(81, 60, 1)

        computeShader2DExecuteSize(
            workGroupSize = IntVector3(8, 8, 1),
            dataSize = IntVector2(641, 481),
        ) shouldBe IntVector3(81, 61, 1)

        shouldThrowWithMessage<IllegalArgumentException>("workGroupSize.z must be 1") {
            computeShader2DExecuteSize(
                workGroupSize = IntVector3(8, 8, 2),
                dataSize = IntVector2(641, 481),
            )
        }

    }

}
