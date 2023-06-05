package array.gui.reporting.edit

import javafx.scene.paint.Color
import org.fxmisc.richtext.model.Codec
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.charset.MalformedInputException
import java.util.*

data class TextStyle(
    val bold: Boolean? = null,
    val italic: Boolean? = null,
    val underline: Boolean? = null,
    val strikethrough: Boolean? = null,
    val fontSize: Int? = null,
    val fontFamily: String? = null,
    val textColour: Color? = null,
    val backgroundColour: Color? = null
) {
    fun toCss(): String {
        val sb = StringBuilder()
        fun appendBoolean(b: Boolean?, prefix: String, ts: String, fs: String) {
            if(b != null) {
                sb.append("${prefix}: ${if(b) ts else fs};")
            }
        }
        appendBoolean(bold, "-fx-font-weight", "bold", "normal")
        appendBoolean(italic, "-fx-font-style", "italic", "normal")
        appendBoolean(underline, "-fx-underline", "true", "false")
        appendBoolean(strikethrough, "-fx-strikethrough", "true", "false")
        if (fontSize != null) {
            sb.append("-fx-font-size: " + fontSize + "pt;")
        }
        if (fontFamily != null) {
            sb.append("-fx-font-family: " + fontFamily + ";")
        }
        if (textColour != null) {
            sb.append("-fx-fill: " + cssColor(textColour) + ";")
        }
        if (backgroundColour != null) {
            sb.append("-rtfx-background-color: " + cssColor(backgroundColour) + ";")
        }
        return sb.toString()
    }

    companion object {
        val EMPTY = TextStyle()
        val CODEC: Codec<TextStyle> = object : Codec<TextStyle> {
            private val OPT_STRING_CODEC = Codec.optionalCodec(Codec.STRING_CODEC)
            private val OPT_COLOR_CODEC = Codec.optionalCodec(Codec.COLOR_CODEC)

            override fun getName(): String {
                return "text-style"
            }

            @Throws(IOException::class)
            override fun encode(os: DataOutputStream, s: TextStyle) {
                os.writeByte(encodeBoldItalicUnderlineStrikethrough(s))
                os.writeInt(s.fontSize ?: -1)
                OPT_STRING_CODEC.encode(os, Optional.ofNullable(s.fontFamily))
                OPT_COLOR_CODEC.encode(os, Optional.ofNullable(s.textColour))
                OPT_COLOR_CODEC.encode(os, Optional.ofNullable(s.backgroundColour))
            }

            @Throws(IOException::class)
            override fun decode(input: DataInputStream): TextStyle {
                val bius = input.readByte()
                val fontSize = decodeOptionalUint(input.readInt())
                val fontFamily = OPT_STRING_CODEC.decode(input).orElse(null)
                val textColour = OPT_COLOR_CODEC.decode(input).orElse(null)
                val backgroundColour = OPT_COLOR_CODEC.decode(input).orElse(null)
                return TextStyle(
                    bold = bold(bius), italic = italic(bius), underline = underline(bius), strikethrough = strikethrough(bius),
                    fontSize = fontSize, fontFamily = fontFamily, textColour = textColour, backgroundColour = backgroundColour)
            }

            private fun encodeBoldItalicUnderlineStrikethrough(s: TextStyle): Int {
                return encodeOptionalBoolean(s.bold) shl 6 or (
                        encodeOptionalBoolean(s.italic) shl 4) or (
                        encodeOptionalBoolean(s.underline) shl 2) or
                        encodeOptionalBoolean(s.strikethrough)
            }

            @Throws(IOException::class)
            private fun bold(bius: Byte): Boolean? {
                return decodeOptionalBoolean(bius.toInt() shr 6 and 3)
            }

            @Throws(IOException::class)
            private fun italic(bius: Byte): Boolean? {
                return decodeOptionalBoolean(bius.toInt() shr 4 and 3)
            }

            @Throws(IOException::class)
            private fun underline(bius: Byte): Boolean? {
                return decodeOptionalBoolean(bius.toInt() shr 2 and 3)
            }

            @Throws(IOException::class)
            private fun strikethrough(bius: Byte): Boolean? {
                return decodeOptionalBoolean(bius.toInt() shr 0 and 3)
            }

            private fun encodeOptionalBoolean(ob: Boolean?): Int {
                return when (ob) {
                    null -> 0
                    false -> 2
                    true -> 3
                }
            }

            @Throws(IOException::class)
            private fun decodeOptionalBoolean(i: Int): Boolean? {
                return when (i) {
                    0 -> null
                    2 -> false
                    3 -> true
                    else -> throw MalformedInputException(0)
                }
            }

            private fun encodeOptionalUint(oi: Optional<Int>): Int {
                return oi.orElse(-1)
            }

            private fun decodeOptionalUint(i: Int): Int? {
                return if (i < 0) null else i
            }
        }

        fun bold(bold: Boolean) = EMPTY.copy(bold = bold)
        fun italic(italic: Boolean) = EMPTY.copy(italic = italic)
        fun underline(underline: Boolean) = EMPTY.copy(underline = underline)
        fun strikethrough(strikethrough: Boolean) = EMPTY.copy(strikethrough = strikethrough)
        fun fontSize(fontSize: Int) = EMPTY.copy(fontSize = fontSize)
        fun fontFamily(family: String) = EMPTY.copy(fontFamily = family)
        fun textColor(color: Color) = EMPTY.copy(textColour = color)
        fun backgroundColor(color: Color) = EMPTY.copy(backgroundColour = color)

        fun cssColor(color: Color): String {
            val red = (color.red * 255).toInt()
            val green = (color.green * 255).toInt()
            val blue = (color.blue * 255).toInt()
            return "rgb($red, $green, $blue)"
        }
    }
}
