package org.openrndr.internal.gl3.extensions

import org.lwjgl.system.FunctionProvider
import org.lwjgl.system.JNI
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.libffi.FFICIF
import org.lwjgl.system.libffi.LibFFI


class AngleExtensions(functionProvider: FunctionProvider) {
    val glTexStorage2DMultisampleANGLEAddress = functionProvider.getFunctionAddress("glTexStorage2DMultisampleANGLE")
    val glGetTexLevelParameterivANGLEAddress = functionProvider.getFunctionAddress("glGetTexLevelParameterivANGLE")
    val glGetTexLevelParameterfvANGLEAddress = functionProvider.getFunctionAddress("glGetTexLevelParameterfvANGLE")

    fun glTexLevelParameterivANGLE(target: Int, level: Int, pname: Int, params: IntArray) {
        require(glGetTexLevelParameterivANGLEAddress != 0L)
        JNI.callPV(target, level, pname, params, glGetTexLevelParameterivANGLEAddress)
    }

    fun glTexStorage2DMultisampleANGLE(
        target: Int,
        samples: Int,
        internalFormat: Int,
        width: Int,
        height: Int,
        fixedSampleLocations: Boolean
    ) {
        require(glTexStorage2DMultisampleANGLEAddress != 0L)

        MemoryStack.stackPush().use { stack ->
            val ptarget = stack.mallocInt(1)
            val psamples = stack.mallocInt(1)
            val pinternalFormat = stack.mallocInt(1)
            val pwidth = stack.mallocInt(1)
            val pheight = stack.mallocInt(1)
            val pfixedSampleLocations = stack.malloc(1)

            ptarget.put(target).flip()
            psamples.put(samples).flip()
            pinternalFormat.put(internalFormat).flip()
            pwidth.put(width).flip()
            pheight.put(height).flip()
            pfixedSampleLocations.put(if (fixedSampleLocations) 1 else 0).flip()

            val arguments = stack.mallocPointer(6)
            arguments.put(MemoryUtil.memAddress(ptarget))
            arguments.put(MemoryUtil.memAddress(psamples))
            arguments.put(MemoryUtil.memAddress(pinternalFormat))
            arguments.put(MemoryUtil.memAddress(pwidth))
            arguments.put(MemoryUtil.memAddress(pheight))
            arguments.put(MemoryUtil.memAddress(pfixedSampleLocations))
            arguments.flip()

            val argumentTypes = stack.mallocPointer(6)
            argumentTypes.put(LibFFI.ffi_type_sint)
            argumentTypes.put(LibFFI.ffi_type_sint)
            argumentTypes.put(LibFFI.ffi_type_sint)
            argumentTypes.put(LibFFI.ffi_type_sint)
            argumentTypes.put(LibFFI.ffi_type_sint)
            argumentTypes.put(LibFFI.ffi_type_uint8)
            argumentTypes.flip()

            val cif = FFICIF.malloc()
            LibFFI.ffi_prep_cif(cif, LibFFI.FFI_DEFAULT_ABI, LibFFI.ffi_type_void, argumentTypes)
            LibFFI.ffi_call(cif, glTexStorage2DMultisampleANGLEAddress, null, arguments)
            cif.free()
        }
    }

}