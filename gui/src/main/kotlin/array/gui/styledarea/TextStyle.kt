package array.gui.styledarea

import array.gui.ClientRenderContext
import org.fxmisc.richtext.TextExt

open class TextStyle(val type: Type = Type.DEFAULT, val promptTag: Boolean = false) {
    fun styleContent(content: TextExt, renderContext: ClientRenderContext) {
        val css = when (type) {
            Type.ERROR -> "editcontent-error"
            Type.PROMPT -> "editcontent-prompt"
            Type.OUTPUT -> "editcontent-output"
            Type.RESULT -> "editcontent-result"
            else -> null
        }
        if (css != null) {
            content.styleClass.add(css)
        }
//        content.font = renderContext.font()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextStyle

        if (type != other.type) return false
        if (promptTag != other.promptTag) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + promptTag.hashCode()
        return result
    }

    override fun toString(): String {
        return "TextStyle(type=$type, promptTag=$promptTag)"
    }

    enum class Type {
        DEFAULT,
        PROMPT,
        INPUT,
        LOG_INPUT,
        OUTPUT,
        RESULT,
        ERROR
    }
}
