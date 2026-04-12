package org.openrndr.draw

import org.openrndr.internal.Driver
import java.io.File
import java.io.RandomAccessFile
import java.net.MalformedURLException
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class BufferTextureFileFormat(val extension: String) {
    ORB("orb")
}

actual abstract class BufferTexture: Texture, AutoCloseable {
    companion object {
        fun create(elementCount: Int, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.FLOAT32, session: Session? = Session.active): BufferTexture {
            val bufferTexture = Driver.instance.createBufferTexture(elementCount, format, type)
            session?.track(bufferTexture)
            return bufferTexture
        }
    }

    fun put(putter: BufferWriter.() -> Unit): Int {
        val w = shadow.writer()
        w.rewind()
        w.putter()
        val count = w.positionElements

        shadow.upload(0, w.position)
        w.rewind()
        return count
    }

    abstract fun read(target: ByteBuffer, offset: Int = 0, elementReadCount: Int = this.elementCount)
    abstract fun write(source: ByteBuffer, offset: Int = 0, elementWriteCount: Int = this.elementCount)

    fun saveToFile(file: File, @Suppress("UNUSED_PARAMETER") fileFormat: BufferTextureFileFormat = BufferTextureFileFormat.ORB) {
        val buffer = ByteBuffer.allocateDirect(elementCount * format.componentCount * type.componentSize)
        val header = ByteBuffer.allocateDirect(12)
        header.order(ByteOrder.nativeOrder())
        header.putInt(buffer.capacity())
        header.putInt(format.ordinal)
        header.putInt(type.ordinal)

        buffer.order(ByteOrder.nativeOrder())
        read(buffer)
        buffer.rewind()
        header.rewind()
        val raf = RandomAccessFile(file, "rw")
        val chan = raf.channel
        chan.write(header)
        chan.write(buffer)
        chan.close()
        raf.close()
    }

    actual abstract val session: Session?
    actual abstract val shadow: BufferTextureShadow
    actual abstract val format: ColorFormat
    actual abstract val type: ColorType
    actual abstract val elementCount: Int
    actual abstract fun destroy()

    /**
     * bind the BufferTexture to a texture unit
     */
    actual abstract fun bind(unit: Int)
}

/**
 * load a buffer texture from file or url
 * @param fileOrUrl filename or url
 * @param formatHint a format hint for the loader
 * @param session the session to which the buffer texture will be registered
 */
fun loadBufferTexture(fileOrUrl: String, formatHint: BufferTextureFileFormat? = BufferTextureFileFormat.ORB, session: Session? = Session.active) : BufferTexture {
    return try {
        if (!fileOrUrl.startsWith("data:")) {
            val url = URL(fileOrUrl)
            return loadBufferTexture(url, formatHint, session = session)
        } else {
            error("data scheme not supported")
        }
    } catch (e: MalformedURLException) {
        loadBufferTexture(File(fileOrUrl), session = session)
    }
}

/**
 * load a buffer texture from a file
 * @param file the file to load the buffer texture from
 * @param formatHint a format hint for the loader
 * @param session the session to which the buffer texture will be registered
 */
fun loadBufferTexture(file: File,
                      @Suppress("UNUSED_PARAMETER") formatHint: BufferTextureFileFormat? = BufferTextureFileFormat.ORB,
                      session: Session? = Session.active) : BufferTexture {
    require(file.exists()) {
        "file ${file.absolutePath} does not exist"
    }
    val raf = RandomAccessFile(file, "r")
    val header = ByteBuffer.allocateDirect(12)
    header.order(ByteOrder.nativeOrder())
    val chan = raf.channel
    chan.read(header)
    header.rewind()
    val size = header.int
    val format = ColorFormat.values()[header.int]
    val type = ColorType.values()[header.int]
    val buffer = ByteBuffer.allocateDirect(size)
    buffer.order(ByteOrder.nativeOrder())
    chan.read(buffer)
    chan.close()

    val elementCount = size / (format.componentCount * type.componentSize)
    val bufferTexture = bufferTexture(elementCount, format, type, session)
    buffer.rewind()
    bufferTexture.write(buffer)
    return bufferTexture
}

/**
 * load a BufferTexture from a url
 *  @param url the url to load from
 *  @param formatHint a format hint for the loader
 *  @param session the session the buffer texture will be created in
 */
fun loadBufferTexture(url: URL, @Suppress("UNUSED_PARAMETER") formatHint: BufferTextureFileFormat? = BufferTextureFileFormat.ORB, session: Session? = Session.active) : BufferTexture {
    val stream = url.openStream()
    val headerData = ByteArray(12)
    stream.read(headerData)
    val header = ByteBuffer.wrap(headerData)
    header.order(ByteOrder.nativeOrder())
    header.rewind()
    val size = header.int
    val format = ColorFormat.values()[header.int]
    val type = ColorType.values()[header.int]
    val buffer = ByteBuffer.allocateDirect(size)
    buffer.order(ByteOrder.nativeOrder())

    val bodyData = ByteArray(size)
    val body = ByteBuffer.wrap(bodyData)
    stream.read(bodyData)
    body.rewind()
    buffer.rewind()
    buffer.put(body)
    buffer.rewind()

    val elementCount = size / (format.componentCount * type.componentSize)
    val bufferTexture = bufferTexture(elementCount, format, type, session)
    bufferTexture.write(buffer)
    return bufferTexture
}

/**
 * create a [BufferTexture]
 * @param elementCount the number of elements in the buffer texture
 * @param format the format of the elements
 * @param type the type of the elements
 * @param session the session that will track the [BufferTexture] resource
 */
fun bufferTexture(elementCount: Int, format: ColorFormat = ColorFormat.RGBa, type: ColorType = ColorType.FLOAT32, session: Session? = Session.active): BufferTexture {
    return BufferTexture.create(elementCount, format, type, session)
}