@file:OptIn(ExperimentalForeignApi::class)

package com.dhsdevelopments.kap.textclient

import array.KeyboardInput
import array.asCodepointList
import array.charToString
import array.keyboard.ExtendedCharsKeyboardInput
import jansson.stderr
import jansson.stdin
import jansson.stdout
import kotlinx.cinterop.*
import libedit.*
import platform.posix.errno
import platform.posix.strerror
import platform.posix.wchar_tVar

private const val PROMPT_BUF_SIZE = 1024

private class EditLineState {
    val currentPromptPtr: CPointer<ByteVar> = nativeHeap.allocArray<ByteVar>(PROMPT_BUF_SIZE)
    val extendedCharsKeyboardInput = ExtendedCharsKeyboardInput()
}

@OptIn(ExperimentalForeignApi::class)
class LibinputKeyboardInput : KeyboardInput {
    private val editLine = el_init("text-client", stdin, stdout, stderr) ?: throw RuntimeException("Error initialisaing libedit")
    private val state = EditLineState()

    init {
        val stableRef = StableRef.create(state)
        el_set(editLine, EL_CLIENTDATA, stableRef.asCPointer())
        el_set(editLine, EL_EDITOR, "emacs")
        el_set(editLine, EL_PROMPT, staticCFunction(::findPrompt))
        el_set(editLine, EL_ADDFN, "charprefix", "Prefix for entering kap characters", staticCFunction(::charPrefixUserCommand))
        el_set(editLine, EL_BIND, "`", "charprefix", null)
    }

    override fun readString(prompt: String): String? {
        memScoped {
            copyTruncated(state.currentPromptPtr, prompt.encodeToByteArray())
            val length = alloc<IntVar>()
            val res = el_wgets(editLine, length.ptr)
            if (res == null) {
                return null
            } else {
                return res.toKStringFromUtf32()
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun addStringToBuffer(editLinePointer: CPointer<EditLine>, str: String) {
    memScoped {
        val list = str.asCodepointList()
        val array = allocArray<wchar_tVar>(list.size + 1)
        list.forEachIndexed { i, ch ->
            array[i] = ch
        }
        array[list.size] = 0
        el_winsertstr(editLinePointer, array)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun charPrefixUserCommand(editLinePointer: CPointer<EditLine>, ch: Int): Int {
    val state = clientStateFromUserData(editLinePointer)
    memScoped {
        val chRet = alloc<wchar_tVar>()
        val ret = el_wgetc(editLinePointer, chRet.ptr)
        return when (ret) {
            0 -> {
                CC_EOF
            }
            1 -> {
                if (chRet.value == ' '.code) {
                    addStringToBuffer(editLinePointer, charToString(ch))
                } else {
                    val str = charToString(chRet.value)
                    val entry = state.extendedCharsKeyboardInput.keymap.entries.find { e ->
                        e.key.character == str
                    }
                    if (entry != null) {
                        addStringToBuffer(editLinePointer, entry.value)
                    }
                }
                CC_REFRESH
            }
            -1 -> {
                println("Error reading character: ${strerror(errno)}")
                CC_FATAL
            }
            else -> {
                println("Unexpected return value from el_wgetc: ${ret}")
                CC_FATAL
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun findPrompt(editLinePointer: CPointer<EditLine>): CPointer<ByteVar> {
    val state = clientStateFromUserData(editLinePointer)
    return state.currentPromptPtr
}

private fun clientStateFromUserData(editLinePointer: CPointer<EditLine>): EditLineState {
    memScoped {
        val data = alloc<COpaquePointerVar>()
        el_get(editLinePointer, EL_CLIENTDATA, data)
        val value = data.value ?: throw RuntimeException("data should not be null")
        return value.asStableRef<EditLineState>().get()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun copyTruncated(dest: CPointer<ByteVarOf<Byte>>, values: ByteArray) {
    var i = 0
    while (i < values.size && i < PROMPT_BUF_SIZE - 1) {
        dest[i] = values[i]
        i++
    }
    dest[i] = 0
}
