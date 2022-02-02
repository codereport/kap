package array.gui

import array.Position
import array.SourceLocation
import array.StringCharacterProvider
import array.gui.styledarea.KAPEditorStyledArea
import array.gui.styledarea.TextStyle
import javafx.event.Event
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.text.TextFlow
import javafx.stage.Stage
import javafx.stage.WindowEvent
import org.fxmisc.flowless.VirtualizedScrollPane
import org.fxmisc.richtext.StyledTextArea
import org.fxmisc.richtext.TextExt
import org.fxmisc.richtext.model.*
import org.fxmisc.wellbehaved.event.EventPattern
import org.fxmisc.wellbehaved.event.InputMap
import java.io.File
import java.io.FileWriter
import java.lang.Integer.min
import java.lang.ref.WeakReference
import java.nio.charset.StandardCharsets
import java.util.function.BiConsumer
import java.util.function.Function

class SourceEditor(val client: Client) {
    private val stage = Stage()
    private var styledArea: SourceEditorStyledArea
    private var loaded: File? = null
    private val messageArea = TextField()

    init {
        val vbox = VBox()

        val toolbar = ToolBar(
            makeToolbarButton("Save", this::processSave),
            makeToolbarButton("Run", this::runClicked))
        vbox.children.add(toolbar)

        styledArea = initStyledArea()
        val scrollArea = VirtualizedScrollPane(styledArea)
        vbox.children.add(scrollArea)
        VBox.setVgrow(scrollArea, Priority.ALWAYS)

        vbox.children.add(messageArea)

        stage.scene = Scene(vbox, 1200.0, 800.0)

        client.sourceEditors.add(this)
        stage.onCloseRequest = EventHandler { event -> handleCloseRequest(event) }
    }

    private var highlightedRow: Int? = null

    private fun initStyledArea(): SourceEditorStyledArea {
        @Suppress("UNUSED_ANONYMOUS_PARAMETER")
        val applyParagraphStyle = BiConsumer<TextFlow, SourceEditorParStyle> { flow, parStyle ->
            flow.background = when (parStyle.type) {
                SourceEditorParStyle.StyleType.NORMAL -> Background.EMPTY
                SourceEditorParStyle.StyleType.ERROR -> Background(
                    BackgroundFill(
                        Color(1.0, 0.6, 0.6, 1.0),
                        CornerRadii.EMPTY,
                        Insets.EMPTY))
            }
        }
        val nodeFactory = Function<StyledSegment<String, TextStyle>, Node> { seg ->
            val applyStyle = { a: TextExt, b: TextStyle ->
                b.styleContent(a, client.renderContext)
            }
            StyledTextArea.createStyledTextNode(seg.segment, seg.style, applyStyle)
        }
        val styledTextOps = SegmentOps.styledTextOps<TextStyle>()
        GenericEditableStyledDocument(SourceEditorParStyle(), TextStyle(), styledTextOps)
        val srcEdit = SourceEditorStyledArea(
            this,
            SourceEditorParStyle(),
            applyParagraphStyle,
            TextStyle(),
            styledTextOps,
            nodeFactory)

        srcEdit.content.paragraphs.addChangeObserver {
            if (highlightedRow != null) {
                highlightedRow = null
                srcEdit.clearHighlights()
                messageArea.text = ""
            }
        }

        return srcEdit
    }

    private fun makeToolbarButton(name: String, fn: () -> Unit): Button {
        return Button(name).apply {
            onAction = EventHandler { fn() }
        }
    }

