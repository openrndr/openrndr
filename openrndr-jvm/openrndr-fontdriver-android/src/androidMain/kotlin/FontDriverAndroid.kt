package org.openrndr.fontdriver.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Typeface
import org.openrndr.draw.font.Face
import org.openrndr.draw.font.Glyph
import org.openrndr.draw.font.internal.FontDriver
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle
import org.openrndr.shape.Shape
import org.openrndr.utils.buffer.MPPBuffer
import java.io.File
import java.net.URL
import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

class FaceAndroid(
    private val fontBytes: ByteArray,
    val typeface: Typeface
) : Face {

    private val ttf = TtfReader(fontBytes)

    // Parse once
    private val head = ttf.readHead()
    private val hhea = ttf.readHhea()
    private val cmap = ttf.readCmapCodePoints()

    // Paint used for kerning approximation (via measuring pair advance)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = this@FaceAndroid.typeface
        isSubpixelText = true
        isLinearText = true
        // If you want to strongly encourage kerning where supported:
        // if (android.os.Build.VERSION.SDK_INT >= 21) fontFeatureSettings = "kern"
    }

    override fun allCodePoints(): Sequence<Int> = cmap.asSequence()

    override fun unitsPerEm(): Int = head.unitsPerEm

    override fun ascentMetrics(): Int = hhea.ascent
    override fun descentMetrics(): Int = hhea.descent
    override fun lineGapMetrics(): Int = hhea.lineGap

    override fun kernAdvance(scale: Double, left: Char, right: Char): Double {
        // We compute kerning in "font units" by measuring at textSize = unitsPerEm
        // then multiply by scale (pixels per unit) like STBTT does.
        val upem = unitsPerEm().coerceAtLeast(1)
        paint.textSize = upem.toFloat()

        val sL = left.toString()
        val sR = right.toString()
        val sLR = "${left}${right}"

        val wL = paint.measureText(sL)
        val wR = paint.measureText(sR)
        val wLR = paint.measureText(sLR)

        val kernInUnits = (wLR - (wL + wR)).toDouble()
        return kernInUnits * scale
    }

    override fun glyphForCharacter(character: Char): Glyph {
        return GlyphAndroid(this, character.code)
    }

    override fun glyphForCodePoint(codePoint: Int): Glyph {
        return GlyphAndroid(this, codePoint)
    }

    override fun bounds(scale: Double): Rectangle {
        val x0 = head.xMin * scale
        val y0 = head.yMin * scale
        val x1 = head.xMax * scale
        val y1 = head.yMax * scale
        // head bbox is in up=+y; your STB impl flips to up=-y:
        return Rectangle(x0, -y0, x1 - x0, -(y1 - y0)).normalized
    }

    override fun close() {
        // nothing to free (fontBytes is a normal ByteArray)
    }

    // Expose paint + upem if your Glyph implementation needs it
    internal fun paintAtUnitsPerEm(): Paint = paint.apply { textSize = unitsPerEm().toFloat() }
}

