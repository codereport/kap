package array.gui.reporting.edit

import javafx.scene.paint.Color
import org.fxmisc.richtext.model.Codec
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.charset.MalformedInputException
import java.util.*

data class TextStyle(
    val bold: Optional<Boolean> = Optional.empty(),
    val italic: Optional<Boolean> = Optional.empty(),
    val underline: Optional<Boolean> = Optional.empty(),
    val strikethrough: Optional<Boolean> = Optional.empty(),
    val fontSize: Optional<Int> = Optional.empty(),
    val fontFamily: Optional<String> = Optional.empty(),
    val textColor: Optional<Color> = Optional.empty(),
    val backgroundColor: Optional<Color> = Optional.empty()
) {
    fun toCss(): String {
        val sb = StringBuilder()
        if (bold.isPresent) {
            if (bold.get()) {
                sb.append("-fx-font-weight: bold;")
            } else {
                sb.append("-fx-font-weight: normal;")
            }
        }
        if (italic.isPresent) {
            if (italic.get()) {
                sb.append("-fx-font-style: italic;")
            } else {
                sb.append("-fx-font-style: normal;")
            }
        }
        if (underline.isPresent) {
            if (underline.get()) {
                sb.append("-fx-underline: true;")
            } else {
                sb.append("-fx-underline: false;")
            }
        }
        if (strikethrough.isPresent) {
            if (strikethrough.get()) {
                sb.append("-fx-strikethrough: true;")
            } else {
                sb.append("-fx-strikethrough: false;")
            }
        }
        if (fontSize.isPresent) {
            sb.append("-fx-font-size: " + fontSize.get() + "pt;")
        }
        if (fontFamily.isPresent) {
            sb.append("-fx-font-family: " + fontFamily.get() + ";")
        }
        if (textColor.isPresent) {
            val color = textColor.get()
            sb.append("-fx-fill: " + cssColor(color) + ";")
        }
        if (backgroundColor.isPresent) {
            val color = backgroundColor.get()
            sb.append("-rtfx-background-color: " + cssColor(color) + ";")
        }
        return sb.toString()
    }

    fun updateWith(mixin: TextStyle): TextStyle {
        return TextStyle(
            if (mixin.bold.isPresent) mixin.bold else bold,
            if (mixin.italic.isPresent) mixin.italic else italic,
            if (mixin.underline.isPresent) mixin.underline else underline,
            if (mixin.strikethrough.isPresent) mixin.strikethrough else strikethrough,
            if (mixin.fontSize.isPresent) mixin.fontSize else fontSize,
            if (mixin.fontFamily.isPresent) mixin.fontFamily else fontFamily,
            if (mixin.textColor.isPresent) mixin.textColor else textColor,
            if (mixin.backgroundColor.isPresent) mixin.backgroundColor else backgroundColor
        )
    }

    fun updateBold(bold: Boolean): TextStyle {
        return TextStyle(
            Optional.of(bold),
            italic,
            underline,
            strikethrough,
            fontSize,
            fontFamily,
            textColor,
            backgroundColor
        )
    }

    fun updateItalic(italic: Boolean): TextStyle {
        return TextStyle(
            bold,
            Optional.of(italic),
            underline,
            strikethrough,
            fontSize,
            fontFamily,
            textColor,
            backgroundColor
        )
    }

    fun updateUnderline(underline: Boolean): TextStyle {
        return TextStyle(
            bold,
            italic,
            Optional.of(underline),
            strikethrough,
            fontSize,
            fontFamily,
            textColor,
            backgroundColor
        )
    }

    fun updateStrikethrough(strikethrough: Boolean): TextStyle {
        return TextStyle(
            bold,
            italic,
            underline,
            Optional.of(strikethrough),
            fontSize,
            fontFamily,
            textColor,
            backgroundColor
        )
    }

    fun updateFontSize(fontSize: Int): TextStyle {
        return TextStyle(
            bold,
            italic,
            underline,
            strikethrough,
            Optional.of(fontSize),
            fontFamily,
            textColor,
            backgroundColor
        )
    }

    fun updateFontFamily(fontFamily: String): TextStyle {
        return TextStyle(
            bold,
            italic,
            underline,
            strikethrough,
            fontSize, Optional.of(fontFamily),
            textColor,
            backgroundColor
        )
    }

    fun updateTextColor(textColor: Color): TextStyle {
        return TextStyle(
            bold,
            italic,
            underline,
            strikethrough,
            fontSize,
            fontFamily, Optional.of(textColor),
            backgroundColor
        )
    }

    fun updateBackgroundColor(backgroundColor: Color): TextStyle {
        return TextStyle(
            bold,
            italic,
            underline,
            strikethrough,
            fontSize,
            fontFamily,
            textColor,
            Optional.of(backgroundColor)
        )
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
                os.writeInt(encodeOptionalUint(s.fontSize))
                OPT_STRING_CODEC.encode(os, s.fontFamily)
                OPT_COLOR_CODEC.encode(os, s.textColor)
                OPT_COLOR_CODEC.encode(os, s.backgroundColor)
            }

            @Throws(IOException::class)
            override fun decode(`is`: DataInputStream): TextStyle {
                val bius = `is`.readByte()
                val fontSize = decodeOptionalUint(`is`.readInt())
                val fontFamily = OPT_STRING_CODEC.decode(`is`)
                val textColor = OPT_COLOR_CODEC.decode(`is`)
                val bgrColor = OPT_COLOR_CODEC.decode(`is`)
                return TextStyle(
                    bold(bius), italic(bius), underline(bius), strikethrough(bius),
                    fontSize, fontFamily, textColor, bgrColor
                )
            }

            private fun encodeBoldItalicUnderlineStrikethrough(s: TextStyle): Int {
                return encodeOptionalBoolean(s.bold) shl 6 or (
                        encodeOptionalBoolean(s.italic) shl 4) or (
                        encodeOptionalBoolean(s.underline) shl 2) or
                        encodeOptionalBoolean(s.strikethrough)
            }

            @Throws(IOException::class)
            private fun bold(bius: Byte): Optional<Boolean> {
                return decodeOptionalBoolean(bius.toInt() shr 6 and 3)
            }

            @Throws(IOException::class)
            private fun italic(bius: Byte): Optional<Boolean> {
                return decodeOptionalBoolean(bius.toInt() shr 4 and 3)
            }

            @Throws(IOException::class)
            private fun underline(bius: Byte): Optional<Boolean> {
                return decodeOptionalBoolean(bius.toInt() shr 2 and 3)
            }

            @Throws(IOException::class)
            private fun strikethrough(bius: Byte): Optional<Boolean> {
                return decodeOptionalBoolean(bius.toInt() shr 0 and 3)
            }

            private fun encodeOptionalBoolean(ob: Optional<Boolean>): Int {
                return ob.map { b: Boolean -> 2 + if (b) 1 else 0 }.orElse(0)
            }

            @Throws(IOException::class)
            private fun decodeOptionalBoolean(i: Int): Optional<Boolean> {
                when (i) {
                    0 -> return Optional.empty()
                    2 -> return Optional.of(false)
                    3 -> return Optional.of(true)
                }
                throw MalformedInputException(0)
            }

            private fun encodeOptionalUint(oi: Optional<Int>): Int {
                return oi.orElse(-1)
            }

            private fun decodeOptionalUint(i: Int): Optional<Int> {
                return if (i < 0) Optional.empty() else Optional.of(i)
            }
        }

        fun bold(bold: Boolean): TextStyle {
            return EMPTY.updateBold(bold)
        }

        fun italic(italic: Boolean): TextStyle {
            return EMPTY.updateItalic(italic)
        }

        fun underline(underline: Boolean): TextStyle {
            return EMPTY.updateUnderline(underline)
        }

        fun strikethrough(strikethrough: Boolean): TextStyle {
            return EMPTY.updateStrikethrough(strikethrough)
        }

        fun fontSize(fontSize: Int): TextStyle {
            return EMPTY.updateFontSize(fontSize)
        }

        fun fontFamily(family: String): TextStyle {
            return EMPTY.updateFontFamily(family)
        }

        fun textColor(color: Color): TextStyle {
            return EMPTY.updateTextColor(color)
        }

        fun backgroundColor(color: Color): TextStyle {
            return EMPTY.updateBackgroundColor(color)
        }

        fun cssColor(color: Color): String {
            val red = (color.red * 255).toInt()
            val green = (color.green * 255).toInt()
            val blue = (color.blue * 255).toInt()
            return "rgb($red, $green, $blue)"
        }
    }
}
