package array.gui.styledarea

import array.APLValue
import javafx.scene.paint.Color
import org.fxmisc.richtext.TextExt

class TextStyle(
    val type: Type = Type.DEFAULT,
    val promptTag: Boolean = false,
    @Suppress("unused") val contentTag: Any? = null,
    val supplementaryValue: MutableSupplementaryValue? = null
) {
    @Suppress("UNUSED_PARAMETER")
    fun styleContent(content: TextExt) {
        val css = when (type) {
            Type.LOG_INPUT -> "editcontent-loginput"
            Type.ERROR -> "editcontent-error"
            Type.PROMPT -> "editcontent-prompt"
            Type.OUTPUT -> "editcontent-output"
            Type.RESULT -> "editcontent-result"
            Type.SINGLE_CHAR_HIGHLIGHT -> "editcontent-warninghighlight"
            else -> null
        }
        if (css != null) {
            content.styleClass.add(css)
        }
        if (type == Type.SINGLE_CHAR_HIGHLIGHT) {
            content.underlineWidth = 1.0
            content.underlineColor = Color.RED
        }
//        content.font = renderContext.font()
    }


    override fun toString(): String {
        return "TextStyle(type=$type, promptTag=$promptTag)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextStyle

        if (type != other.type) return false
        if (promptTag != other.promptTag) return false
        if (contentTag != other.contentTag) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + promptTag.hashCode()
        result = 31 * result + (contentTag?.hashCode() ?: 0)
        return result
    }

    enum class Type {
        DEFAULT,
        PROMPT,
        INPUT,
        LOG_INPUT,
        OUTPUT,
        RESULT,
        ERROR,
        SINGLE_CHAR_HIGHLIGHT
    }

    class MutableSupplementaryValue(var value: APLValue?)
}
