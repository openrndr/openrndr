@file:OptIn(ExperimentalWasmJsInterop::class)

package org.openrndr.fontdriver.harfbuzzjs

import js.promise.Promise
import js.promise.await
import js.typedarrays.Float64Array
import js.typedarrays.Int32Array
import js.typedarrays.Uint8Array

/**
 * The harfbuzzjs module, https://github.com/harfbuzz/harfbuzzjs
 */
external interface HarfBuzzModule : JsAny

/**
 * A harfbuzzjs object, a face, a font or a glyph path
 */
external interface HarfBuzzObject : JsAny

private fun importHarfBuzz(): Promise<HarfBuzzModule> = js("import('harfbuzzjs')")

/**
 * Loads the harfbuzzjs module, which instantiates its WebAssembly module
 */
suspend fun loadHarfBuzzModule(): HarfBuzzModule = importHarfBuzz().await()

// -- loading font data

/** reads a file or url synchronously, from the file system in Node.js and with a synchronous request in browsers */
internal fun readBytesSync(fileOrUrl: String): Uint8Array<*> = js(
    """(() => {
        const process = globalThis.process;
        const node = process && process.getBuiltinModule ? process.getBuiltinModule('fs') : undefined;
        if (node && !/^[a-z]+:\/\//i.test(fileOrUrl)) {
            return new Uint8Array(node.readFileSync(fileOrUrl));
        }
        const request = new XMLHttpRequest();
        request.open('GET', fileOrUrl, false);
        request.overrideMimeType('text/plain; charset=x-user-defined');
        request.send(null);
        if (request.status !== 200 && request.status !== 0) {
            throw new Error('failed to load ' + fileOrUrl + ': ' + request.status);
        }
        const text = request.responseText;
        const bytes = new Uint8Array(text.length);
        for (let i = 0; i < text.length; i++) {
            bytes[i] = text.charCodeAt(i) & 0xff;
        }
        return bytes;
    })()"""
)

private fun fetchBytes(fileOrUrl: String): Promise<Uint8Array<*>> = js(
    """(() => {
        const process = globalThis.process;
        const node = process && process.getBuiltinModule ? process.getBuiltinModule('fs') : undefined;
        if (node && !/^[a-z]+:\/\//i.test(fileOrUrl)) {
            return node.promises.readFile(fileOrUrl).then((data) => new Uint8Array(data));
        }
        return fetch(fileOrUrl).then((response) => {
            if (!response.ok) {
                throw new Error('failed to load ' + fileOrUrl + ': ' + response.status);
            }
            return response.arrayBuffer();
        }).then((buffer) => new Uint8Array(buffer));
    })()"""
)

/** reads a file or url asynchronously, from the file system in Node.js and with fetch in browsers */
internal suspend fun readBytes(fileOrUrl: String): Uint8Array<*> = fetchBytes(fileOrUrl).await()

internal fun byteArrayToUint8Array(bytes: ByteArray): Uint8Array<*> {
    val array = createUint8Array(bytes.size)
    for (i in bytes.indices) {
        setUint8(array, i, bytes[i].toInt() and 0xff)
    }
    return array
}

private fun createUint8Array(size: Int): Uint8Array<*> = js("new Uint8Array(size)")
private fun setUint8(array: Uint8Array<*>, index: Int, value: Int): Unit = js("array[index] = value")

// -- faces and fonts

internal fun hbCreateFace(hb: HarfBuzzModule, data: Uint8Array<*>, index: Int): HarfBuzzObject =
    js("new hb.Face(new hb.Blob(data), index)")

internal fun hbCreateFont(hb: HarfBuzzModule, face: HarfBuzzObject): HarfBuzzObject = js("new hb.Font(face)")

internal fun hbUpem(face: HarfBuzzObject): Int = js("face.upem")

/** ascender, descender and line gap in font units */
internal fun hbHorizontalExtents(font: HarfBuzzObject): Int32Array<*> =
    js("(() => { const e = font.hExtents(); return new Int32Array([e.ascender, e.descender, e.lineGap]); })()")

internal fun hbCollectUnicodes(face: HarfBuzzObject): Int32Array<*> = js("new Int32Array(face.collectUnicodes())")

/** the glyph for a code point, -1 when the face has no glyph for it */
internal fun hbNominalGlyph(font: HarfBuzzObject, codePoint: Int): Int =
    js("(() => { const glyph = font.glyph(codePoint); return glyph === undefined ? -1 : glyph; })()")

internal fun hbGlyphHAdvance(font: HarfBuzzObject, glyph: Int): Int = js("font.glyphHAdvance(glyph)")

