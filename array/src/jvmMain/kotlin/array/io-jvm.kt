package array

import java.io.BufferedReader
import java.io.InputStreamReader

actual class StringCharacterProvider actual constructor(private val s: String) : CharacterProvider {

    private var pos = 0

    override fun nextCodepoint(): Int? {
        return if (pos >= s.length) {
            null
        } else {
            val result = s.codePointAt(pos)
            pos = s.offsetByCodePoints(pos, 1)
            result
        }
    }

    override fun revertLastChars(n: Int) {
        pos = s.offsetByCodePoints(pos, -n)
    }
}

class KeyboardInputJvm() : KeyboardInput {
    private val reader = BufferedReader(InputStreamReader(System.`in`))

    override fun readString(): String? {
        return reader.readLine()
    }
}

actual fun makeKeyboardInput(): KeyboardInput {
    return KeyboardInputJvm()
}
