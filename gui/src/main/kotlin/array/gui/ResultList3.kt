package array.gui

import array.APLGenericException
import array.APLValue
import array.gui.styledarea.ParStyle
import array.gui.styledarea.TextStyle
import javafx.scene.Node
import javafx.scene.text.TextFlow
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.GenericStyledArea
import org.fxmisc.richtext.StyledTextArea
import org.fxmisc.richtext.TextExt
import org.fxmisc.richtext.model.*
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Function

class ResultList3(val renderContext: ClientRenderContext) {
    private val styledTextOps = SegmentOps.styledTextOps<TextStyle>()
    private val styledArea: GenericStyledArea<ParStyle, String, TextStyle>
    private val scrollArea: VirtualizedScrollPane<GenericStyledArea<ParStyle, String, TextStyle>>

    init {
        val applyParagraphStyle = BiConsumer<TextFlow, ParStyle> { t, u ->
            println("accept: t=${t}, u=${u}")
        }
        val nodeFactory = Function<StyledSegment<String, TextStyle>, Node> { seg ->
            val applyStyle = { a: TextExt, b: TextStyle ->
                a.font = renderContext.font()
            }
            StyledTextArea.createStyledTextNode(seg.segment, seg.style, applyStyle)
        }

        val segmentOps: TextOps<String, TextStyle> = styledTextOps

        styledArea = GenericStyledArea(
            ParStyle(),
            applyParagraphStyle,
            TextStyle(),
            segmentOps,
            nodeFactory
        )

        styledArea.isEditable = false
        styledArea.isWrapText = false

        scrollArea = VirtualizedScrollPane(styledArea)
    }

    fun getNode() = scrollArea

    fun addResult(text: String, v: APLValue) {
        addInput(text)
        styledArea.appendText(v.formatted() + "\n")
    }

    fun addResult(text: String, e: APLGenericException) {
        addInput(text)
        styledArea.appendText(e.formattedError() + "\n")
    }

    private fun addInput(text: String) {
        //val doc: StyledDocument<ParStyle, String, TextStyle> = HistoryEntryDocument(text)
        val doc = ReadOnlyStyledDocumentBuilder<ParStyle, String, TextStyle>(styledTextOps, ParStyle())
            .addParagraph(text + "\n", TextStyle())
            .build()
        styledArea.insert(styledArea.length, doc)
    }

    inner class HistoryEntryDocument(private val text: String) : StyledDocument<ParStyle, String, TextStyle> {
        override fun length() = text.length

        override fun concat(that: StyledDocument<ParStyle, String, TextStyle>?): StyledDocument<ParStyle, String, TextStyle> {
            TODO("not implemented")
        }

        override fun getText() = text

        override fun offsetToPosition(offset: Int, bias: TwoDimensional.Bias?): TwoDimensional.Position {
            TODO("not implemented")
        }

        override fun position(major: Int, minor: Int): TwoDimensional.Position {
            TODO("not implemented")
        }

        override fun getParagraphs(): MutableList<Paragraph<ParStyle, String, TextStyle>> {
            val paragraph = Paragraph<ParStyle, String, TextStyle>(ParStyle(), styledTextOps, text + "\n", HistoryEntryStyle())
            return arrayListOf(paragraph)
        }

        override fun subSequence(start: Int, end: Int): StyledDocument<ParStyle, String, TextStyle> {
            return HistoryEntryDocument(text.substring(start, end))
        }
    }

    class HistoryEntryStyle : TextStyle()
}