package array.gui.reporting.edit

import array.FormatStyle
import array.gui.reporting.Formula
import array.gui.reporting.ReportingClient
import javafx.scene.control.Label
import javafx.scene.layout.VBox

class FormulaEditorElement(client: ReportingClient, formula: Formula) : VBox() {
    val label = Label()

    init {
        children.add(label)

        client.registerVariableListener(formula.name) { value ->
            label.text = value.formatted(FormatStyle.PRETTY)
        }
    }
}
