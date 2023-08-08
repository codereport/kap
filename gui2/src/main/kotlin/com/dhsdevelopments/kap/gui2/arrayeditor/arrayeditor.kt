package com.dhsdevelopments.kap.gui2.arrayeditor

import array.*
import array.builtins.ResizedArrayImpls
import java.awt.Component
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellEditor

class ArrayEditor(value: APLValue) : JTable() {
    private val arrayEditorTableModel get() = model as ArrayEditorTableModel

    init {
        columnSelectionAllowed = true
        model = ArrayEditorTableModel(value)
        setDefaultRenderer(Object::class.java, APLValueRenderer())
        cellEditor = APLValueCellEditor()
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

    override fun isCellEditable(rowIndex: Int, columnIndex: Int) = true
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

    private fun makeLabel(text: String? = "", horizontalAlignment: Int = SwingConstants.LEFT): JLabel {
        val label = defaultLabel ?: JLabel().also { l ->
            defaultLabel = l
        }
        label.text = text
        label.horizontalAlignment = horizontalAlignment
        return label
    }

    private fun makeAPLNumberRenderer(value: APLNumber) =
        makeLabel(text = value.formatted(FormatStyle.PLAIN), horizontalAlignment = SwingConstants.RIGHT)

    private fun makeAPLStringRenderer(value: String) =
        makeLabel(text = value)

    private fun makeAPLArrayRenderer(value: APLValue) =
        makeLabel(text = "array(${value.dimensions.dimensions.joinToString(", ")})")

    private fun makeFormattedOutputRenderer(value: APLValue) =
        makeLabel(text = value.formatted(FormatStyle.PLAIN))
}

fun openInArrayEditor(v: APLValue) {
    val frame = JFrame()
    val editor = ArrayEditor(v)
    editor.autoResizeMode = JTable.AUTO_RESIZE_OFF
    val scrollPane = JScrollPane(editor)
    scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
    scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
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

class APLValueCellEditor : AbstractCellEditor(), TableCellEditor {
    private val textField by lazy { JTextField(30) }

    override fun getCellEditorValue(): Any {
        return APLLONG_1
    }

    override fun getTableCellEditorComponent(table: JTable?, value: Any?, isSelected: Boolean, row: Int, column: Int): Component {
        return textField
    }
}
