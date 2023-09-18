package array.gui.styledarea

import array.assertx
import array.gui.Client
import array.gui.EvalExpressionResult
import array.gui.display.makeKapValueDoc
import array.gui.settings.ReturnBehaviour
import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.text.TextFlow
import org.fxmisc.richtext.model.ReadOnlyStyledDocumentBuilder
import org.fxmisc.richtext.model.StyledDocument
import org.fxmisc.richtext.model.StyledSegment
import org.fxmisc.richtext.model.TextOps
import org.fxmisc.wellbehaved.event.EventPattern
import org.fxmisc.wellbehaved.event.InputMap
import java.util.function.BiConsumer
import java.util.function.Function
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface CommandListener {
    fun valid(text: String): Boolean
    fun handle(text: String)
}

@OptIn(ExperimentalContracts::class)
class ROStyledArea(
    val client: Client,
    applyParagraphStyle: BiConsumer<TextFlow, ParStyle>,
    styledTextOps: TextOps<EditorContent, TextStyle>,
    nodeFactory: Function<StyledSegment<EditorContent, TextStyle>, Node>
) : KAPEditorStyledArea<ParStyle, EditorContent, TextStyle>(
    ParStyle(),
    applyParagraphStyle,
    TextStyle(),
    styledTextOps,
    nodeFactory
) {
    private var updatesEnabled = false
    private var commandListener: CommandListener? = null
    private val historyListeners = ArrayList<HistoryListener>()

    init {
        undoManager = null
        displayPrompt()
    }

    override fun addInputMappings(entries: MutableList<InputMap<*>>) {
        entries.add(InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER), { sendCurrentContent() }))
        entries.add(InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER, KeyCombination.CONTROL_DOWN), { insertNewline() }))

        // History navigation
        entries.add(InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.UP), { atEditboxStart() }, { prevHistory() }))
        entries.add(InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.DOWN), { atEditboxEnd() }, { nextHistory() }))
        entries.add(InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.P, KeyCombination.CONTROL_DOWN), { atEditboxStart() }, { prevHistory() }))
        entries.add(InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.N, KeyCombination.CONTROL_DOWN), { atEditboxEnd() }, { nextHistory() }))

        // Cursor movement
        entries.add(InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.HOME), { atEditbox() }, { moveToBeginningOfInput() }))
        entries.add(InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.HOME, KeyCombination.SHIFT_DOWN), { atEditbox() }, { moveToBeginningOfInput() }))

        // Emacs-style cursor movement
        entries.add(InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.F, KeyCombination.CONTROL_DOWN), { true }, { caretSelectionBind.moveToNextChar() }))
        entries.add(InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.B, KeyCombination.CONTROL_DOWN), { true }, { caretSelectionBind.moveToPrevChar() }))
        entries.add(InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.A, KeyCombination.CONTROL_DOWN), { atEditbox() }, { moveToBeginningOfInput() }))
        entries.add(InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.E, KeyCombination.CONTROL_DOWN), { atEditbox() }, { moveToEndOfInput() }))
        entries.add(InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.K, KeyCombination.CONTROL_DOWN), { atEditbox() }, { deleteToEnd() }))
        entries.add(InputMap.consumeWhen(EventPattern.keyPressed(KeyCode.D, KeyCombination.CONTROL_DOWN), { atEditbox() }, { deleteNextChar() }))
    }

    fun displayPrompt() {
        withUpdateEnabledNoPreserveCursor {
            val inputDocument = ReadOnlyStyledDocumentBuilder(segOps, ParStyle(ParStyle.ParStyleType.NORMAL))
                .addParagraph(
                    listOf(
                        StyledSegment(EditorContent.makeString(">"), TextStyle(TextStyle.Type.PROMPT)),
                        StyledSegment(EditorContent.makeString(" "), TextStyle(TextStyle.Type.PROMPT, promptTag = true))))
                .build()
            insert(document.length(), inputDocument)
        }
    }

    fun setCommandListener(listener: CommandListener) {
        commandListener = listener
    }

    private fun atEditboxStart(): Boolean {
        val inputPosition = findInputStartEnd()
        val pos = caretPosition
        return pos >= inputPosition.inputStart && pos <= inputPosition.inputEnd
    }

    private fun atEditboxEnd(): Boolean {
        val inputPosition = findInputStartEnd()
        val pos = caretPosition
        return pos >= inputPosition.inputStart && pos <= inputPosition.inputEnd
    }

    private fun atEditbox(): Boolean {
        return true
    }

    private fun isAtInput(start: Int, end: Int): Boolean {
        return if (start == end && document.getStyleAtPosition(start).promptTag) {
            true
        } else {
            val spans = document.getStyleSpans(start, end)
            val firstNonInputSpan = spans.find { span ->
                span.style.type != TextStyle.Type.INPUT
            }
            firstNonInputSpan == null
        }
    }

    fun addHistoryListener(historyListener: HistoryListener) {
        historyListeners.add(historyListener)
    }

    private fun insertNewline() {
        insertText(caretPosition, "\n")
    }

    private fun prevHistory() {
        historyListeners.forEach { it.prevHistory() }
    }

    private fun nextHistory() {
        historyListeners.forEach { it.nextHistory() }
    }

    private fun findInputStartEnd(): InputPositions {
        var pos = document.length() - 1
        while (pos >= 0) {
            val style = document.getStyleOfChar(pos)
            if (style.promptTag) {
                break
            }
            pos--
        }
        assertx(pos >= 0)

        val inputStartPos = pos + 1
        while (pos >= 0) {
            val style = document.getStyleOfChar(pos)
            if (style.type != TextStyle.Type.PROMPT) {
                break
            }
            pos--
        }

        val promptStartPos = pos + 1
        return InputPositions(promptStartPos, inputStartPos, document.length())
    }

    private fun sendCurrentContent() {
        val inputPosition = findInputStartEnd()
        val text = document.subSequence(inputPosition.inputStart, inputPosition.inputEnd).text
        val listener = commandListener
        if (listener != null && listener.valid(text)) {
            if (client.settings.newlineBehaviourWithDefault() === ReturnBehaviour.CLEAR_INPUT) {
                withUpdateEnabledNoPreserveCursor {
                    deleteText(inputPosition.inputStart, inputPosition.inputEnd)
                }
                moveToEndOfInput()
            }
            listener.handle(text)
        }
    }

    fun currentInput(): String {
        val inputPosition = findInputStartEnd()
        return document.subSequence(inputPosition.inputStart, inputPosition.inputEnd).text
    }

    fun <T> withUpdateEnabled(fn: () -> T): T {
        contract { callsInPlace(fn, InvocationKind.EXACTLY_ONCE) }
        val oldEnabled = updatesEnabled
        updatesEnabled = true
        try {
            return withPreservedSelection {
                fn()
            }
        } finally {
            updatesEnabled = oldEnabled
        }
    }

    fun <T> withUpdateEnabledNoPreserveCursor(fn: () -> T): T {
        contract { callsInPlace(fn, InvocationKind.EXACTLY_ONCE) }
        val oldEnabled = updatesEnabled
        updatesEnabled = true
        try {
            return fn()
        } finally {
            updatesEnabled = oldEnabled
        }
    }

    private fun <T> withPreservedSelection(fn: () -> T): T {
        contract { callsInPlace(fn, InvocationKind.EXACTLY_ONCE) }
        val inputPosition = findInputStartEnd()
        val sel = caretSelectionBind.underlyingSelection
        val selStart = sel.startPosition
        val selEnd = sel.endPosition
        val selectionStartOffset = selStart - inputPosition.inputStart
        val selectionEndOffset = selEnd - inputPosition.inputStart
        val result = fn()
        val newPosition = findInputStartEnd()
        if (selStart >= inputPosition.inputStart && selEnd <= inputPosition.inputEnd) {
            caretSelectionBind.selectRange(selectionStartOffset + newPosition.inputStart, selectionEndOffset + newPosition.inputStart)
        }
        return result
    }

    fun appendTextEnd(
        text: String,
        style: TextStyle,
        parStyle: ParStyle? = null
    ): StyledDocument<ParStyle, EditorContent, TextStyle> {
        val builder = ReadOnlyStyledDocumentBuilder(segOps, parStyle ?: ParStyle())
        text.split("\n").forEach { part ->
            builder.addParagraph(EditorContent.makeString(part), style)
        }
        val inputPos = findInputStartEnd()
        val doc = builder.build()
        withUpdateEnabled {
            insert(inputPos.promptStartPos, doc)
        }
        showBottomParagraphAtTop()
        return doc
    }

    fun appendExpressionResultEnd(value: EvalExpressionResult, style: TextStyle, parStyle: ParStyle = ParStyle()) {
        withUpdateEnabled {
            val newDoc = makeKapValueDoc(segOps, value, style, parStyle)
            val inputPos = findInputStartEnd()
            insert(inputPos.promptStartPos, newDoc)
        }
        Platform.runLater {
            showBottomParagraphAtTop()
        }
    }

    fun appendErrorMessage(text: String) {
        withUpdateEnabled {
            val inputPos = findInputStartEnd()
            val newDoc = ReadOnlyStyledDocumentBuilder(segOps, ParStyle())
                .addParagraph(
                    mutableListOf(
                        StyledSegment(EditorContent.makeString(text), TextStyle(TextStyle.Type.ERROR))))
                .addParagraph(EditorContent.makeBlank(), TextStyle(TextStyle.Type.ERROR))
                .build()
            insert(inputPos.promptStartPos, newDoc)
        }
        showBottomParagraphAtTop()
    }

    fun appendOutputEnd(text: String) {
        withUpdateEnabled {
            val textStyle = TextStyle(TextStyle.Type.OUTPUT)
            val builder = ReadOnlyStyledDocumentBuilder(segOps, ParStyle(ParStyle.ParStyleType.OUTPUT))
            text.split("\n").forEach { part -> builder.addParagraph(EditorContent.makeString(part), textStyle) }

            val inputPos = findInputStartEnd()
            val p = inputPos.promptStartPos
            // Input position at the beginning of the buffer
            if (p == 0) {
                TODO("This code path is never tested")
            } else {
                val style = document.getParagraphStyleAtPosition(p - 1)
                val newPos = if (style.type == ParStyle.ParStyleType.OUTPUT) {
                    p - 1
                } else {
                    builder.addParagraph(EditorContent.makeBlank(), textStyle)
                    p
                }
                insert(newPos, builder.build())
            }
        }
    }

    override fun replace(start: Int, end: Int, replacement: StyledDocument<ParStyle, EditorContent, TextStyle>) {
        when {
            updatesEnabled -> super.replace(start, end, replacement)
            isAtInput(start, end) -> super.replace(start, end, makeInputStyle(replacement.text))
        }
    }

    private fun makeInputStyle(s: String): StyledDocument<ParStyle, EditorContent, TextStyle> {
        return ReadOnlyStyledDocumentBuilder(segOps, ParStyle())
            .addParagraph(EditorContent.makeString(s), TextStyle(type = TextStyle.Type.INPUT))
            .build()
    }

    fun replaceInputText(s: String) {
        val inputPos = findInputStartEnd()
        withUpdateEnabledNoPreserveCursor {
            deleteText(inputPos.inputStart, inputPos.inputEnd)
            replace(inputPos.inputStart, inputPos.inputStart, makeInputStyle(s))
        }
        moveToEndOfInput()
    }

    private fun moveToBeginningOfInput() {
        val inputPosition = findInputStartEnd()
        caretSelectionBind.moveTo(inputPosition.inputStart)
    }

    private fun moveToEndOfInput() {
        val inputPosition = findInputStartEnd()
        caretSelectionBind.moveTo(inputPosition.inputEnd)
    }

    private fun deleteToEnd() {
        val inputPosition = findInputStartEnd()
        withUpdateEnabled {
            deleteText(caretSelectionBind.position, inputPosition.inputEnd)
        }
    }

    data class InputPositions(val promptStartPos: Int, val inputStart: Int, val inputEnd: Int)
}