    private fun handleCloseRequest(event: WindowEvent) {
        var close = false
        if (!styledArea.modifided) {
            close = true
        } else {
            val dialog = Dialog<ButtonType>().apply {
                title = "File is modified"
                contentText = "This file has unsaved changes. Save changes?"
                dialogPane.buttonTypes.apply {
                    add(ButtonType("Save", ButtonBar.ButtonData.YES))
                    add(ButtonType("Don't save", ButtonBar.ButtonData.NO))
                    add(ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE))
                }
            }
            val result = dialog.showAndWait()
            if (result.isPresent) {
                val type = result.get()
                if (type.buttonData == ButtonBar.ButtonData.YES) {
                    if (processSave()) {
                        close = true
                    }
                } else if (type.buttonData == ButtonBar.ButtonData.NO) {
                    close = true
                }
            }
        }
        if (close) {
            client.sourceEditors.remove(this)
        } else {
            event.consume()
        }
    }

    fun runClicked() {
        val source = EditorSourceLocation(this, styledArea.document.text)
        client.evalSource(source, true)
    }

    private fun processSave(): Boolean {
        if (loaded == null) {
            val selectedFile = client.selectFile(true) ?: return false
            loaded = selectedFile
        }
        saveContentToFile(loaded)
        styledArea.modifided = false
        return true
    }

    private fun saveContentToFile(name: File?) {
        FileWriter(name, StandardCharsets.UTF_8).use { out ->
            val content = styledArea.document.text
            val fixed = if (content.isNotEmpty() && !content.endsWith("\n")) {
                content + "\n"
            } else {
                content
            }
            out.write(fixed)
        }
    }

    fun setFile(file: File) {
        val content = file.readLines()
        styledArea.deleteText(0, styledArea.length)
        //styledArea.insert(0, content, TextStyle())
        val builder = ReadOnlyStyledDocumentBuilder(styledArea.segOps, styledArea.initialParagraphStyle)
        content.forEach { text ->
            builder.addParagraph(text, styledArea.initialTextStyle)
        }
        styledArea.insert(0, builder.build())
        loaded = file
    }

    fun show() {
        stage.show()
    }

    fun highlightError(pos: Position, message: String?) {
        try {
            messageArea.text = message ?: ""
            styledArea.caretSelectionBind.moveTo(pos.line, pos.col)
            styledArea.highlightRow(pos.line)
            highlightedRow = pos.line
            styledArea.requestFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    class EditorSourceLocation(editor: SourceEditor, val text: String) : SourceLocation {
        private val editorReference = WeakReference(editor)

        val editor get() = editorReference.get()

        override fun sourceText() = text
        override fun open() = StringCharacterProvider(text)
    }
}

class SourceEditorParStyle(val type: StyleType = StyleType.NORMAL) {
    enum class StyleType {
        NORMAL,
        ERROR
    }
}

class SourceEditorStyledArea(
    private val sourceEditor: SourceEditor,
    parStyle: SourceEditorParStyle,
    applyParagraphStyle: BiConsumer<TextFlow, SourceEditorParStyle>,
    textStyle: TextStyle,
    styledTextOps: TextOps<String, TextStyle>,
    nodeFactory: Function<StyledSegment<String, TextStyle>, Node>
) : KAPEditorStyledArea<SourceEditorParStyle, String, TextStyle>(
    parStyle,
    applyParagraphStyle,
    textStyle,
    styledTextOps,
    nodeFactory
) {
    var modifided = false

    init {
        textProperty().addListener { _, _, _ -> modifided = true }
    }

    override fun addInputMappings(entries: MutableList<InputMap<out Event>>) {
        entries.add(InputMap.consume(EventPattern.keyPressed(KeyCode.ENTER, KeyCombination.CONTROL_DOWN), { sourceEditor.runClicked() }))
        entries.add(InputMap.consume(
            EventPattern.keyPressed(KeyCode.F, KeyCombination.CONTROL_DOWN),
            { caretSelectionBind.moveToNextChar() }))
        entries.add(InputMap.consume(
            EventPattern.keyPressed(KeyCode.B, KeyCombination.CONTROL_DOWN),
            { caretSelectionBind.moveToPrevChar() }))
        entries.add(InputMap.consume(EventPattern.keyPressed(KeyCode.P, KeyCombination.CONTROL_DOWN), { moveToPrevLine() }))
        entries.add(InputMap.consume(EventPattern.keyPressed(KeyCode.N, KeyCombination.CONTROL_DOWN), { moveToNextLine() }))
        entries.add(InputMap.consume(
            EventPattern.keyPressed(KeyCode.A, KeyCombination.CONTROL_DOWN),
            { caretSelectionBind.moveToParStart() }))
        entries.add(InputMap.consume(
            EventPattern.keyPressed(KeyCode.E, KeyCombination.CONTROL_DOWN),
            { caretSelectionBind.moveToParEnd() }))
    }

    private fun moveToNextLine() {
        val n = currentParagraph
        if (n < paragraphs.size - 1) {
            val p = paragraphs[n + 1]
            caretSelectionBind.moveTo(n + 1, min(caretSelectionBind.columnPosition, p.length()))
        }
    }

    private fun moveToPrevLine() {
        val n = currentParagraph
        if (n > 0) {
            val p = paragraphs[n - 1]
            caretSelectionBind.moveTo(n - 1, min(caretSelectionBind.columnPosition, p.length()))
        }
    }

    fun highlightRow(row: Int) {
        clearHighlights()
        setParagraphStyle(row, SourceEditorParStyle(type = SourceEditorParStyle.StyleType.ERROR))
    }

    fun clearHighlights() {
        for (row in 0 until paragraphs.size) {
            setParagraphStyle(row, SourceEditorParStyle(type = SourceEditorParStyle.StyleType.NORMAL))
        }
    }
}