class GlyphAndroid(
    private val face: FaceAndroid,
    private val codePoint: Int
) : Glyph {

    private fun codePointString(): String = String(Character.toChars(codePoint))

    /**
     * Convert OPENRNDR's scale (font-units -> pixels) into an Android textSize in pixels.
     * If scale = pixelsPerUnit, then textSize(px) = unitsPerEm * pixelsPerUnit.
     */
    private fun textSizePx(scale: Double): Float =
        (face.unitsPerEm().toDouble() * scale).toFloat()

    private fun configuredPaint(scale: Double, subpixel: Boolean = true): Paint {
        val p = face.paintAtUnitsPerEm() // returns a Paint already configured with typeface
        p.textSize = textSizePx(scale)
        p.isSubpixelText = subpixel
        p.isLinearText = true
        p.isAntiAlias = true
        // You can tune hinting if you want:
        // p.hinting = Paint.HINTING_ON
        return p
    }

    override fun shape(scale: Double): Shape {
        val s = codePointString()
        val paint = configuredPaint(scale, subpixel = false)

        // Build a glyph outline path. Baseline at y=0, x=0.
        val path = Path()
        paint.getTextPath(s, 0, s.length, 0f, 0f, path)

        // Android path coordinates are in a y-down screen-like space.
        // Your Glyph docs say: "bounds in up=+y space". OPENRNDR typically uses up=+y.
        // So we flip Y here (around baseline).
        val m = Matrix().apply { setScale(1f, -1f) }
        path.transform(m)

        return pathToShape(path)
    }

    override fun advanceWidth(scale: Double): Double {
        val s = codePointString()
        val paint = configuredPaint(scale)
        return paint.measureText(s).toDouble()
    }

    override fun leftSideBearing(scale: Double): Double {
        // Android doesn't expose per-glyph LSB directly.
        // We can approximate via path bounds: left extent relative to origin.
        val r = bounds(scale)
        return r.x // left edge from origin in up=+y
    }

    override fun topSideBearing(scale: Double): Double {
        // TSB isn't directly exposed either; we approximate as top extent above baseline.
        val r = bounds(scale)
        return r.y + r.height // since r.y is bottom in up=+y after normalization, this depends on Rectangle convention.
    }

    override fun bounds(scale: Double): Rectangle {
        // Compute outline bounds in up=+y space.
        val s = codePointString()
        val paint = configuredPaint(scale, subpixel = false)

        val path = Path()
        paint.getTextPath(s, 0, s.length, 0f, 0f, path)

        val bounds = RectF()
        path.computeBounds(bounds, true)

        // bounds currently in y-down; convert to y-up by flipping around baseline (y=0):
        // y_up = -y_down
        val x0 = bounds.left.toDouble()
        val x1 = bounds.right.toDouble()
        val y0 = (-bounds.bottom).toDouble()
        val y1 = (-bounds.top).toDouble()

        return Rectangle(x0, y0, x1 - x0, y1 - y0).normalized
    }

    override fun bitmapBounds(scale: Double, subpixel: Boolean): IntRectangle {
        val s = codePointString()
        val paint = configuredPaint(scale, subpixel = subpixel).apply {
            style = Paint.Style.FILL
        }

        val path = Path()
        paint.getTextPath(s, 0, s.length, 0f, 0f, path)

        val b = RectF()
        path.computeBounds(b, true)

        // Padding to avoid clipping from AA/subpixel
        val pad = if (subpixel) 2f else 1f

        val left = floor(b.left - pad).toInt()
        val top = floor(b.top - pad).toInt()
        val right = ceil(b.right + pad).toInt()
        val bottom = ceil(b.bottom + pad).toInt()

        return IntRectangle(left, top, right - left, bottom - top)
    }

    override fun rasterize(
        scale: Double,
        bitmap: MPPBuffer,
        stride: Int,
        subpixel: Boolean
    ) {
        val s = codePointString()
        val paint = configuredPaint(scale, subpixel = subpixel).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }

        val bb = bitmapBounds(scale, subpixel)
        val w = max(1, bb.width)
        val h = max(1, bb.height)

        require(stride >= w) { "stride($stride) < glyphWidth($w)" }
        require(bitmap.capacity() >= stride * h) {
            "MPPBuffer too small: cap=${bitmap.capacity()} need=${stride * h} (stride=$stride, h=$h)"
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ALPHA_8)
        val canvas = Canvas(out)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val path = Path()
        paint.getTextPath(s, 0, s.length, 0f, 0f, path)

        // Translate so bb's top-left becomes (0,0) in the bitmap
        canvas.translate((-bb.x).toFloat(), (-bb.y).toFloat())
        canvas.drawPath(path, paint)

        // Copy ALPHA_8 pixels
        val alpha = ByteArray(w * h)
        out.copyPixelsToBuffer(ByteBuffer.wrap(alpha))

        writeAlpha8ToMppBuffer(
            src = alpha,
            srcWidth = w,
            srcHeight = h,
            dst = bitmap,
            dstStride = stride
        )
    }

    /**
     * Adapter: convert an Android Path (already in up=+y space) to OPENRNDR Shape.
     * You likely already have a vector/path builder in OPENRNDR; plug it in here.
     */
    private fun pathToShape(path: Path): Shape {
        // TODO: implement using OPENRNDR's ShapeContour/Shape building utilities.
        // Common approaches:
        // - Use Path.approximate(...) then build contours
        // - Use android.graphics.PathIterator (API 26+) to read segments precisely
        error("pathToShape(path) not implemented")
    }

    private fun writeAlpha8ToMppBuffer(
        src: ByteArray,
        srcWidth: Int,
        srcHeight: Int,
        dst: MPPBuffer,
        dstStride: Int
    ) {
        require(dstStride >= srcWidth) { "dstStride($dstStride) < srcWidth($srcWidth)" }
        require(src.size >= srcWidth * srcHeight) { "src too small" }

        val dstBB: ByteBuffer = dst.byteBuffer.duplicate()
        val base = dstBB.position()
        val limit = dstBB.limit() // region limit (often set by slicer)

        val needed = base + dstStride * srcHeight
        require(needed <= limit) {
            "dst region too small: need up to $needed, but limit=$limit (base=$base, stride=$dstStride, h=$srcHeight)"
        }

        var srcOff = 0
        for (y in 0 until srcHeight) {
            dstBB.position(base + y * dstStride) // write relative to base
            dstBB.put(src, srcOff, srcWidth)
            srcOff += srcWidth
        }
    }
}

