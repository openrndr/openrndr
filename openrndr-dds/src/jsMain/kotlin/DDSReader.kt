package org.openrndr.dds

import js.buffer.ArrayBuffer
import js.buffer.DataView
import kotlinx.coroutines.await
import org.openrndr.utils.buffer.MPPBuffer
import web.events.EventHandler
import web.http.GET
import web.http.RequestMethod
import web.xhr.XMLHttpRequest
import web.xhr.XMLHttpRequestResponseType
import web.xhr.arraybuffer
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
suspend fun loadDDS(url: String, bgrIsRgb:Boolean = true) : DDSData {
    val promise = Promise { resolve, reject ->
        val request = XMLHttpRequest()
        request.open(RequestMethod.GET, url)
        request.responseType = XMLHttpRequestResponseType.arraybuffer
        request.onload = EventHandler {
             if (request.readyState == XMLHttpRequest.DONE && request.status == 200.toShort()) {
                val arrayBuffer = request.response as ArrayBuffer
                resolve(arrayBuffer)
            } else {
                reject
            }
        }
        request.send("")
    }
    val data = promise.await()
    val dataView = DataView(data)
    return loadDDS(MPPBuffer(dataView), bgrIsRgb)
}