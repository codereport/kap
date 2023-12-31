package array.gui

import array.APLGenericException
import array.APLValue
import array.SourceLocation
import array.StringCharacterProvider
import array.gui.arrayedit.ArrayEditor
import array.gui.styledarea.*
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.TextFlow
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.model.GenericEditableStyledDocument
import org.fxmisc.richtext.model.SegmentOpsBase
import org.fxmisc.richtext.model.StyledSegment
import org.fxmisc.richtext.model.TextOps
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Function

class REPLSourceLocation(
    private val text: String,
    val parent: ResultList3,
    val tag: InputLogTag
) : SourceLocation {
    override fun sourceText() = text
    override fun open() = StringCharacterProvider(text)
}

class InputLogTag

class ResultList3(val client: Client) {
    private val styledOps = CodeSegmentOps()
    private val styledArea: ROStyledArea
    private val scrollArea: VirtualizedScrollPane<ROStyledArea>

    private val history = ArrayList<String>()
    private var historyPos = 0
    private var pendingInput: String? = null

    private val resultRefHistory = ArrayList<TextStyle.MutableSupplementaryValue>()

    init {
        val applyParagraphStyle = BiConsumer<TextFlow, ParStyle> { text, parStyle ->
            if (parStyle.type == ParStyle.ParStyleType.INDENT) {
                text.border =
                    Border(BorderStroke(Color.TRANSPARENT, BorderStrokeStyle.NONE, CornerRadii.EMPTY, BorderWidths(5.0, 5.0, 5.0, 30.0)))
            }
        }
        val nodeFactory = Function<StyledSegment<EditorContent, TextStyle>, Node> { segment ->
            segment.segment.createNode(client.renderContext, segment.style)
        }

        GenericEditableStyledDocument(ParStyle(), TextStyle(), styledOps)
        styledArea = ROStyledArea(client, applyParagraphStyle, styledOps, nodeFactory)
        styledArea.setCommandListener(ResultListCommandListener())
        initStyledAreaContextMenu()

        val historyListener = ResultHistoryListener()
        styledArea.addHistoryListener(historyListener)

        styledArea.isWrapText = false

        scrollArea = VirtualizedScrollPane(styledArea)
    }

    private fun initStyledAreaContextMenu() {
        val m = ContextMenu(MenuItem("Selection").apply { isDisable = true })
        styledArea.contextMenu = m
        styledArea.onContextMenuRequested = EventHandler { event ->
            repeat(m.items.size - 1) {
                m.items.removeLast()
            }
            styledArea.hit(event.x, event.y).characterIndex.ifPresent { index ->
                val style = styledArea.document.getStyleAtPosition(index)
                val supp = style.supplementaryValue
                if (supp != null) {
                    val v = supp.value
                    if (v != null) {
                        m.items.add(MenuItem("Copy as string").apply { onAction = EventHandler { copyValueAsString(v) } })
                        m.items.add(MenuItem("Copy as code").apply { onAction = EventHandler { copyValueAsCode(v) } })
                        m.items.add(MenuItem("Copy as HTML").apply { onAction = EventHandler { copyValueAsHtml(v) } })
                        if (v.dimensions.size >= 1) {
                            m.items.add(SeparatorMenuItem())
                            m.items.add(MenuItem("Open in array editor").apply { onAction = EventHandler { openInArrayEditor(v) } })
                        }
                    }
                }
            }
        }
    }


    private fun openInArrayEditor(value: APLValue) {
        Platform.runLater {
            ArrayEditor.open(client, value)
        }
    }

    fun requestFocus() {
        styledArea.requestFocus()
    }

    fun getNode() = scrollArea

    fun addResult(v: EvalExpressionResult) {
        val supp = TextStyle.MutableSupplementaryValue(v.value)
        @Suppress("KotlinConstantConditions")
        if (MAX_REF_HISTORY_SIZE > 0) {
            resultRefHistory.add(supp)
        }
        while (resultRefHistory.size > MAX_REF_HISTORY_SIZE) {
            val oldEntry = resultRefHistory.removeFirst()
            oldEntry.value = null
        }
        styledArea.appendExpressionResultEnd(v, TextStyle(TextStyle.Type.RESULT, supplementaryValue = supp))
    }

