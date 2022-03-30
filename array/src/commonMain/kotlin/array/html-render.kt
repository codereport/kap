package array

fun escapeHtml(s: String, buf: Appendable) {
    s.forEach { ch ->
        val escapedChar = when (ch) {
            '&' -> "&amp;"
            '<' -> "&lt;"
            '>' -> "&gt;"
            else -> ch.toString()
        }
        buf.append(escapedChar)
    }
}

inline fun encloseInTag(buf: Appendable, tag: String, vararg attrs: Pair<String, String>, fn: () -> Unit) {
    buf.append("<")
    buf.append(tag)
    attrs.forEach { a ->
        buf.append(" ")
        buf.append(a.first)
        buf.append("=\"")
        buf.append(a.second)
        buf.append("\"")
    }
    buf.append(">")
    fn()
    buf.append("</")
    buf.append(tag)
    buf.append(">")
}

fun array2DAsHtml(value: APLValue, buf: Appendable) {
    val borderStyle = "border: 1px solid; border-collapse: collapse;"
    val colHeaderStyle = "${borderStyle} text-align: center;"
    val tdStyle = "${borderStyle} text-align: right;"
    val d = value.dimensions
    val labelsCopy = value.labels
    encloseInTag(buf, "table", "style" to borderStyle) {
        val rowLabels = labelsCopy?.labels?.get(0)
        labelsCopy?.labels?.get(1)?.let { colLabels ->
            encloseInTag(buf, "thead") {
                encloseInTag(buf, "tr") {
                    if (rowLabels != null) {
                        buf.append("<th style=\"${borderStyle}\"></th>")
                    }
                    colLabels.forEach { header ->
                        if (header != null) {
                            encloseInTag(buf, "th", "style" to colHeaderStyle) {
                                escapeHtml(header.title, buf)
                            }
                        } else {
                            buf.append("<th style=\"${colHeaderStyle}\"></th>")
                        }
                    }
                }
            }
        }
        encloseInTag(buf, "tbody") {
            repeat(d[0]) { rowIndex ->
                encloseInTag(buf, "tr") {
                    if (rowLabels != null) {
                        val header = rowLabels[rowIndex]?.title
                        if (header != null) {
                            encloseInTag(buf, "td", "style" to borderStyle) {
                                escapeHtml(header, buf)
                            }
                        } else {
                            buf.append("<td style=\"${borderStyle}\"></td>")
                        }
                    }
                    repeat(d[1]) { colIndex ->
                        encloseInTag(buf, "td", "style" to tdStyle) {
                            val v = value.valueAt(rowIndex * d[1] + colIndex)
                            v.asHtml(buf)
                        }
                    }
                }
            }
        }
    }
}
