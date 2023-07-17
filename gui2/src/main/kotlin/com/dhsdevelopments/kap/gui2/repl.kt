package com.dhsdevelopments.kap.gui2

import array.*
import java.awt.Color
import java.awt.Font
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JTextPane
import javax.swing.text.*
import javax.swing.text.Position
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class ReplPanel(val computeQueue: ComputeQueue, fontIn: Font) : JTextPane() {
    val history = ArrayList<HistoryEntry>()

    init {
        font = fontIn.deriveFont(18.0f)
        val doc = ReplDoc()
        styledDocument = doc

        doc.documentFilter = ReplFilter()

        computeQueue.addStandardOutputListener(StdoutListener(::appendToOutput))
        setCaretPosition(doc.length)
        addKeyListener(ReplKeyListener())
        enableKapKeyboard(this)
    }

    val replDoc get() = styledDocument as ReplDoc

    val errorStyle = addStyle("error", null).also { style ->
        StyleConstants.setForeground(style, Color.RED)
    }

    val commandHistoryStyle = addStyle("commandHistoryParent", null).also { style ->
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
        val style = addStyle(null, commandHistoryStyle)
        val textIndex: Int
        withNoCursorMovementAndEditable {
            styledDocument.insertString(replDoc.outputPos.offset - 1, "    ", null)
            textIndex = replDoc.outputPos.offset
            styledDocument.insertString(replDoc.outputPos.offset - 1, s, style)
            styledDocument.insertString(replDoc.outputPos.offset - 1, "\n", null)
        }
        val docPos = replDoc.createPosition(textIndex)
        val src = ReplSourceLocation(s, style, docPos)
        if (history.isEmpty() || history.last().src.text != s) {
            history.add(HistoryEntry(src))
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

    fun editedText(): String {
        val pos = replDoc.startEditPosition()
        return this.getText(pos, document.length - pos)
    }

    private fun sendToInterpreter(src: SourceLocation) {
        val req = Request { engine ->
            val result = evalExpression(engine, src)
            when (result) {
                is Either.Left -> formatAndAddResultToDoc(result.value)
                is Either.Right -> formatAndAddErrorToDoc(result.value)
            }
        }
        computeQueue.requestJob(req)
    }

    private fun formatAndAddResultToDoc(result: APLValue) {
        val buf = StringBuilder()
        formatResult(result) { s ->
            buf.append(s)
            buf.append("\n")
        }
        appendToOutput(buf.toString())
    }

    @Suppress("USELESS_IS_CHECK")
    private fun formatAndAddErrorToDoc(value: Exception) {
        if (value is APLGenericException) {
            val pos = value.pos
            if (pos != null) {
                val src = pos.source
                if (src is ReplSourceLocation) {
                    //println("error pos: ${src.docPos}")
                    //TODO: Highlight error
                }
            }
        }
        val message = when (value) {
            is APLGenericException -> value.formattedError()
            else -> value.message ?: "no description"
        }
        appendToOutput(message, style = errorStyle, appendNewline = true)
    }

    private fun evalExpression(engine: Engine, src: SourceLocation): Either<APLValue, Exception> {
        return try {
            val result = engine.parseAndEval(src, formatResult = true)
            Either.Left(result)
        } catch (e: Exception) {
            Either.Right(e)
        }
    }

    inner class ReplKeyListener : KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
            if (e.keyChar == '\n') {
                val text = editedText()
                addCommandToHistoryAndSend(text)
                e.consume()
            }
        }
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

class ReplSourceLocation(val text: String, val style: Style, val docPos: Position) : SourceLocation {
    override fun sourceText() = text
    override fun open() = StringCharacterProvider(text)
}

class HistoryEntry(val src: ReplSourceLocation)