    // Need to suppress error warning here because of https://youtrack.jetbrains.com/issue/KTIJ-20744
    @Suppress("USELESS_IS_CHECK")
    fun addExceptionResult(e: Exception) {
        val message = if (e is APLGenericException) {
            e.formattedError()
        } else {
            "Exception from KAP engine: ${e.message}"
        }
        styledArea.appendErrorMessage(message)
    }

    fun addOutput(text: String) {
        styledArea.appendOutputEnd(text)
    }

    private fun addInput(text: String): InputLogTag {
        val tag = InputLogTag()
        styledArea.appendTextEnd(text + "\n", TextStyle(TextStyle.Type.LOG_INPUT), ParStyle(ParStyle.ParStyleType.INDENT, tag = tag))
        return tag
    }

    fun updateStyle(tag: InputLogTag, startLine: Int, startCol: Int, endLine: Int, endCol: Int, textStyle: TextStyle) {
        styledArea.withUpdateEnabled {
            val startIndex = indexOfTag(tag)
            if (startIndex != null) {
                styledArea.setStyleForRange(startIndex + startLine, startCol, startIndex + endLine, endCol, textStyle)
            }
        }
    }

    private fun indexOfTag(tag: Any): Int? {
        var foundIndex: Int? = null
        for (i in styledArea.paragraphs.size - 1 downTo 0) {
            val paragraph = styledArea.paragraphs[i]
            if (paragraph.paragraphStyle.tag === tag) {
                foundIndex = i
            } else {
                if (foundIndex != null) {
                    break
                }
            }
        }
        return foundIndex
    }

    inner class ResultHistoryListener : HistoryListener {
        override fun prevHistory() {
            if (historyPos > 0) {
                if (historyPos == history.size) {
                    pendingInput = styledArea.currentInput()
                }
                historyPos--
                styledArea.replaceInputText(history[historyPos])
                styledArea.showBottomParagraphAtTop()
            }
        }

        override fun nextHistory() {
            when {
                historyPos < history.size - 1 -> {
                    historyPos++
                    styledArea.replaceInputText(history[historyPos])
                }
                historyPos == history.size - 1 -> {
                    historyPos++
                    styledArea.replaceInputText(pendingInput ?: "")
                    pendingInput = null
                    styledArea.showBottomParagraphAtTop()
                }
            }
        }
    }

    companion object {
        private const val MAX_REF_HISTORY_SIZE = 10
    }

    class CodeSegmentOps : SegmentOpsBase<EditorContent, TextStyle>(EditorContent.makeBlank()), TextOps<EditorContent, TextStyle> {
        override fun length(seg: EditorContent): Int {
            return seg.length()
        }

        override fun joinSeg(currentSeg: EditorContent, nextSeg: EditorContent): Optional<EditorContent> {
            return currentSeg.joinSegment(nextSeg)
        }

        override fun realGetText(seg: EditorContent): String? {
            return seg.realGetText()
        }

        override fun realCharAt(seg: EditorContent, index: Int): Char {
            return seg.realCharAt(index)
        }

        override fun realSubSequence(seg: EditorContent, start: Int, end: Int): EditorContent {
            return seg.realSubsequence(start, end)
        }

        override fun create(text: String): EditorContent {
            return EditorContent.makeString(text)
        }
    }

    inner class ResultListCommandListener : CommandListener {
        override fun valid(text: String): Boolean {
            return !client.calculationQueue.isActive()
        }

        override fun handle(text: String) {
            if (text.trim().isNotBlank()) {
                if (history.isEmpty() || history.last() != text) {
                    history.add(text)
                }
                historyPos = history.size
                pendingInput = null
                val tag = addInput(text)
                val source = REPLSourceLocation(text, this@ResultList3, tag)
                client.evalSource(source)
            }
        }
    }
}
