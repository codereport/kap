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
import platform.posix.*

private const val PROMPT_BUF_SIZE = 1024

private var currentEditline: CPointer<EditLine>? = null

@OptIn(ExperimentalForeignApi::class)
private class EditLineState {
    val currentPromptPtr: CPointer<ByteVar> = nativeHeap.allocArray<ByteVar>(PROMPT_BUF_SIZE)
    val extendedCharsKeyboardInput = ExtendedCharsKeyboardInput()
}

@OptIn(ExperimentalForeignApi::class)
class LibinputKeyboardInput : KeyboardInput {
    private val editLine: CPointer<EditLine>
    private val historyInst: CPointer<HistoryW>
    private val state = EditLineState()

    init {
        editLine = el_init("text-client", stdin, stdout, stderr) ?: throw RuntimeException("Error initialising libedit")
        historyInst = history_winit() ?: throw RuntimeException("Error initialising history")
        val stableRef = StableRef.create(state)
        el_set(editLine, EL_CLIENTDATA, stableRef.asCPointer())
        el_set(editLine, EL_EDITOR, "emacs")
        el_set(editLine, EL_PROMPT, staticCFunction(::findPrompt))
        el_set(editLine, EL_ADDFN, "charprefix", "Prefix for entering kap characters", staticCFunction(::charPrefixUserCommand))
        el_set(editLine, EL_BIND, "`", "charprefix", null)
        el_set(editLine, EL_HIST, findHistoryWFn(), historyInst)

        memScoped {
            val event = alloc<HistEventW>()
            history_w(historyInst, event.ptr, H_SETSIZE, 100)
            history_w(historyInst, event.ptr, H_SETUNIQUE, 1)
        }

        signal(SIGWINCH, staticCFunction(::windowUpdate))
    }

    override fun readString(prompt: String): String? {
        memScoped {
            copyTruncated(state.currentPromptPtr, prompt.encodeToByteArray())
            val length = alloc<IntVar>()
            if (currentEditline !== null) {
                throw IllegalStateException("currentEditline is not null")
            }
            currentEditline = editLine
            val res = el_wgets(editLine, length.ptr)
            currentEditline = null
            if (res == null) {
                return null
            } else {
                val resString = res.toKStringFromUtf32()
                if (resString.isNotBlank()) {
                    val event = alloc<HistEventW>()
                    history_w(historyInst, event.ptr, H_ENTER, resString.cstr)
                }
                return resString
            }
        }
    }
}

private fun windowUpdate(@Suppress("UNUSED_PARAMETER") n: Int) {
    val l = currentEditline
    if (l !== null) {
        el_resize(l)
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

@OptIn(ExperimentalForeignApi::class)
private fun clientStateFromUserData(editLinePointer: CPointer<EditLine>): EditLineState {
    memScoped {
        val data = alloc<COpaquePointerVar>()
        el_get(editLinePointer, EL_CLIENTDATA, data)
        val value = data.value ?: throw RuntimeException("data should not be null")
        return value.asStableRef<EditLineState>().get()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun copyTruncated(dest: CPointer<ByteVar>, values: ByteArray) {
    var i = 0
    while (i < values.size && i < PROMPT_BUF_SIZE - 1) {
        dest[i] = values[i]
        i++
    }
    dest[i] = 0
}
