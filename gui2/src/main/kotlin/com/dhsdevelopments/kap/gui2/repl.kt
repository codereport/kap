package com.dhsdevelopments.kap.gui2

import array.*
import java.awt.Color
import java.awt.Font
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JTextPane
import javax.swing.text.*
import javax.swing.text.Position
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.max

object ResultValueAttribute

class ReplPanel(val computeQueue: ComputeQueue, fontIn: Font) : JTextPane() {
    val history = ArrayList<HistoryEntry>()
    var historyPosition = 0
    var resultHistoryMaxSize = 10
    val resultHistory = ArrayDeque<Style>()

    init {
        font = fontIn.deriveFont(18.0f)
        val doc = ReplDoc()
        styledDocument = doc

        doc.documentFilter = ReplFilter()

        computeQueue.addStandardOutputListener(StdoutListener(::appendToOutput))
        addMouseListener(ReplMouseListener())
        addKeyListener(ReplKeyListener())
        enableKapKeyboard(this)
        setCaretPosition(doc.length)
    }

    val replDoc get() = styledDocument as ReplDoc

    val errorStyle: Style = addStyle("error", null).also { style ->
        StyleConstants.setForeground(style, Color.RED)
    }

    val errorLocationStyle: Style = addStyle("errorLocation", null).also { style ->
        StyleConstants.setForeground(style, Color.RED)
        StyleConstants.setUnderline(style, true)
    }

    val commandHistoryStyle: Style = addStyle("commandHistory", null).also { style ->
        StyleConstants.setForeground(style, Color(0, 104, 0))
    }

    fun appendToOutput(s: String, style: AttributeSet? = null, appendNewline: Boolean = false) {
        // This is run in the calculation thread, but it's safe thanks to swing documents being thread safe
        withNoCursorMovementAndEditable {
            styledDocument.insertString(replDoc.outputPos.offset - 1, s, style)
            if (appendNewline) {
                styledDocument.insertString(replDoc.outputPos.offset - 1, "\n", null)
            }
        }
    }

    private fun addCommandToHistoryAndSend(s: String) {
        val textIndex: Int
        withNoCursorMovementAndEditable {
            replDoc.insertString(replDoc.outputPos.offset - 1, "    ", null)
            textIndex = replDoc.outputPos.offset - 1
            replDoc.insertString(replDoc.outputPos.offset - 1, s, commandHistoryStyle)
            replDoc.insertString(replDoc.outputPos.offset - 1, "\n", null)
        }
        val docPos = replDoc.createPosition(textIndex)
        val src = ReplSourceLocation(s, docPos)
        if (history.isEmpty() || history.last().src.text != s) {
            history.add(HistoryEntry(src))
            historyPosition = history.size - 1
        }
        sendToInterpreter(src)
    }

    @OptIn(ExperimentalContracts::class)
    fun <T> withNoCursorMovementAndEditable(fn: () -> T): T {
        contract { callsInPlace(fn, InvocationKind.EXACTLY_ONCE) }
        val editorCaret = caret as DefaultCaret
        val oldUpdatePolicy = editorCaret.updatePolicy
        editorCaret.updatePolicy = DefaultCaret.ALWAYS_UPDATE
        try {
            return replDoc.withUpdateAllowed {
                fn()
            }
        } finally {
            editorCaret.updatePolicy = oldUpdatePolicy
        }
    }

    private fun sendToInterpreter(src: SourceLocation) {
        val req = Request { engine ->
            when (val result = evalExpression(engine, src)) {
                is Either.Left -> formatAndAddResultToDoc(result.value)
                is Either.Right -> formatAndAddErrorToDoc(result.value)
            }
        }
        computeQueue.requestJob(req)
    }

    private fun formatAndAddResultToDoc(result: EvalExpressionResult) {
        val buf = StringBuilder()
        for (s in result.formatttedResult) {
            buf.append(s)
            buf.append("\n")
        }
        val style = addResultToHistory(result.value)
        appendToOutput(buf.toString(), style = style)
    }

