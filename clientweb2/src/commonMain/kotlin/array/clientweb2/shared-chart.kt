package array.clientweb2

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("chart")
class ChartOutput(val content: ChartDescriptor) : AdditionalOutputData()

@Serializable
sealed class ChartDescriptor

@Serializable
@SerialName("line")
class LineChartDescriptor(val horizontalAxisLabels: Array<String>, val data: Array<DatasetDouble>) : ChartDescriptor()

@Serializable
class DatasetDouble(val name: String, val data: DoubleArray)
