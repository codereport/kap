package array.gui.styledarea

/**
 * Holds information about the style of a paragraph.
 */
class ParStyle(val type: ParStyleType = ParStyleType.NORMAL, val tag: Any? = null) {
    override fun toString(): String {
        return "ParStyle[type=${type}, tag=${tag}]"
    }

    enum class ParStyleType {
        NORMAL,
        INDENT,
        OUTPUT
    }
}
