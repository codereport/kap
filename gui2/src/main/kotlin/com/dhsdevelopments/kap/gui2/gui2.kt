package com.dhsdevelopments.kap.gui2

import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JLabel

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val client = Gui2Client()
            client.openRepFramel()
        }
    }
}

class Gui2Client {
    private val computeQueue = ComputeQueue()

    fun openRepFramel() {
        val frame = JFrame("Test frame")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        val scrollPane = RTextScrollPane(ReplPanel(computeQueue)).apply {
            preferredSize = Dimension(400, 400)
            lineNumbersEnabled = false
        }
        frame.contentPane.layout = BorderLayout()
        frame.contentPane.add(scrollPane, BorderLayout.CENTER)
        frame.contentPane.add(JLabel("Foo"), BorderLayout.NORTH)
        frame.pack()
        frame.isVisible = true
    }
}
