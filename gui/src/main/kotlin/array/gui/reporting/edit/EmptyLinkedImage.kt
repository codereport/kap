package array.gui.reporting.edit

import javafx.scene.Node


class EmptyLinkedImage : LinkedImage {
    override val isReal get() = false
    override val imagePath get() = ""

    override fun createNode(): Node? {
        throw AssertionError("Unreachable code")
    }
}