/** x bearing, y bearing, width and height in font units, height is negative for y-up outlines */
internal fun hbGlyphExtents(font: HarfBuzzObject, glyph: Int): Int32Array<*> =
    js("(() => { const e = font.glyphExtents(glyph); return e ? new Int32Array([e.xBearing, e.yBearing, e.width, e.height]) : new Int32Array(4); })()")

/** the outline of a glyph in font units, as a path of commands and their values */
internal fun hbGlyphPath(font: HarfBuzzObject, glyph: Int): HarfBuzzObject = js(
    """(() => {
        const commands = font.glyphToJson(glyph);
        let count = 0;
        for (const c of commands) count += c.values.length;
        const values = new Float64Array(count);
        let types = '';
        let i = 0;
        for (const c of commands) {
            types += c.type;
            for (const v of c.values) values[i++] = v;
        }
        return { types: types, values: values };
    })()"""
)

internal fun hbPathTypes(path: HarfBuzzObject): String = js("path.types")
internal fun hbPathValues(path: HarfBuzzObject): Float64Array<*> = js("path.values")

/** reads a big-endian 16-bit signed integer at [offset] in [table], 0 when the face has no such table */
internal fun hbTableInt16(face: HarfBuzzObject, table: String, offset: Int): Int = js(
    """(() => {
        const data = face.referenceTable(table);
        if (!data || data.byteLength < offset + 2) return 0;
        return new DataView(data.buffer, data.byteOffset, data.byteLength).getInt16(offset);
    })()"""
)

// -- variations

/** the variation axes of a face, sorted by axis index */
internal fun hbAxes(face: HarfBuzzObject): HarfBuzzObject =
    js("Object.values(face.getAxisInfos()).sort((a, b) => a.axisIndex - b.axisIndex)")

internal fun hbAxisCount(axes: HarfBuzzObject): Int = js("axes.length")
internal fun hbAxisTag(axes: HarfBuzzObject, index: Int): String = js("axes[index].tag")
internal fun hbAxisMin(axes: HarfBuzzObject, index: Int): Double = js("axes[index].min")
internal fun hbAxisDefault(axes: HarfBuzzObject, index: Int): Double = js("axes[index].default")
internal fun hbAxisMax(axes: HarfBuzzObject, index: Int): Double = js("axes[index].max")

/** the English name of an axis, its tag when the face has no name for it */
internal fun hbAxisName(face: HarfBuzzObject, axes: HarfBuzzObject, index: Int): String =
    js("(() => { const a = axes[index]; let name = ''; try { name = face.getName(a.nameId, 'en'); } catch (e) {} return name || a.tag; })()")

/** sets the variation of [font], [tags] are comma separated axis tags with [values] in the same order */
internal fun hbSetVariations(hb: HarfBuzzModule, font: HarfBuzzObject, tags: String, values: Float64Array<*>): Unit = js(
    "font.setVariations(tags.length === 0 ? [] : tags.split(',').map((tag, i) => new hb.Variation(tag, values[i])))"
)

internal fun createFloat64Array(values: DoubleArray): Float64Array<*> {
    val array = newFloat64Array(values.size)
    for (i in values.indices) {
        setFloat64(array, i, values[i])
    }
    return array
}

private fun newFloat64Array(size: Int): Float64Array<*> = js("new Float64Array(size)")
private fun setFloat64(array: Float64Array<*>, index: Int, value: Double): Unit = js("array[index] = value")

internal fun int32At(array: Int32Array<*>, index: Int): Int = js("array[index]")
internal fun int32Length(array: Int32Array<*>): Int = js("array.length")
internal fun float64At(array: Float64Array<*>, index: Int): Double = js("array[index]")
internal fun float64Length(array: Float64Array<*>): Int = js("array.length")

// -- OpenType layout queries

/** the OpenType script tags for an ISO 15924 script tag, preferred tag first, comma separated */
fun hbOpenTypeScriptTags(hb: HarfBuzzModule, script: String): String =
    js("hb.otTagsFromScriptAndLanguage(script, '').scriptTags.join(',')")

/** the script tags of the GSUB or GPOS table of a face, in table order, comma separated */
fun hbTableScriptTags(face: HarfBuzzObject, table: String): String = js("face.getTableScriptTags(table).join(',')")

/** the feature tags of the default language of a script in the GSUB or GPOS table of a face, comma separated */
fun hbDefaultLanguageFeatureTags(face: HarfBuzzObject, table: String, scriptIndex: Int): String =
    js("face.getLanguageFeatureTags(table, scriptIndex, 0xFFFF).join(',')")
