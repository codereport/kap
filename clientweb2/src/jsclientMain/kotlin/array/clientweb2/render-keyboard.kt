package array.clientweb2

import array.keyboard.ExtendedCharsKeyboardInput
import kotlinx.html.*

private data class Key(val unshifted: String, val shifted: String, val width: Int = 2)

private val row1 = listOf(
    Key("`", "~"), Key("1", "!"), Key("2", "@"), Key("3", "#"), Key("4", "$"), Key("5", "%"),
    Key("6", "^"), Key("7", "&"), Key("8", "*"), Key("9", "("), Key("0", ")"), Key("-", "_"),
    Key("=", "+"), Key("BS", "", 4))

private val row2 = listOf(
    Key("Tab", "", 3), Key("q", "Q"), Key("w", "W"), Key("e", "E"), Key("r", "R"), Key("t", "T"),
    Key("y", "Y"), Key("u", "U"), Key("i", "I"), Key("o", "O"), Key("p", "P"),
    Key("[", "{"), Key("]", "}"), Key("\\", "|", 3))

private val row3 = listOf(
    Key("Caps", "", 4), Key("a", "A"), Key("s", "S"), Key("d", "D"), Key("f", "F"), Key("g", "G"),
    Key("h", "H"), Key("j", "J"), Key("k", "K"), Key("l", "L"), Key(";", ":"),
    Key("'", "\""), Key("Enter", "", 4))

private val row4 = listOf(
    Key("Shift", "", 5), Key("z", "Z"), Key("x", "X"), Key("c", "C"), Key("v", "V"), Key("b", "B"),
    Key("n", "N"), Key("m", "M"), Key(",", "<"), Key(".", ">"),
    Key("/", "?"), Key("Shift", "", 5))

private val rows = listOf(row1, row2, row3, row4)

fun DIV.createKeyboardHelp() {
    val keyboard = ExtendedCharsKeyboardInput()
    div {
        table(classes = "keyboard") {
            for (row in rows) {
                tr {
                    renderRow(keyboard, row)
                }
            }
        }
    }
}

private fun TR.renderRow(keyboard: ExtendedCharsKeyboardInput, row: List<Key>) {
    for (key in row) {
        renderCell(keyboard, key)
    }
}

private fun TR.renderCell(keyboard: ExtendedCharsKeyboardInput, key: Key) {
    val unshiftedMapping = keyboard.keymap[ExtendedCharsKeyboardInput.KeyDescriptor(key.unshifted, false)]
    val shiftedMapping = keyboard.keymap[ExtendedCharsKeyboardInput.KeyDescriptor(key.shifted, true)]
    td {
        colSpan = key.width.toString()
        div {
            span { +key.shifted }
            span { +(shiftedMapping ?: "") }
        }
        div {
            span { +key.unshifted }
            span { +(unshiftedMapping ?: "") }
        }
    }
}
