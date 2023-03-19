package array.gui.reporting.edit

import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment
import org.fxmisc.richtext.model.Codec
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.*

class ParStyle private constructor(
    val alignment: Optional<TextAlignment> = Optional.empty(),
    val backgroundColor: Optional<Color> = Optional.empty(),
    val indent: Optional<Indent> = Optional.empty(),
    val foldCount: Int = 0
) {
    override fun hashCode() = Objects.hash(alignment, backgroundColor, indent, foldCount)

    override fun equals(other: Any?): Boolean {
        return if (other is ParStyle) {
            alignment == other.alignment && backgroundColor == other.backgroundColor && indent == other.indent && foldCount == other.foldCount
        } else {
            false
        }
    }

    override fun toString() = toCss()

    fun toCss(): String {
        val sb = StringBuilder()
        alignment.ifPresent { al: TextAlignment ->
            val cssAlignment: String = when (al) {
                TextAlignment.LEFT -> "left"
                TextAlignment.CENTER -> "center"
                TextAlignment.RIGHT -> "right"
                TextAlignment.JUSTIFY -> "justify"
            }
            sb.append("-fx-text-alignment: $cssAlignment;")
        }
        backgroundColor.ifPresent { color: Color ->
            sb.append("-fx-background-color: ${TextStyle.cssColor(color)};")
        }
        if (foldCount > 0) sb.append("visibility: collapse;")
        return sb.toString()
    }

    fun updateWith(mixin: ParStyle): ParStyle {
        return ParStyle(
            if (mixin.alignment.isPresent) mixin.alignment else alignment,
            if (mixin.backgroundColor.isPresent) mixin.backgroundColor else backgroundColor,
            if (mixin.indent.isPresent) mixin.indent else indent,
            mixin.foldCount + foldCount)
    }

    fun updateAlignment(alignment: TextAlignment): ParStyle {
        return ParStyle(Optional.of(alignment), backgroundColor, indent, foldCount)
    }

    fun updateBackgroundColor(backgroundColor: Color): ParStyle {
        return ParStyle(alignment, Optional.of(backgroundColor), indent, foldCount)
    }

    fun updateIndent(indent: Indent?): ParStyle {
        return ParStyle(alignment, backgroundColor, Optional.ofNullable(indent), foldCount)
    }

    fun increaseIndent(): ParStyle {
        return updateIndent(
            indent.map { obj: Indent -> obj.increase() }.orElseGet { Indent() })
    }

    fun decreaseIndent(): ParStyle {
        return updateIndent(
            indent
                .filter { indent: Indent -> indent.level > 1 }
                .map { obj: Indent -> obj.decrease() }
                .orElse(null))
    }

    fun getIndent() = indent.get()

    val isIndented get() = indent.map { indent: Indent -> indent.level > 0 }.orElse(false)

    fun updateFold(fold: Boolean): ParStyle {
        val foldLevels = if (fold) foldCount + 1 else Math.max(0, foldCount - 1)
        return ParStyle(alignment, backgroundColor, indent, foldLevels)
    }

    val isFolded get() = foldCount > 0

    companion object {
        val EMPTY = ParStyle()
        val CODEC: Codec<ParStyle> = object : Codec<ParStyle> {
            private val OPT_ALIGNMENT_CODEC = Codec.optionalCodec(
                Codec.enumCodec(
                    TextAlignment::class.java))
            private val OPT_COLOR_CODEC = Codec.optionalCodec(Codec.COLOR_CODEC)
            override fun getName(): String {
                return "par-style"
            }

            @Throws(IOException::class)
            override fun encode(os: DataOutputStream, t: ParStyle) {
                OPT_ALIGNMENT_CODEC.encode(os, t.alignment)
                OPT_COLOR_CODEC.encode(os, t.backgroundColor)
                os.writeInt(t.indent.map { i: Indent -> Integer.valueOf(i.level) }.orElse(0))
                os.writeInt(t.foldCount)
            }

            @Throws(IOException::class)
            override fun decode(inStream: DataInputStream): ParStyle {
                return ParStyle(
                    OPT_ALIGNMENT_CODEC.decode(inStream),
                    OPT_COLOR_CODEC.decode(inStream),
                    Optional.of(Indent(inStream.readInt())),
                    inStream.readInt())
            }
        }

        fun alignLeft(): ParStyle {
            return EMPTY.updateAlignment(TextAlignment.LEFT)
        }

        fun alignCenter(): ParStyle {
            return EMPTY.updateAlignment(TextAlignment.CENTER)
        }

        fun alignRight(): ParStyle {
            return EMPTY.updateAlignment(TextAlignment.RIGHT)
        }

        fun alignJustify(): ParStyle {
            return EMPTY.updateAlignment(TextAlignment.JUSTIFY)
        }

        fun backgroundColor(color: Color): ParStyle {
            return EMPTY.updateBackgroundColor(color)
        }

        fun folded(): ParStyle {
            return EMPTY.updateFold(java.lang.Boolean.TRUE)
        }

        fun unfolded(): ParStyle {
            return EMPTY.updateFold(java.lang.Boolean.FALSE)
        }
    }
}