    private fun addResultToHistory(value: APLValue): Style {
        val style = addStyle(null, null)
        style.addAttribute(ResultValueAttribute, ResultValueReference(value))
        while (resultHistory.size >= max(resultHistoryMaxSize, 1)) {
            val oldStyle = resultHistory.removeFirst()
            val oldRef = oldStyle.getAttribute(ResultValueAttribute) as ResultValueReference
            oldRef.value = null
        }
        if (resultHistoryMaxSize > 0) {
            resultHistory.add(style)
        }
        return style
    }

    @Suppress("USELESS_IS_CHECK")
    private fun formatAndAddErrorToDoc(value: Exception) {
        if (value is APLGenericException) {
            val pos = value.pos
            if (pos != null) {
                val src = pos.source
                if (src is ReplSourceLocation) {
                    val startPos = src.computeOffset(pos.line, pos.col) + src.docPos.offset
                    val endPos = src.computeOffset(pos.computedEndLine, pos.computedEndCol) + src.docPos.offset
                    replDoc.setCharacterAttributes(startPos, endPos - startPos, errorLocationStyle, false)
                }
            }
        }
        val message = when (value) {
            is APLGenericException -> value.formattedError()
            else -> value.message ?: "no description"
        }
        appendToOutput(message, style = errorStyle, appendNewline = true)
    }

    private fun evalExpression(engine: Engine, src: SourceLocation): Either<EvalExpressionResult, Exception> {
        return try {
            val (result, formatted) = engine.parseAndEvalWithFormat(src)
            Either.Left(EvalExpressionResult(result, formatted))
        } catch (e: Exception) {
            Either.Right(e)
        }
    }

    private fun traverseHistory(offset: Int) {
        val newPos = historyPosition + offset
        if (newPos !in 0..history.size) return
        historyPosition = newPos
        val s = if (newPos == history.size) "" else history[newPos].src.text
        replaceInput(s)
    }

    private fun editedText(): String {
        val pos = replDoc.startEditPosition()
        return this.getText(pos, document.length - pos)
    }

    private fun replaceInput(s: String) {
        withNoCursorMovementAndEditable {
            val pos = replDoc.startEditPosition()
            replDoc.remove(pos, document.length - pos)
            replDoc.insertString(pos, s, null)
        }
    }

    private fun handlePopup(event: MouseEvent) {
        val position = viewToModel2D(event.point)
        val element = replDoc.getCharacterElement(position)
        val value = element.attributes.getAttribute(ResultValueAttribute) as ResultValueReference?
        val menu = JPopupMenu().apply {
            add("Test")
            if (value != null) {
                val v = value.value
                if (v != null) {
                    add(JPopupMenu.Separator())
                    add(JMenuItem("Copy as code").apply { addActionListener { copyValueAsCode(v) } })
                    add(JMenuItem("Copy as HTML").apply { addActionListener { copyValueAsHtml(v) } })
                }
            }
        }
        menu.show(this, event.x, event.y)
    }

    private fun copyValueAsCode(value: APLValue) {
        val transferable = StringSelection(value.formatted(FormatStyle.READABLE))
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(transferable, null)
    }

    private fun copyValueAsHtml(value: APLValue) {
        val buf = StringBuilder()
        val transferable = HtmlTransferable(value.formatted(FormatStyle.PLAIN), buf.toString())
        value.asHtml(buf)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(transferable, null)
    }

