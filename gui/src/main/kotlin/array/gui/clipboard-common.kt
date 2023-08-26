package array.gui

import array.APLValue
import array.FormatStyle
import javafx.scene.input.Clipboard
import javafx.scene.input.DataFormat

fun copyValueAsString(value: APLValue) {
    val clipboard = Clipboard.getSystemClipboard()
    clipboard.setContent(mapOf(DataFormat.PLAIN_TEXT to value.formatted(FormatStyle.PRETTY)))
}

fun copyValueAsCode(value: APLValue) {
    val clipboard = Clipboard.getSystemClipboard()
    clipboard.setContent(mapOf(DataFormat.PLAIN_TEXT to value.formatted(FormatStyle.READABLE)))
}

fun copyValueAsHtml(value: APLValue) {
    val buf = StringBuilder()
    value.asHtml(buf)
    val result = buf.toString()
    val clipboard = Clipboard.getSystemClipboard()
    clipboard.setContent(
        mapOf(
            DataFormat.HTML to result,
            DataFormat.PLAIN_TEXT to result))
}
