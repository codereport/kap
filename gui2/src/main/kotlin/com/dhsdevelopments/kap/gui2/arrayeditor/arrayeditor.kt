package com.dhsdevelopments.kap.gui2.arrayeditor

import array.*
import java.awt.Component
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableColumnModel
import javax.swing.table.TableCellRenderer

class ArrayEditor(value: APLValue) : JTable() {
    private val arrayEditorTableModel get() = model as ArrayEditorTableModel

    init {
        columnModel = ArrayEditorColumnModel()
        columnSelectionAllowed = true
        model = ArrayEditorTableModel(value)
        setDefaultRenderer(Object::class.java, APLValueRenderer())
    }
}

class ArrayEditorTableModel(value: APLValue) : AbstractTableModel() {
    val content = MutableAPLValue(value)

    init {
        assertx(content.dimensions.size == 2)
    }

    override fun getRowCount(): Int {
        return content.dimensions[0]
    }

    override fun getColumnCount(): Int {
        return content.dimensions[1]
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): APLValue {
        val p = content.dimensions.indexFromPosition(intArrayOf(rowIndex, columnIndex))
        return content.valueAt(p)
    }
}

class ArrayEditorColumnModel : DefaultTableColumnModel()

class APLValueRenderer : TableCellRenderer {
    override fun getTableCellRendererComponent(table: JTable, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        assertx(value is APLValue)
        return when {
            value.isStringValue() -> makeAPLStringRenderer(value.toStringValue())
            !value.isScalar() -> makeAPLArrayRenderer(value)
            else -> makeFormattedOutputRenderer(value)
        }
    }
}

private fun makeAPLStringRenderer(value: String): Component {
    return JLabel(value)
}

private fun makeAPLArrayRenderer(value: APLValue): Component {
    return JLabel("array(${value.dimensions.dimensions.joinToString(", ")})")
}

private fun makeFormattedOutputRenderer(value: APLValue): Component {
    return JLabel(value.formatted(FormatStyle.PLAIN))
}

fun openInArrayEditor(v: APLValue) {
    val frame = JFrame()
    val editor = ArrayEditor(v)
    editor.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    val scrollPane = JScrollPane(editor)
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    frame.contentPane = scrollPane
    frame.pack()
    frame.isVisible = true
}
