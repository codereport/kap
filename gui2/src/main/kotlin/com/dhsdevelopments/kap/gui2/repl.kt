package com.dhsdevelopments.kap.gui2

import array.*
import org.fife.ui.rtextarea.RDocument
import org.fife.ui.rtextarea.RTextArea
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DocumentFilter
import javax.swing.text.Position

class ReplPanel(val computeQueue: ComputeQueue) : RTextArea() {
    init {
        val doc = ReplDoc()
        computeQueue.addStandardOutputListener(StdoutListener(doc::appendToOutput))
        document = doc
        setCaretPosition(doc.length)
        addKeyListener(ReplKeyListener())
    }

    private fun sendToInterpreter(text: String) {
        val req = Request { engine ->
            println("req: ${text}")
            val result = evalExpression(engine, text)
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
        (document as ReplDoc).appendToOutput(buf.toString())
    }

    private fun formatAndAddErrorToDoc(value: Exception) {
        val message = when (value) {
            is APLGenericException -> value.formattedError()
            else -> value.message ?: "no description"
        }
        (document as ReplDoc).appendToOutput(message)
    }

    private fun evalExpression(engine: Engine, text: String): Either<APLValue, Exception> {
        return try {
            val result = engine.parseAndEval(StringSourceLocation(text), formatResult = true)
            Either.Left(result)
        } catch (e: Exception) {
            Either.Right(e)
        }
    }

    inner class ReplKeyListener : KeyAdapter() {
        override fun keyPressed(e: KeyEvent) {
            if (e.keyChar == '\n') {
                val text = (document as ReplDoc).editedText()
                println("Text: '${text}'")
                sendToInterpreter(text)
            }
        }
    }
}

class ReplDoc : RDocument() {
    val outputPos: Position
    private val startEditAreaMark: Position
    var updateAllowed: Boolean = false
        private set

    init {
        val promptText = "> "
        insertString(0, promptText, null)
        outputPos = content.createPosition(0)
        startEditAreaMark = content.createPosition(promptText.length - 1)
        documentFilter = ReplFilter()
    }

    fun appendToOutput(s: String) {
        // This is run in the calculation thread, but it's safe thanks to swing documents being thread safe
        withUpdateAllowed {
            val before = outputPos.offset
            insertString(outputPos.offset, s, null)
            val after = outputPos.offset
            println("BEFORE:$before after adding ${s.length}, AFTER:$after")
        }
    }

    fun editedText(): String {
        val pos = startEditPosition()
        return this.getText(pos, length - pos)
    }

    fun startEditPosition() = startEditAreaMark.offset + 1

    fun <T> withUpdateAllowed(fn: () -> T): T {
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
