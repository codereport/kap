@file:OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)

package array

import icu.UCharVar
import kotlinx.cinterop.*
import kotlin.experimental.ExperimentalNativeApi

actual fun isLetter(codepoint: Int): Boolean {
    return icu.u_isUAlphabetic!!(codepoint).toBoolean()
}

actual fun isDigit(codepoint: Int): Boolean {
    return icu.u_isdigit!!(codepoint).toBoolean()
}

actual fun isWhitespace(codepoint: Int): Boolean {
    return icu.u_isUWhiteSpace!!(codepoint).toBoolean()
}

actual fun charToString(codepoint: Int): String {
    return Char.toChars(codepoint).concatToString()
}

actual fun nameToCodepoint(name: String): Int? {
    memScoped {
        val errorCode = alloc<icu.UErrorCodeVar>()
        val nameBuf = name.cstr.getPointer(this)
        val codepoint = icu.u_charFromName!!(icu.U_UNICODE_CHAR_NAME, nameBuf, errorCode.ptr)
        return if (icu.icu_u_success(errorCode.value) != 0) {
            codepoint
        } else {
            null
        }
    }
}

actual fun codepointToName(codepoint: Int): String? {
    memScoped {
        val errorCode = alloc<icu.UErrorCodeVar>()
        val length = 200
        val nameBuf = allocArray<ByteVar>(length)
        val ret = icu.u_charName!!(codepoint, icu.U_UNICODE_CHAR_NAME, nameBuf, length, errorCode.ptr)
        if (icu.icu_u_success(errorCode.value) != 0) {
            return if (ret == 0) {
                null
            } else {
                nameBuf.toKStringFromUtf8()
            }
        } else {
            throw Exception("Error finding name for char: ${codepoint}")
        }
    }
}

actual val backendSupportsUnicodeNames = true

actual fun StringBuilder.addCodepoint(codepoint: Int): StringBuilder {
    val v = Char.toChars(codepoint)
    v.forEach {
        this.append(it)
    }
    return this
}

actual fun String.asCodepointList(): List<Int> {
    val result = ArrayList<Int>()
    var i = 0
    while (i < this.length) {
        val ch = this[i++]
        val v = when {
            ch.isHighSurrogate() -> {
                val low = this[i++]
                if (low.isLowSurrogate()) {
                    Char.toCodePoint(ch, low)
                } else {
                    throw IllegalStateException("Expected low surrogate, got: ${low.code}")
                }
            }
            ch.isLowSurrogate() -> throw IllegalStateException("Standalone low surrogate found: ${ch.code}")
            else -> ch.code
        }
        result.add(v)
    }
    return result
}

actual fun String.asGraphemeList(): List<String> {
    val text = this
    memScoped {
        val length = text.length + 1
        val nativeBuf = allocArray<UCharVar>(length)
        for (i in text.indices) {
            nativeBuf[i] = text[i].code.toUShort()
        }
        nativeBuf[text.length] = 0.toUShort()

        val retCode = allocArray<IntVar>(1)
        val brkIterator = icu.ubrk_open!!(icu.UBRK_CHARACTER, null, nativeBuf, -1, retCode)
        var pos = icu.ubrk_first!!(brkIterator)
        val buf = ArrayList<String>()
        while (true) {
            val newPos = icu.ubrk_next!!(brkIterator)
            if (newPos == icu.UBRK_DONE) {
                break
            }
            val b = StringBuilder()
            for (i in pos until newPos) {
                b.append(nativeBuf[i].toInt().toChar())
            }
            buf.add(b.toString())
            pos = newPos
        }
        icu.ubrk_close!!(brkIterator)
        return buf
    }
}
