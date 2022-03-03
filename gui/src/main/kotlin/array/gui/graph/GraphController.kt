package array.gui.graph

import array.*
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.chart.LineChart
import javafx.scene.chart.XYChart
import javafx.scene.layout.BorderPane
import javafx.stage.Stage

class GraphController {
    lateinit var borderPane: BorderPane
    lateinit var lineChart: LineChart<Number, Number>

    fun updateData(data: ObservableList<XYChart.Series<Number, Number>>) {
        lineChart.data = data
    }

    companion object {
        fun make(): GraphController {
            val loader = FXMLLoader(GraphController::class.java.getResource("graph-window.fxml"))
            loader.load<GraphController>()
            return loader.getController()
        }

        fun makeSeriesList(value: APLValue, pos: Position): ObservableList<XYChart.Series<Number, Number>> {
            val d = value.dimensions

            fun checkAxis(vararg n: Int) {
                for (i in n) {
                    if (d[i] == 0) {
                        throwAPLException(InvalidDimensionsException("Axis ${i} is size 0", pos))
                    }
                }
            }

            if (d.lastDimension(pos) != 2) {
                throwAPLException(InvalidDimensionsException("minor axis must be size 2, was: ${d}", pos))
            }

            return when (d.size) {
                2 -> {
                    checkAxis(0)
                    makeSeries(value, 1, d[0], pos)
                }
                3 -> {
                    checkAxis(0, 1)
                    makeSeries(value, d[0], d[1], pos)
                }
                else -> throwAPLException(InvalidDimensionsException("value must be a 2 or 3 dimensional array, was: ${d}", pos))
            }
        }

        private fun makeSeries(value: APLValue, numLists: Int, n: Int, pos: Position): ObservableList<XYChart.Series<Number, Number>> {
            val result = FXCollections.observableArrayList<XYChart.Series<Number, Number>>()
            var p = 0
            var currList: ObservableList<XYChart.Data<Number, Number>>? = null
            value.membersSequence().chunked(2).forEach { (x, y) ->
                if (p == 0) {
                    currList = FXCollections.observableArrayList()
                }
                val xDouble = x.ensureNumber(pos).asDouble(pos)
                val yDouble = y.ensureNumber(pos).asDouble(pos)
                currList!!.add(XYChart.Data(xDouble, yDouble))
                if (++p >= n) {
                    p = 0
                    result.add(XYChart.Series(currList).apply { name = "foo" })
                    assertx(result.size < numLists)
                }
            }
            assertx(p == 0)

            return result
        }

        fun openWindowWithData(data: ObservableList<XYChart.Series<Number, Number>>) {
            val controller = make()
            controller.updateData(data)
            val stage = Stage()
            stage.title = "Graph"
            stage.scene = Scene(controller.borderPane)
            stage.show()
        }
    }
}
