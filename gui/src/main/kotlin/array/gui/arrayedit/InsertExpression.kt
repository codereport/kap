package array.gui.arrayedit

import array.gui.styledarea.InputFieldStyledArea
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar
import javafx.scene.control.ChoiceBox

class InsertExpression {
    lateinit var styledArea: InputFieldStyledArea
    lateinit var insertStyleSelector: ChoiceBox<InsertStyleOptionHolder>
    lateinit var ok: Button
    lateinit var cancel: Button

    fun initialize() {
        insertStyleSelector.items.let { v ->
            v.add(InsertStyleOptionHolder(InsertStyleOption.RESHAPE, "Reshape"))
            v.add(InsertStyleOptionHolder(InsertStyleOption.REPLICATE, "Replicate"))
            v.add(InsertStyleOptionHolder(InsertStyleOption.REPEAT, "Repeat"))
            v.add(InsertStyleOptionHolder(InsertStyleOption.ERROR, "Match"))
        }
        insertStyleSelector.selectionModel.select(0)

        ButtonBar.setButtonData(ok, ButtonBar.ButtonData.OK_DONE)
        ButtonBar.setButtonData(cancel, ButtonBar.ButtonData.CANCEL_CLOSE)
    }

    fun updateInsertionType(insertionType: InsertStyleOption) {
        val index = insertStyleSelector.items.indexOfFirst { it.option == insertionType }
        if (index >= 0) {
            insertStyleSelector.selectionModel.select(index)
        }
    }
}

class InsertStyleOptionHolder(val option: InsertStyleOption, val title: String) {
    override fun toString() = title
}

enum class InsertStyleOption {
    ERROR,
    REPLICATE,
    RESHAPE,
    REPEAT
}
