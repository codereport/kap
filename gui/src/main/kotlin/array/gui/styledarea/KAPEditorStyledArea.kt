package array.gui.styledarea

import array.keyboard.ExtendedCharsKeyboardInput
import javafx.event.Event
import javafx.scene.Node
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import javafx.scene.text.TextFlow
import org.fxmisc.richtext.GenericStyledArea
import org.fxmisc.richtext.model.GenericEditableStyledDocument
import org.fxmisc.richtext.model.StyledSegment
import org.fxmisc.richtext.model.TextOps
import org.fxmisc.wellbehaved.event.EventPattern
import org.fxmisc.wellbehaved.event.InputHandler
import org.fxmisc.wellbehaved.event.InputMap
import org.fxmisc.wellbehaved.event.Nodes
import org.reactfx.util.FxTimer
import java.time.Duration
import java.util.function.BiConsumer
import java.util.function.Function

open class KAPEditorStyledArea<PS, SEG, S>(
    parStyle: PS,
    applyParagraphStyle: BiConsumer<TextFlow, PS>,
    textStyle: S,
    segmentOps: TextOps<SEG, S>,
    nodeFactory: Function<StyledSegment<SEG, S>, Node>
) : GenericStyledArea<PS, SEG, S>(
    parStyle,
    applyParagraphStyle,
    textStyle,
    GenericEditableStyledDocument(parStyle, textStyle, segmentOps),
    segmentOps,
    nodeFactory
) {
    private var defaultKeymap: InputMap<*> = Nodes.getInputMap(this)
    private var prefixActive = false
    private val extendedInput = ExtendedCharsKeyboardInput()
    private var currentInputMap: InputMap<out Event>? = null

    init {
        stylesheets.add("/array/gui/interactor.css")
        styleClass.addAll("editcontent", "kapfont")
        updateKeymap()
    }

    fun updateKeymap() {
        val entries = ArrayList<InputMap<out Event>>()

        // Keymap
        extendedInput.keymap.forEach { e ->
            val modifiers =
                if (e.key.shift) arrayOf(KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN) else arrayOf(KeyCombination.ALT_DOWN)
            val v = InputMap.consume(EventPattern.keyTyped(e.key.character, *modifiers), { replaceSelectionAndDisplay(e.value) })
            entries.add(v)
        }
        entries.add(InputMap.consume(EventPattern.keyTyped(" ", KeyCombination.ALT_DOWN), { replaceSelectionAndDisplay(" ") }))
        addInputMappings(entries)

        // Prefix input
        val prefixChar = "`" // this should be read from config
        entries.add(makePrefixInputKeymap(prefixChar))

        entries.add(defaultKeymap)
        if (currentInputMap != null) {
            Nodes.removeInputMap(this, currentInputMap)
        }
        currentInputMap = InputMap.sequence(*entries.toTypedArray())
        Nodes.pushInputMap(this, currentInputMap)
    }

    open fun addInputMappings(entries: MutableList<InputMap<out Event>>) {}

    private fun makePrefixInputKeymap(@Suppress("SameParameterValue") prefixChar: String): InputMap<out Event> {
        fun disableAndAdd(s: String) {
            prefixActive = false
            replaceSelectionAndDisplay(s)
        }

        fun processKey(event: KeyEvent) {
            val charMapping = extendedInput.keymap[ExtendedCharsKeyboardInput.KeyDescriptor(
                event.character,
                event.isShiftDown)]
            if (charMapping == null) {
                disableAndAdd(prefixChar)
            } else {
                disableAndAdd(charMapping)
            }
        }

        fun emptyKeyModifiers(event: KeyEvent): Boolean {
            return !(event.isAltDown || event.isShiftDown || event.isControlDown || event.isMetaDown)
        }

        val entries = ArrayList<InputMap<out Event>>()
        entries.add(InputMap.consume(EventPattern.keyTyped(prefixChar)) { event ->
            if (!prefixActive) {
                prefixActive = true
            } else {
                processKey(event)
            }
        })
        entries.add(InputMap.process(EventPattern.keyTyped()) { event ->
            when {
                !prefixActive -> {
                    InputHandler.Result.PROCEED
                }
                event.character == " " && emptyKeyModifiers(event) -> {
                    prefixActive = false
                    replaceSelectionAndDisplay(prefixChar)
                    InputHandler.Result.CONSUME
                }
                else -> {
                    prefixActive = false
                    val charMapping = extendedInput.keymap[ExtendedCharsKeyboardInput.KeyDescriptor(
                        event.character,
                        event.isShiftDown)]
                    if (charMapping == null) {
                        InputHandler.Result.PROCEED
                    } else {
                        replaceSelectionAndDisplay(charMapping)
                        InputHandler.Result.CONSUME
                    }
                }
            }
        })
        return InputMap.sequence(*entries.toTypedArray())
    }

    private var outstandingScroll = 0

    fun showBottomParagraphAtTop() {
        outstandingScroll++
        FxTimer.runLater(Duration.ofMillis(100)) {
            if (--outstandingScroll == 0) {
                showParagraphAtTop(document.paragraphs.size - 1)
            }
        }
    }

    fun replaceSelectionAndDisplay(s: String) {
        replaceSelection(s)
//        showBottomParagraphAtTop()
    }

    fun clearStyles() {
        repeat(paragraphs.size) { i ->
            setStyle(i, initialTextStyle)
        }
    }

    fun setStyleForRange(startLine: Int, startCol: Int, endLine: Int, endCol: Int, textStyle: S) {
        val numLines = endLine - startLine + 1
        when {
            numLines == 1 -> {
                setStyle(startLine, startCol, endCol, textStyle)
            }
            numLines > 1 -> {
                setStyle(startLine, startCol, paragraphs[startLine].length(), textStyle)
                repeat(numLines - 2) { i ->
                    val rowIndex = startLine + i + 1
                    setStyle(rowIndex, 0, paragraphs[rowIndex].length(), textStyle)
                }
                setStyle(endLine, 0, endCol, textStyle)
            }
        }
    }
}
