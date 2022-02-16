package array.gui.styledarea

/**
 * Holds information about the style of a paragraph.
 */
class ParStyle(val type: ParStyleType = ParStyleType.NORMAL, val tag: Any? = null) {
    override fun toString(): String {
        return "ParStyle[type=${type}, tag=${tag}]"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ParStyle

        if (type != other.type) return false
        if (tag != other.tag) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + (tag?.hashCode() ?: 0)
        return result
    }


    enum class ParStyleType {
        NORMAL,
        INDENT,
        OUTPUT
    }
}
