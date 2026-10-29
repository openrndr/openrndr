@file:OptIn(ExperimentalWasmJsInterop::class)

package org.openrndr.imagedriver.browser

import js.buffer.ArrayBuffer
import js.buffer.DataView
import js.promise.Promise
import js.promise.await
import org.openrndr.utils.buffer.MPPBuffer

/**
 * An image decoded by the browser
 */
internal external interface DecodedImage : JsAny

// -- reading files

/**
 * Reads a file or url synchronously: base64 data urls are decoded, files are read from the file system in Node.js and
 * urls with a synchronous request in browsers
 */
private fun readSync(fileOrUrl: String): DataView<ArrayBuffer> = js(
    """(() => {
        let bytes;
        const process = globalThis.process;
        const fs = process && process.getBuiltinModule ? process.getBuiltinModule('fs') : undefined;
        if (fileOrUrl.indexOf('data:') === 0) {
            const binary = atob(fileOrUrl.substring(fileOrUrl.indexOf(',') + 1));
            bytes = new Uint8Array(binary.length);
            for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
        } else if (fs && !/^[a-z][a-z0-9+.-]+:\/\//i.test(fileOrUrl)) {
            bytes = new Uint8Array(fs.readFileSync(fileOrUrl));
        } else {
            const request = new XMLHttpRequest();
            request.open('GET', fileOrUrl, false);
            request.overrideMimeType('text/plain; charset=x-user-defined');
            request.send(null);
            if (request.status !== 200 && request.status !== 0) {
                throw new Error('failed to load ' + fileOrUrl + ': ' + request.status);
            }
            const text = request.responseText;
            bytes = new Uint8Array(text.length);
            for (let i = 0; i < text.length; i++) bytes[i] = text.charCodeAt(i) & 0xff;
        }
        return new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
    })()"""
)

private fun readAsync(fileOrUrl: String): Promise<DataView<ArrayBuffer>> = js(
    """(() => {
        const process = globalThis.process;
        const fs = process && process.getBuiltinModule ? process.getBuiltinModule('fs') : undefined;
        const view = (bytes) => new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
        if (fs && fileOrUrl.indexOf('data:') !== 0 && !/^[a-z][a-z0-9+.-]+:\/\//i.test(fileOrUrl)) {
            return fs.promises.readFile(fileOrUrl).then((data) => view(new Uint8Array(data)));
        }
        return fetch(fileOrUrl).then((response) => {
            if (!response.ok) {
                throw new Error('failed to load ' + fileOrUrl + ': ' + response.status);
            }
            return response.arrayBuffer();
        }).then((buffer) => new DataView(buffer));
    })()"""
)

internal fun readFileSync(fileOrUrl: String): MPPBuffer = MPPBuffer(readSync(fileOrUrl))

internal suspend fun readFile(fileOrUrl: String): MPPBuffer = MPPBuffer(readAsync(fileOrUrl).await())

// -- decoding

/**
 * Decodes an image with the browser, to RGBA bytes with the bottom row first and alpha not premultiplied
 */
private fun decode(bytes: DataView<ArrayBuffer>, offset: Int, length: Int): Promise<DecodedImage> = js(
    """(() => {
        if (typeof createImageBitmap === 'undefined' || typeof OffscreenCanvas === 'undefined') {
            throw new Error('decoding images needs createImageBitmap and OffscreenCanvas, which are only available in browsers');
        }
        const blob = new Blob([new Uint8Array(bytes.buffer, bytes.byteOffset + offset, length)]);
        const options = { imageOrientation: 'flipY', premultiplyAlpha: 'none', colorSpaceConversion: 'none' };
        return createImageBitmap(blob, options).then((bitmap) => {
            const canvas = new OffscreenCanvas(bitmap.width, bitmap.height);
            const context = canvas.getContext('2d', { willReadFrequently: true });
            context.drawImage(bitmap, 0, 0);
            const pixels = context.getImageData(0, 0, bitmap.width, bitmap.height).data;
            bitmap.close();
            return { width: canvas.width, height: canvas.height, pixels: new DataView(pixels.buffer, pixels.byteOffset, pixels.byteLength) };
        });
    })()"""
)

private fun decodedWidth(image: DecodedImage): Int = js("image.width")
private fun decodedHeight(image: DecodedImage): Int = js("image.height")
private fun decodedPixels(image: DecodedImage): DataView<ArrayBuffer> = js("image.pixels")

/**
 * The width, height and RGBA pixels of a decoded image, the bottom row first
 */
internal class DecodedPixels(val width: Int, val height: Int, val pixels: MPPBuffer)

internal suspend fun decodeImage(buffer: MPPBuffer): DecodedPixels {
    val image = decode(buffer.dataView, buffer.position(), buffer.limit() - buffer.position()).await()
    return DecodedPixels(decodedWidth(image), decodedHeight(image), MPPBuffer(decodedPixels(image)))
}

// -- encoding

/**
 * Encodes RGBA bytes, the top row first, as a data url with the browser
 * @param quality the quality of lossy formats from 0 to 1, negative for the default
 */
private fun encode(pixels: DataView<ArrayBuffer>, width: Int, height: Int, mimeType: String, quality: Double): String = js(
    """(() => {
        if (typeof document === 'undefined') {
            throw new Error('encoding images needs a canvas, which is only available in browsers');
        }
        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        const context = canvas.getContext('2d');
        const image = context.createImageData(width, height);
        image.data.set(new Uint8Array(pixels.buffer, pixels.byteOffset, width * height * 4));
        context.putImageData(image, 0, 0);
        return quality < 0 ? canvas.toDataURL(mimeType) : canvas.toDataURL(mimeType, quality);
    })()"""
)

internal fun encodeDataUrl(rgbaTopDown: MPPBuffer, width: Int, height: Int, mimeType: String, quality: Double): String =
    encode(rgbaTopDown.dataView, width, height, mimeType, quality)

/**
 * Saves a data url as [filename]: to the file system in Node.js, as a download in browsers
 */
private fun saveDataUrl(dataUrl: String, filename: String): Unit = js(
    """(() => {
        const process = globalThis.process;
        const fs = process && process.getBuiltinModule ? process.getBuiltinModule('fs') : undefined;
        if (fs && typeof document === 'undefined') {
            const binary = atob(dataUrl.substring(dataUrl.indexOf(',') + 1));
            const bytes = new Uint8Array(binary.length);
            for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
            fs.writeFileSync(filename, bytes);
            return;
        }
        const anchor = document.createElement('a');
        anchor.href = dataUrl;
        anchor.download = filename.substring(filename.lastIndexOf('/') + 1);
        document.body.appendChild(anchor);
        anchor.click();
        document.body.removeChild(anchor);
    })()"""
)

internal fun saveFile(dataUrl: String, filename: String) = saveDataUrl(dataUrl, filename)
