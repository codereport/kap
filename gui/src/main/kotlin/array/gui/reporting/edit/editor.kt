package array.gui.reporting.edit

import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.model.Codec

class ResultEditor private constructor() {
    val root: VBox

    init {
        val vbox = VBox()
        root = vbox

        val editorArea = FoldableStyledArea().apply {
            isWrapText = true
            setStyleCodecs(
                ParStyle.CODEC,
                Codec.styledSegmentCodec(Codec.eitherCodec(Codec.STRING_CODEC, LinkedImage.codec()), TextStyle.CODEC))
        }
        val scrollPane = VirtualizedScrollPane(editorArea)
        vbox.children.add(scrollPane)
        VBox.setVgrow(scrollPane, Priority.ALWAYS)
    }

    companion object {
        fun make(): ResultEditor {
            return ResultEditor()
        }
    }
}
