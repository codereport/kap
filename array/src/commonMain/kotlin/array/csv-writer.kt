package array.csv

import array.*

fun writeAPLArrayAsCsv(dest: CharacterConsumer, value: APLValue, pos: Position? = null) {
    val dimensions = value.dimensions
    if (dimensions.size != 2) {
        throwAPLException(InvalidDimensionsException("Value must be a 2-dimensional array", pos))
    }

    val width = dimensions[1]
    var x = 0
    value.iterateMembers { v ->
        val v0 = v.unwrapDeferredValue()
        if (x > 0) {
            dest.writeString(",")
        }
        when {
            v0.isStringValue() -> dest.writeString("\"${escapeString(v.toStringValue(pos))}\"")
            v0 is APLNumber -> {
                when (val n = v0.ensureNumber(pos)) {
                    is APLLong -> dest.writeString(n.value.toString())
                    is APLDouble -> dest.writeString(n.value.toString())
                    is APLComplex -> if (n.isComplex()) dest.writeString("complex") else dest.writeString(n.asDouble(pos).toString())
                    else -> dest.writeString("error")
                }
            }
            else -> dest.writeString("unknown-type")
        }
        if (++x >= width) {
            dest.writeString("\n")
            x = 0
        }
    }
    assertx(x == 0)
}

private fun escapeString(s: String): String {
    val buf = StringBuilder()
    s.forEach { ch ->
        when (ch) {
            '\"' -> buf.append("\\\"")
            '\n' -> buf.append("\\n")
            else -> buf.append(ch)
        }
    }
    return buf.toString()
}

class CsvWriter(val consumer: CharacterConsumer) {
    private var numColumns: Int? = null

    fun writeRow(values: List<String>) {
        val n = numColumns
        if (n == null) {
            numColumns = values.size
        } else if (n != values.size) {
            throw IllegalArgumentException("Attempt to add a row of ${values.size} cells. Table is expected to have ${n} columns.")
        }
        values.map { v -> maybeEscape(v) }.joinToString(",").let(consumer::writeString)
        consumer.writeString("\n")
    }

    private fun maybeEscape(s: String): String {
        val v = s.replace("\"".toRegex(), "\"\"")
        return "\"${v}\""
    }
}
