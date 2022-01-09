package array.gui

import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox

class SideButtonPanel(inner: Parent) : BorderPane() {
    init {
        val vbox = VBox()
        vbox.alignment = Pos.CENTER
        val openButton = Label(">")
        VBox.setMargin(openButton, Insets(2.0, 2.0, 4.0, 2.0))
        openButton.onMouseClicked = EventHandler { onOpen?.let { fn -> fn() } }
        vbox.children.add(openButton)
        val closeButton = Label("<")
        VBox.setMargin(closeButton, Insets(4.0, 2.0, 2.0, 2.0))
        closeButton.onMouseClicked = EventHandler { onClose?.let { fn -> fn() } }
        vbox.children.add(closeButton)
        setAlignment(vbox, Pos.CENTER)
        left = vbox
        center = inner
    }

    var onOpen: (() -> Unit)? = null
    var onClose: (() -> Unit)? = null
}
