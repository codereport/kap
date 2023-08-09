package com.dhsdevelopments.kap.gui2.arrayeditor

import array.*
import array.builtins.IotaArrayImpls
import array.builtins.ResizedArrayImpls
import com.dhsdevelopments.kap.gui2.enableKapKeyboard
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.event.*
import javax.swing.table.*

class ArrayEditor(value: APLValue) : JTable(ArrayEditorTableModel(value), ArrayEditorColumnModel()) {
    private val arrayEditorTableModel get() = model as ArrayEditorTableModel

    init {
        columnModel.addColumnModelListener(ArrayEditorColumnListener(arrayEditorTableModel, columnModel))
        autoCreateColumnsFromModel = true
        createDefaultColumnsFromModel()
        columnSelectionAllowed = true
        setDefaultRenderer(Object::class.java, APLValueRenderer())
    }

    fun editedContent() = arrayEditorTableModel.content.makeAPLArray()
}

class ArrayEditorColumnListener(val model: ArrayEditorTableModel, val columnModel: TableColumnModel) : TableColumnModelListener {
    override fun columnAdded(e: TableColumnModelEvent) {
        val colIndex = e.toIndex
        val col = columnModel.getColumn(colIndex)
        col.headerValue = model.content.labels?.labels?.get(0)?.get(colIndex)?.title ?: colIndex.toString()
        col.cellEditor = APLValueCellEditor()
    }

    override fun columnRemoved(e: TableColumnModelEvent) {}
    override fun columnMoved(e: TableColumnModelEvent) {}
    override fun columnMarginChanged(e: ChangeEvent) {}
    override fun columnSelectionChanged(e: ListSelectionEvent) {}
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

    override fun getValueAt(row: Int, col: Int): APLValue {
        val p = content.dimensions.indexFromPosition(intArrayOf(row, col))
        return content.valueAt(p)
    }

    override fun setValueAt(value: Any?, row: Int, col: Int) {
        assertx(value is APLValue)
        content.updateValueAt(content.dimensions.indexFromPosition(intArrayOf(row, col)), value)
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int) = true
}

class ArrayEditorColumnModel : DefaultTableColumnModel() {

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

private var arrayEditorIndex = 0
private val activeEdtiors = HashMap<Int, ArrayEditor>()

fun openInArrayEditor(v: APLValue) {
    val frame = JFrame()

    val panel = JPanel(BorderLayout())

    val index = arrayEditorIndex++
    panel.add(JLabel("Index: ${index}"), BorderLayout.NORTH)

    val editor = ArrayEditor(v)
    activeEdtiors[index] = editor
    editor.autoResizeMode = JTable.AUTO_RESIZE_OFF
    val scrollPane = JScrollPane(editor)
    scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
    scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
    panel.add(scrollPane, BorderLayout.CENTER)

    frame.contentPane = panel
    frame.pack()
    frame.isVisible = true
}

fun valueFromIndex(index: Int) = activeEdtiors[index]?.editedContent()

fun main() {
    SwingUtilities.invokeLater {
        val v = ResizedArrayImpls.makeResizedArray(dimensionsOfSize(20, 10), IotaArrayImpls.IotaArrayLong(20 * 10))
        openInArrayEditor(v)
    }
}

class APLValueCellEditor : AbstractCellEditor(), TableCellEditor {
    private val tokeniserEngine = Engine()
    private var currentValue: APLValue? = null

    private val textField by lazy {
        JTextField().also { f ->
            f.enableKapKeyboard()
//            f.addKeyListener(object : KeyAdapter() {
//                override fun keyTyped(event: KeyEvent) {
//                    if (event.keyChar == '\n' &&
//                        (event.modifiersEx and
//                                (InputEvent.SHIFT_DOWN_MASK or InputEvent.ALT_DOWN_MASK or InputEvent.CTRL_DOWN_MASK)) == 0) {
//                        stopCellEditing()
//                    }
//                }
//            })
            f.addActionListener {
                stopCellEditing()
            }
            f.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) {
                    reparseContent()
                }

                override fun removeUpdate(e: DocumentEvent?) {
                    reparseContent()
                }

                override fun changedUpdate(e: DocumentEvent?) {
                    reparseContent()
                }
            })
        }
    }

    private fun reparseContent() {
        val tokeniser = TokenGenerator(tokeniserEngine, StringSourceLocation(textField.text))
        try {
            val token = tokeniser.nextToken()
            currentValue = if (token == EndOfFile) {
                APLLONG_0
            } else {
                val endToken = tokeniser.nextToken()
                if (endToken != EndOfFile) {
                    null
                } else {
                    when (token) {
                        is ConstantToken -> token.parsedValue()
                        else -> null
                    }
                }
            }
        } catch (e: ParseException) {
            currentValue = null
        }
    }

    override fun getCellEditorValue(): Any {
        val v = currentValue
        assertx(v != null)
        return v
    }

    override fun stopCellEditing(): Boolean {
        if (currentValue != null) {
            return super.stopCellEditing()
        }
        return false
    }

    override fun isCellEditable(event: EventObject): Boolean {
        return if (event is MouseEvent) {
            event.getClickCount() >= 2
        } else {
            true
        }
    }

    override fun getTableCellEditorComponent(table: JTable?, value: Any?, isSelected: Boolean, row: Int, column: Int): Component {
        assertx(value is APLValue)
        textField.text = value.formatted(FormatStyle.READABLE)
        currentValue = value
        return textField
    }
}
