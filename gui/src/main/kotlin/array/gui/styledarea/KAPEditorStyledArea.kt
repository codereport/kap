package array.gui.styledarea

import array.gui.ExtendedCharsKeyboardInput
import javafx.event.Event
import javafx.scene.Node
import javafx.scene.input.KeyCombination
import javafx.scene.text.TextFlow
import org.fxmisc.richtext.GenericStyledArea
import org.fxmisc.richtext.model.EditableStyledDocument
import org.fxmisc.richtext.model.StyledSegment
import org.fxmisc.richtext.model.TextOps
import org.fxmisc.wellbehaved.event.EventPattern
import org.fxmisc.wellbehaved.event.InputHandler
import org.fxmisc.wellbehaved.event.InputMap
import org.fxmisc.wellbehaved.event.Nodes
import java.util.function.BiConsumer
import java.util.function.Function

open class KAPEditorStyledArea(
    val keyboardInput: ExtendedCharsKeyboardInput,
    parStyle: ParStyle,
    applyParagraphStyle: BiConsumer<TextFlow, ParStyle>,
    textStyle: TextStyle,
    document: EditableStyledDocument<ParStyle, String, TextStyle>,
    segmentOps: TextOps<String, TextStyle>,
    nodeFactory: Function<StyledSegment<String, TextStyle>, Node>
) : GenericStyledArea<ParStyle, String, TextStyle>(
    parStyle,
    applyParagraphStyle,
    textStyle,
    document,
    segmentOps,
    nodeFactory
) {
    private var defaultKeymap: InputMap<*> = Nodes.getInputMap(this)
    private var prefixActive = false

    init {
        updateKeymap()
    }

    private fun updateKeymap() {
        val entries = ArrayList<InputMap<out Event>>()

        // Keymap
        keyboardInput.keymap.forEach { e ->
            val modifiers =
                if (e.key.shift) arrayOf(KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN) else arrayOf(KeyCombination.ALT_DOWN)
            val v = InputMap.consume(EventPattern.keyTyped(e.key.character, *modifiers), { replaceSelection(e.value) })
            entries.add(v)
        }

        addInputMappings(entries)

        // Prefix input
        val prefixChar = "." // this should be read from config
        entries.add(makePrefixInputKeymap(prefixChar))

        entries.add(defaultKeymap)
        Nodes.pushInputMap(this, InputMap.sequence(*entries.toTypedArray()))
    }

    open fun addInputMappings(entries: MutableList<InputMap<out Event>>) {}

    private fun makePrefixInputKeymap(prefixChar: String): InputMap<out Event> {
        fun verifyPrefixActive(): Boolean {
            return prefixActive
        }

        fun disableAndAdd(s: String) {
            prefixActive = false
            replaceSelection(s)
        }

        val entries = ArrayList<InputMap<out Event>>()
        entries.add(InputMap.consume(EventPattern.keyTyped(prefixChar), {
            if (!prefixActive) {
                prefixActive = true
            } else {
                disableAndAdd(prefixChar)
            }
        }))
        entries.add(InputMap.process(EventPattern.keyTyped(),
            { event ->
                if (!prefixActive) {
                    InputHandler.Result.PROCEED
                } else {
                    prefixActive = false
                    val charMapping = keyboardInput.keymap[ExtendedCharsKeyboardInput.KeyDescriptor(event.character, event.isShiftDown)]
                    if (charMapping == null) {
                        InputHandler.Result.PROCEED
                    } else {
                        replaceSelection(charMapping)
                        InputHandler.Result.CONSUME
                    }
                }
            }))
        return InputMap.sequence(*entries.toTypedArray())
    }
}
