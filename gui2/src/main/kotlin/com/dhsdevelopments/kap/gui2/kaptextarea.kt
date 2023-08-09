package com.dhsdevelopments.kap.gui2

import org.fife.ui.rtextarea.RTextArea

class KapTextArea(text: String? = null, rows: Int = 0, cols: Int = 0) : RTextArea(text, rows, cols) {
    init {
        enableKapKeyboard()
    }
}
