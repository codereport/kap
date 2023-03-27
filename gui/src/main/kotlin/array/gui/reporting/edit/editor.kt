package array.gui.reporting.edit

import array.gui.Client
import array.gui.reporting.Formula
import array.gui.reporting.ReportingClient
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
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
            contextMenu = ContextMenu(MenuItem("Insert dynamic value").apply { onAction = EventHandler { insertDynamicValue() } })
        }
        val scrollPane = VirtualizedScrollPane(editorArea)
        vbox.children.add(scrollPane)
        VBox.setVgrow(scrollPane, Priority.ALWAYS)
    }

    private fun insertDynamicValue() {
        println("insert")
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