class FontDriverAndroid(
    private val context: Context
) : FontDriver {

    override fun loadFace(fileOrUrl: String): Face {
        val source = resolveSource(fileOrUrl)

        val bytes = source.readBytes()
            ?: error("no content for file or url: '$fileOrUrl'")

        val typeface = source.buildTypeface(bytes)
            ?: error("failed to create Typeface for '$fileOrUrl'")

        return FaceAndroid(bytes, typeface)
    }

    private sealed class Source {
        abstract fun readBytes(): ByteArray?
        abstract fun buildTypeface(bytes: ByteArray): Typeface?
    }

    private fun resolveSource(fileOrUrl: String): Source {
        val fileOrUrl = fileOrUrl.removePrefix("file:")
        return when {
            fileOrUrl.startsWith("data/") -> {
                AssetSource(fileOrUrl)
            }

            fileOrUrl.startsWith("http://") || fileOrUrl.startsWith("https://") -> {
                UrlSource(fileOrUrl)
            }

            else -> FileSource(File(fileOrUrl))
        }
    }

    private inner class AssetSource(private val assetPath: String) : Source() {
        override fun readBytes(): ByteArray =
            context.assets.open(assetPath).use { it.readBytes() }

        override fun buildTypeface(bytes: ByteArray): Typeface? {
            // Best path for assets: no temp file needed
            return Typeface.createFromAsset(context.assets, assetPath)
        }
    }

    private inner class FileSource(private val file: File) : Source() {
        override fun readBytes(): ByteArray? =
            if (file.exists()) file.readBytes() else null

        override fun buildTypeface(bytes: ByteArray): Typeface? {
            if (!file.exists()) return null
            return Typeface.createFromFile(file)
        }
    }

    private inner class UrlSource(private val url: String) : Source() {
        override fun readBytes(): ByteArray =
            URL(url).openStream().use { it.readBytes() }

        override fun buildTypeface(bytes: ByteArray): Typeface? {
            // Need a file-backed typeface → write to cache
            val tmp = File.createTempFile("openrndr-font-", ".ttf", context.cacheDir)
            tmp.outputStream().use { it.write(bytes) }

            // Many OEMs are fine if you delete after creating; some are flaky.
            // If you want maximum safety, keep the file (or manage cleanup yourself).
            return Typeface.createFromFile(tmp)
        }
    }
}