package array.gui.chart

import array.*
import javafx.application.Platform

class DatasetDouble(val name: String, val data: DoubleArray)

abstract class GenericLineChartFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
    override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
        val a0 = a.collapse()
        val labels = a0.labels
        val d = a0.dimensions
        val (horizontalAxisLabels, datasets) = when (d.size) {
            1 -> Pair(
                Array(d[0]) { i -> labels?.labels?.get(0)?.get(i)?.title ?: i.toString() },
                arrayOf(DatasetDouble("unnamed", DoubleArray(d[0]) { i -> a0.valueAtDouble(i, pos) })))
            2 -> {
                val axis0Labels = labels?.labels?.get(0)
                val axis1Labels = labels?.labels?.get(1)
                val labelNames = Array(d[1]) { i -> axis1Labels?.get(i)?.title ?: i.toString() }
                val data = Array(d[0]) { i ->
                    val offset = i * d[1]
                    val graphLabel = axis0Labels?.get(i)?.title ?: i.toString()
                    DatasetDouble(graphLabel, DoubleArray(d[1]) { i2 -> a0.valueAtDouble(offset + i2, pos) })
                }
                Pair(labelNames, data)
            }
            else -> throwAPLException(InvalidDimensionsException("chart data must be 1- or 2-dimensional", pos))
        }
        Platform.runLater {
            ChartController.openWindowWithData(type, horizontalAxisLabels, datasets)
        }
        return a0
    }

    abstract val type: ChartType
}

class LineChartFunction : APLFunctionDescriptor {
    class LineChartFunctionImpl(instantiation: FunctionInstantiation) : GenericLineChartFunctionImpl(instantiation) {
        override val type: ChartType get() = LineCharType
    }

    override fun make(instantiation: FunctionInstantiation) = LineChartFunctionImpl(instantiation)
}

class BarChartFunction : APLFunctionDescriptor {
    class LineChartFunctionImpl(instantiation: FunctionInstantiation) : GenericLineChartFunctionImpl(instantiation) {
        override val type: ChartType get() = BarChartType
    }

    override fun make(instantiation: FunctionInstantiation) = LineChartFunctionImpl(instantiation)
}

class JavaChartModule : KapModule {
    override val name get() = "javachart"

    override fun init(engine: Engine) {
        val ns = engine.makeNamespace("chart")
        engine.registerFunction(ns.internAndExport("line"), LineChartFunction())
        engine.registerFunction(ns.internAndExport("bar"), BarChartFunction())
    }
}
