package array.gui.reporting.edit


class Indent {
    var width = 15.0
    var level = 1

    constructor()

    constructor(level: Int) {
        if (level > 0) {
            this.level = level
        }
    }

    fun increase(): Indent {
        return Indent(level + 1)
    }

    fun decrease(): Indent {
        return Indent(level - 1)
    }

    override fun toString(): String {
        return "indent: ${level}"
    }
}
