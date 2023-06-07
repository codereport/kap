package array.csv

import array.*

class CsvParseException(msg: String, val line: Int, val col: Int) : Exception("Error at ${line + 1}:${col + 1}: $msg")

fun readCsv(source: CharacterProvider): APLValue {
    val rows = readRows(source)

    if (rows.isEmpty()) {
        return APLArrayImpl.make(dimensionsOfSize(0, 0)) {
            throw Exception("Attempt to read a value when initialising empty array")
        }
    }

    val width = rows.maxValueBy { it.size }
    return APLArrayImpl.make(dimensionsOfSize(rows.size, width)) { index ->
        val rowIndex = index / width
        val colIndex = index % width
        val row = rows[rowIndex]
        if (colIndex >= row.size) {
            APLNullValue.APL_NULL_INSTANCE
        } else {
            row[colIndex]
        }
    }
}

private fun readRows(source: CharacterProvider): List<List<APLValue>> {
    val rows = ArrayList<List<APLValue>>()
    var lineNumber = 0
    while (true) {
        var line = source.nextLine() ?: break
        val currLineNumber = lineNumber++
        val fields = ArrayList<APLValue>()
        var pos = 0

        fun atEol() = pos >= line.length

        fun skipWhitespace() {
            while (!atEol() && line[pos].isWhitespace()) {
                pos++
            }
        }

        fun readQuotedField(): String {
            val buf2 = StringBuilder()
            loop@ while (true) {
                if (atEol()) {
                    line = source.nextLine() ?: throw CsvParseException(
                        "End of file in the middle of string",
                        currLineNumber,
                        pos)
                    pos = 0
                    buf2.append("\n")
                }
                when (val ch = line[pos++]) {
                    '\"' -> break@loop
                    '\\' -> {
                        if (atEol()) {
                            throw CsvParseException("Unterminated string", currLineNumber, pos)
                        } else {
                            buf2.append(line[pos++])
                        }
                    }
                    else -> buf2.append(ch)
                }
            }
            return buf2.toString()
        }

        fun readUnquotedField(initial: Char): String {
            val buf = StringBuilder()
            buf.append(initial)
            var heldWhitespace = StringBuilder()
            loop@ while (!atEol()) {
                val ch = line[pos]
                when {
                    ch.isWhitespace() -> heldWhitespace.append(ch)
                    ch == ',' -> break@loop
                    else -> {
                        buf.append(heldWhitespace)
                        heldWhitespace = StringBuilder()
                        buf.append(ch)
                    }
                }
                pos++
            }
            return buf.toString()
        }
        skipWhitespace()
        if (!atEol()) {
            while (true) {
                skipWhitespace()
                if (atEol()) break
                val field = when (val ch = line[pos++]) {
                    '"' -> APLString.make(readQuotedField())
                    ',' -> {
                        pos--
                        APLString.make("")
                    }
                    else -> stringToAplValue(readUnquotedField(ch))
                }
                fields.add(field)
                // At this point the next character must be either a comma or we're at the end of the line
                skipWhitespace()
                if (!atEol()) {
                    val ch2 = line[pos++]
                    if (ch2 != ',') {
                        throw CsvParseException("Syntax error in CSV file", currLineNumber, pos)
                    }
                }
            }
            rows.add(fields)
        }
    }
    return rows
}

private val PATTERN_INTEGER = "^-?[0-9]+$".toRegex()
private val PATTERN_FLOAT1 = "^-?[0-9]+\\.[0-9]*$".toRegex()
private val PATTERN_FLOAT2 = "^-?[0-9]*\\.[0-9]+$".toRegex()

fun stringToAplValue(string: String): APLValue {
    return when {
        PATTERN_INTEGER.matches(string) -> string.toLong().makeAPLNumber()
        PATTERN_FLOAT1.matches(string) -> string.toDouble().makeAPLNumber()
        PATTERN_FLOAT2.matches(string) -> string.toDouble().makeAPLNumber()
        else -> APLString.make(string)
    }
}
