package array.gui.styledarea

import array.gui.Client
import array.gui.ExtendedCharsKeyboardInput
import javafx.event.Event
import javafx.scene.Node
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
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

open class KAPEditorStyledArea<P, S>(
    val client: Client,
    parStyle: P,
    applyParagraphStyle: BiConsumer<TextFlow, P>,
    textStyle: TextStyle,
    document: EditableStyledDocument<P, S, TextStyle>,
    segmentOps: TextOps<S, TextStyle>,
    nodeFactory: Function<StyledSegment<S, TextStyle>, Node>
) : GenericStyledArea<P, S, TextStyle>(
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
        stylesheets.add("/array/gui/interactor.css")
        updateKeymap()
    }

    private fun updateKeymap() {
        val entries = ArrayList<InputMap<out Event>>()

        // Keymap
        client.renderContext.extendedInput().keymap.forEach { e ->
            val modifiers =
                if (e.key.shift) arrayOf(KeyCombination.ALT_DOWN, KeyCombination.SHIFT_DOWN) else arrayOf(KeyCombination.ALT_DOWN)
            val v = InputMap.consume(EventPattern.keyTyped(e.key.character, *modifiers), { replaceSelection(e.value) })
            entries.add(v)
        }
        entries.add(InputMap.consume(EventPattern.keyTyped(" ", KeyCombination.ALT_DOWN), { replaceSelection(" ") }))
        addInputMappings(entries)

        // Prefix input
        val prefixChar = "`" // this should be read from config
        entries.add(makePrefixInputKeymap(prefixChar))

        entries.add(defaultKeymap)
        Nodes.pushInputMap(this, InputMap.sequence(*entries.toTypedArray()))
    }

    open fun addInputMappings(entries: MutableList<InputMap<out Event>>) {}

    private fun makePrefixInputKeymap(prefixChar: String): InputMap<out Event> {
        fun disableAndAdd(s: String) {
            prefixActive = false
            replaceSelection(s)
        }

        fun processKey(event: KeyEvent) {
            val charMapping = client.renderContext.extendedInput().keymap[ExtendedCharsKeyboardInput.KeyDescriptor(
                event.character,
                event.isShiftDown)]
            if(charMapping == null) {
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
                    replaceSelection(prefixChar)
                    InputHandler.Result.CONSUME
                }
                else -> {
                    prefixActive = false
                    val charMapping = client.renderContext.extendedInput().keymap[ExtendedCharsKeyboardInput.KeyDescriptor(
                        event.character,
                        event.isShiftDown)]
                    if (charMapping == null) {
                        InputHandler.Result.PROCEED
                    } else {
                        replaceSelection(charMapping)
                        InputHandler.Result.CONSUME
                    }
                }
            }
        })
        return InputMap.sequence(*entries.toTypedArray())
    }
}
