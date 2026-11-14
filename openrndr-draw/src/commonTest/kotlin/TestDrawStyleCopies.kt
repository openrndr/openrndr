package org.openrndr.draw

import org.openrndr.color.ColorRGBa
import org.openrndr.math.Matrix55
import org.openrndr.shape.FillRule
import org.openrndr.shape.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/**
 * [DrawStyle.setFrom] sets every property, like [DrawStyle.copy] does, [DrawStyle.deepCopy] copies the stencils too
 */
class TestDrawStyleCopies {
    /** a style with no default property */
    private fun style(): DrawStyle {
        val stencil = StencilStyle(
            StencilOperation.INCREASE, StencilOperation.DECREASE, StencilOperation.INVERT, 0x0f, 3, 0xf0,
            StencilTest.EQUAL
        )
        return DrawStyle(
            clip = Rectangle(1.0, 2.0, 3.0, 4.0),
            fill = ColorRGBa.RED,
            stroke = ColorRGBa.BLUE,
            lineCap = LineCap.ROUND,
            lineJoin = LineJoin.BEVEL,
            strokeWeight = 7.0,
            smooth = false,
            miterLimit = 2.0,
            quality = DrawQuality.PERFORMANCE,
            depthTestPass = DepthTestPass.LESS,
            depthWrite = true,
            blendMode = BlendMode.ADD,
            cullTestPass = CullTestPass.BACK,
            channelWriteMask = ChannelMask(red = true, green = false, blue = true, alpha = false),
            alphaToCoverage = true,
            shadeStyle = ShadeStyle(),
            fontMap = null,
            kerning = KernMode.DISABLED,
            textSetting = TextSettingMode.PIXEL,
            stencil = stencil,
            frontStencil = stencil.copy(stencilTestReference = 4),
            backStencil = stencil.copy(stencilTestReference = 5),
            colorMatrix = Matrix55(c0r0 = 2.0, c1r1 = 1.0, c2r2 = 1.0, c3r3 = 1.0, c4r4 = 1.0),
            strokeAlignment = StrokeAlignment.INSIDE,
            fillRule = FillRule.EVEN_ODD,
            paintOrder = PaintOrder.STROKE_FILL,
            lineBlendOnce = true,
            lineWidthMode = LineWidthMode.SCREEN
        )
    }

    @Test
    fun `setFrom sets every property`() {
        val style = style()
        val target = DrawStyle()
        target.setFrom(style)
        assertEquals(style, target)
        assertSame(style.stencil, target.stencil)
    }

    @Test
    fun `stencil setFrom sets every property`() {
        val stencil = style().stencil
        val target = StencilStyle()
        target.setFrom(stencil)
        assertEquals(stencil, target)
    }

    @Test
    fun `deep copies have stencils of their own`() {
        val style = style()
        val copy = style.deepCopy()
        assertEquals(style, copy)
        assertNotSame(style.stencil, copy.stencil)
        assertNotSame(style.frontStencil, copy.frontStencil)
        assertNotSame(style.backStencil, copy.backStencil)

        // a style whose front and back stencils are its stencil keeps them shared in the copy
        val shared = DrawStyle()
        val sharedCopy = shared.deepCopy()
        assertSame(sharedCopy.stencil, sharedCopy.frontStencil)
        assertSame(sharedCopy.stencil, sharedCopy.backStencil)
        sharedCopy.stencil.stencilTest = StencilTest.NEVER
        assertEquals(StencilTest.DISABLED, shared.stencil.stencilTest)
    }
}
