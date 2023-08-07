package com.dhsdevelopments.kap.gui2.arrayeditor

import array.*
import array.builtins.ResizedArrayImpls
import java.awt.Component
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

class ArrayEditor(value: APLValue) : JTable() {
    private val arrayEditorTableModel get() = model as ArrayEditorTableModel

    init {
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

class APLValueRenderer : DefaultTableCellRenderer() {
    private var defaultLabel: JLabel? = null

    override fun getTableCellRendererComponent(table: JTable?, value: Any, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        assertx(value is APLValue)
        val component = when {
            value is APLNumber -> makeAPLNumberRenderer(value)
            value.isStringValue() -> makeAPLStringRenderer(value.toStringValue())
            !value.isScalar() -> makeAPLArrayRenderer(value)
            else -> makeFormattedOutputRenderer(value)
        }
        component.isOpaque = true
        if (table != null) {
            component.foreground = if (isSelected) table.selectionForeground else table.foreground
            component.background = if (isSelected) table.selectionBackground else table.background
        }
        component.border = if (hasFocus) UIManager.getBorder("Table.focusCellHighlightBorder") else noFocusBorder
        return component
    }

    private fun makeLabel() = defaultLabel ?: JLabel().also { l -> defaultLabel = l }

    private fun makeAPLNumberRenderer(value: APLNumber): JComponent {
        return makeLabel().also { l ->
            l.text = value.formatted(FormatStyle.PLAIN)
            l.horizontalAlignment = SwingConstants.RIGHT
        }
    }

    private fun makeAPLStringRenderer(value: String): JComponent {
        return makeLabel().also { l ->
            l.text = value
            l.horizontalAlignment = SwingConstants.LEFT
        }
    }

    private fun makeAPLArrayRenderer(value: APLValue): JComponent {
        return makeLabel().also { l ->
            l.text = "array(${value.dimensions.dimensions.joinToString(", ")})"
            l.horizontalAlignment = SwingConstants.LEFT
        }
    }

    private fun makeFormattedOutputRenderer(value: APLValue): JComponent {
        return makeLabel().also { l ->
            l.text = value.formatted(FormatStyle.PLAIN)
            l.horizontalAlignment = SwingConstants.LEFT
        }
    }
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

fun main() {
    SwingUtilities.invokeLater {
        val v = ResizedArrayImpls.makeResizedArray(dimensionsOfSize(20, 10), APLLONG_1)
        openInArrayEditor(v)
    }
}
