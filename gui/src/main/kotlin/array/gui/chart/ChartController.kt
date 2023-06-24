package array.gui.chart

import javafx.collections.FXCollections
import javafx.scene.Scene
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.layout.BorderPane
import javafx.stage.Stage

class ChartController(val lineChart: LineChart<Number, Number>) {
    val borderPane = BorderPane(lineChart)

    fun updateData(labels: Array<String>, datasets: Array<DatasetDouble>) {
        val list = FXCollections.observableArrayList<XYChart.Series<Number, Number>>()
        datasets.forEach { dataset ->
            val series = FXCollections.observableArrayList<XYChart.Data<Number, Number>>()
            var i = 0L
            dataset.data.forEach { v ->
                series.add(XYChart.Data(i, v))
                i++
            }
            list.add(XYChart.Series(dataset.name, series))
        }
        lineChart.data = list
    }

    companion object {
        private fun make(): ChartController {
            val lineChart = LineChart(NumberAxis(), NumberAxis())
            return ChartController(lineChart)
        }

        fun openWindowWithData(labels: Array<String>, datasets: Array<DatasetDouble>) {
            val controller = make()
            controller.updateData(labels, datasets)
            val stage = Stage()
            stage.title = "Graph"
            stage.scene = Scene(controller.borderPane)
            stage.show()
        }
    }
}
