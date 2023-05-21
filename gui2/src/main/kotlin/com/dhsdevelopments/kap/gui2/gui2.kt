package com.dhsdevelopments.kap.gui2

import javax.swing.JFrame

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val frame = JFrame("Test frame")
            frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            frame.contentPane.add(ReplPanel())
            frame.pack()
            frame.isVisible = true
        }
    }
}
