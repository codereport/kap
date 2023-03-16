package array.gui.reporting.edit

import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import org.fxmisc.flowless.VirtualizedScrollPane

class ResultEditor private constructor() {
    val root: VBox
    val editorArea: FoldableStyledArea

    init {
        val vbox = VBox()
        root = vbox

        editorArea = FoldableStyledArea().apply {
            isWrapText = true
//            setStyleCodecs(
//                ParStyle.CODEC,
//                Codec.styledSegmentCodec(Codec.eitherCodec(Codec.STRING_CODEC, LinkedImage.codec()), TextStyle.CODEC))
        }
        val scrollPane = VirtualizedScrollPane(editorArea)
        vbox.children.add(scrollPane)
        VBox.setVgrow(scrollPane, Priority.ALWAYS)
    }

    fun addInlineValue() {
        //editorArea.insert
    }

    companion object {
        fun make(): ResultEditor {
            return ResultEditor()
        }
    }
}