    inner class ReplKeyListener : KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
            when (e.keyCode) {
                KeyEvent.VK_ENTER -> {
                    if (e.isShiftDown) {
                        replDoc.insertString(caretPosition, "\n", null)
                    } else {
                        val text = editedText()
                        addCommandToHistoryAndSend(text)
                    }
                    e.consume()
                }
                KeyEvent.VK_UP -> {
                    when {
                        caretOnFirstEditLine() -> {
                            e.consume()
                            traverseHistory(-1)
                        }
                        caretPosition < replDoc.startEditPosition() -> {
                            e.consume()
                        }
                    }
                }
                KeyEvent.VK_DOWN -> {
                    when {
                        caretOnLastEditLine() -> {
                            e.consume()
                            traverseHistory(1)
                        }
                        caretPosition < replDoc.startEditPosition() -> {
                            e.consume()
                        }
                    }
                }
                KeyEvent.VK_HOME -> {
                    if (caretOnFirstEditLine()) {
                        caretPosition = replDoc.startEditPosition()
                        e.consume()
                    }
                }
            }
        }
    }

    inner class ReplMouseListener : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
            if (e.isPopupTrigger) {
                handlePopup(e)
            }
        }

        override fun mouseReleased(e: MouseEvent) {
            if (e.isPopupTrigger) {
                handlePopup(e)
            }
        }
    }

    private fun caretOnFirstEditLine(): Boolean {
        val curr = caretPosition
        val start = replDoc.startEditPosition()
        if (curr < start) {
            return false
        }
        val text = replDoc.getText(start, replDoc.length - start)
        val index = text.indexOf('\n')
        return index == -1 || (curr - start) < index
    }

    private fun caretOnLastEditLine(): Boolean {
        val curr = caretPosition
        val start = replDoc.startEditPosition()
        if (curr < start) {
            return false
        }
        val text = replDoc.getText(start, replDoc.length - start)
        val index = text.lastIndexOf('\n')
        return index == -1 || (curr - start) > index
    }
}

class ReplDoc : DefaultStyledDocument() {
    val outputPos: Position
    private val startEditAreaMark: Position
    var updateAllowed: Boolean = false
        private set

    init {
        val promptText = "> "
        insertString(0, promptText, null)
        outputPos = createPosition(1)
        startEditAreaMark = createPosition(promptText.length - 1)
    }

    fun startEditPosition() = startEditAreaMark.offset + 1

    @OptIn(ExperimentalContracts::class)
    fun <T> withUpdateAllowed(fn: () -> T): T {
        contract { callsInPlace(fn, InvocationKind.EXACTLY_ONCE) }
        val oldUpdateAllowed = updateAllowed
        updateAllowed = true
        try {
            return fn()
        } finally {
            updateAllowed = oldUpdateAllowed
        }
    }
}

class ReplFilter : DocumentFilter() {
    override fun remove(fb: FilterBypass, offset: Int, length: Int) {
        checkRangeIsEditable(fb.document as ReplDoc, offset, length)
        super.remove(fb, offset, length)
    }

    override fun insertString(fb: FilterBypass, offset: Int, string: String?, attr: AttributeSet?) {
        checkRangeIsEditable(fb.document as ReplDoc, offset, 0)
        super.insertString(fb, offset, string, attr)
    }

    override fun replace(fb: FilterBypass, offset: Int, length: Int, text: String?, attrs: AttributeSet?) {
        checkRangeIsEditable(fb.document as ReplDoc, offset, 0)
        super.replace(fb, offset, length, text, attrs)
    }

    private fun checkRangeIsEditable(replDoc: ReplDoc, offset: Int, length: Int) {
        if (!replDoc.updateAllowed && !(offset >= replDoc.startEditPosition() && offset + length <= replDoc.length)) {
            throw BadLocationException("Region cannot be updated", offset)
        }
    }
}

class ReplSourceLocation(val text: String, val docPos: Position) : SourceLocation {
    override fun sourceText() = text
    override fun open() = StringCharacterProvider(text)

    fun computeOffset(line: Int, col: Int): Int {
        var i = 0
        var rowIndex = 0
        while (rowIndex < line && i < text.length) {
            val ch = text[i++]
            if (ch == '\n') {
                rowIndex++
            }
        }
        return i + col
    }
}

class HistoryEntry(val src: ReplSourceLocation)
class ResultValueReference(var value: APLValue?)
class EvalExpressionResult(val value: APLValue, val formatttedResult: List<String>)
