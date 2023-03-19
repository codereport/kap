package array.gui.reporting.edit

import array.gui.Client
import array.gui.reporting.Formula
import array.gui.reporting.ReportingClient
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.model.ReadOnlyStyledDocumentBuilder

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

    fun addInlineValue(client: ReportingClient, formula: Formula) {
        val builder = ReadOnlyStyledDocumentBuilder(SegOps(), ParStyle.EMPTY)
        builder.addParagraph(DynamicValueSegment(InlineValue(client, formula)), TextStyle.EMPTY)
        val doc = builder.build()
        editorArea.insert(0, doc)
    }

    companion object {
        fun make(): ResultEditor {
            return ResultEditor()
        }
    }
}
