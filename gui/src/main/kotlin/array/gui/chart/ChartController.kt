package array.gui.chart

import javafx.collections.FXCollections
import javafx.scene.Scene
import javafx.scene.chart.*
import javafx.scene.layout.BorderPane
import javafx.stage.Stage

interface ChartType {
    fun make(): XYChart<String, Number>
}

object LineChartType : ChartType {
    override fun make() = LineChart(CategoryAxis(), NumberAxis())
}

object BarChartType : ChartType {
    override fun make() = BarChart(CategoryAxis(), NumberAxis())
}

class ChartController(val lineChart: XYChart<String, Number>) {
    val borderPane = BorderPane(lineChart)

    fun updateData(labels: Array<String>, datasets: Array<DatasetDouble>) {
        val list = FXCollections.observableArrayList<XYChart.Series<String, Number>>()
        datasets.forEach { dataset ->
            val series = FXCollections.observableArrayList<XYChart.Data<String, Number>>()
            dataset.data.forEachIndexed { i, v ->
                series.add(XYChart.Data(labels[i], v))
            }
            list.add(XYChart.Series(dataset.name, series))
        }
        lineChart.data = list
    }

    companion object {
        private fun make(type: ChartType): ChartController {
            val lineChart = type.make()
            return ChartController(lineChart)
        }

        fun openWindowWithData(type: ChartType, labels: Array<String>, datasets: Array<DatasetDouble>) {
            val controller = make(type)
            controller.updateData(labels, datasets)
            val stage = Stage()
            stage.title = "Graph"
            stage.scene = Scene(controller.borderPane)
            stage.show()
        }
    }
}
