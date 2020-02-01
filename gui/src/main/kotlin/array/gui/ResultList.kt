package array.gui

import array.APLValue
import javafx.scene.layout.VBox
import javafx.scene.text.Font

class ResultList : VBox() {
    fun addResult(context: ClientRenderContext, inputString: String, value: APLValue) {
        children.add(ResultPanel(context, inputString, value))
    }
}
