package array.clientweb2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("chart")
class ChartOutput(val content: ChartDescriptor) : AdditionalOutputData()

@Serializable
sealed class ChartDescriptor

enum class LineChartSubtype {
    LINE,
    BAR,
    DOUGHNUT,
    PIE
}

@Serializable
@SerialName("line")
class LineChartDescriptor(val subtype: LineChartSubtype, val horizontalAxisLabels: Array<String>, val data: Array<DatasetDouble>) : ChartDescriptor()

@Serializable
class DatasetDouble(val name: String, val data: DoubleArray)
