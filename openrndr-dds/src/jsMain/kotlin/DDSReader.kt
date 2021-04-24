package org.openrndr.dds
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.DataView
import org.openrndr.utils.buffer.MPPBuffer
import org.w3c.xhr.ARRAYBUFFER
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType
import kotlin.js.Promise

suspend fun loadDDS(url: String, bgrIsRgb:Boolean = true) : DDSData {
    val promise = Promise<ArrayBuffer> { resolve, reject ->
        val request = XMLHttpRequest()
        request.open("GET", url)
        request.responseType = XMLHttpRequestResponseType.ARRAYBUFFER
        request.onload = {
            if (request.readyState == 4.toShort() && request.status == 200.toShort()) {
                val arrayBuffer = request.response as ArrayBuffer
                resolve(arrayBuffer)
            } else {
                reject
            }
        }
        request.send(null)
    }
    val data = promise.await()
    val dataview = DataView(data)
    return loadDDS(MPPBuffer(dataview), bgrIsRgb)
}