package com.dhsdevelopments.kap.gui2

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JScrollPane
import javax.swing.SwingUtilities

class Main {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SwingUtilities.invokeLater {
                val client = Gui2Client()
                client.openReplFrame()
            }
        }
    }
}

class Gui2Client {
    private val computeQueue = ComputeQueue()
    private val font: Font

    init {
        font = Font.createFont(Font.TRUETYPE_FONT, this::class.java.getResourceAsStream("fonts/iosevka-fixed-regular.ttf")).deriveFont(18.0f)
    }

    fun openReplFrame() {
        val frame = JFrame("Test frame")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        val scrollPane = JScrollPane(ReplPanel(computeQueue, font)).apply {
            preferredSize = Dimension(800, 800)
        }
        frame.contentPane.layout = BorderLayout()
        frame.contentPane.add(scrollPane, BorderLayout.CENTER)
        frame.contentPane.add(JLabel("Foo"), BorderLayout.NORTH)
        frame.pack()
        frame.isVisible = true
    }
}
