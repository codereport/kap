package com.dhsdevelopments.kap.gui2

import array.keyboard.ExtendedCharsKeyboardInput
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.text.JTextComponent

fun enableKapKeyboard(panel: JTextComponent) {
    panel.addKeyListener(KapKeyboardHandler(panel))
}

class KapKeyboardHandler(val panel: JTextComponent) : KeyAdapter() {
    private val inputPrefix = '`'
    private var prefixEnabled = false
    private val keyboardInput = ExtendedCharsKeyboardInput()

    override fun keyTyped(e: KeyEvent) {
        if (prefixEnabled) {
            if (e.keyChar == ' ') {
                appendToInput(inputPrefix.toString())
            }
            val charAsString = e.keyChar.toString()
            val res = keyboardInput.keymap.toList().find { (k, _) -> k.character == charAsString }
            if (res != null) {
                e.consume()
                appendToInput(res.second)
            }
            prefixEnabled = false
        } else {
            if (e.keyChar == inputPrefix) {
                prefixEnabled = true
                e.consume()
            }
        }
    }

    private fun appendToInput(s: String) {
        panel.document.insertString(panel.caretPosition, s, null)
    }
}