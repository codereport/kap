package array.clientweb2

import array.*

class JsChartModule(val sendMessageFn: (ResponseMessage) -> Unit) : KapModule {
    override val name: String get() = "jschart"

    override fun init(engine: Engine) {
        val ns = engine.makeNamespace("chart")
        engine.registerFunction(engine.internSymbol("line", ns), LineChartFunction())
        engine.registerFunction(engine.internSymbol("bar", ns), BarChartFunction())
        engine.registerFunction(engine.internSymbol("doughnut", ns), DoughnutChartFunction())
        engine.registerFunction(engine.internSymbol("pie", ns), PieChartFunction())
    }
}

abstract class GenericLineChartFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
    override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
        val labels = a.labels
        val d = a.dimensions
        val (horizontalAxisLabels, datasets) = when (d.size) {
            1 -> Pair(
                Array(d[0]) { i -> labels?.labels?.get(0)?.get(i)?.title ?: i.toString() },
                arrayOf(DatasetDouble("unnamed", DoubleArray(d[0]) { i -> a.valueAtDouble(i, pos) })))
            2 -> {
                val axis0Labels = labels?.labels?.get(0)
                val axis1Labels = labels?.labels?.get(1)
                val labelNames = Array(d[1]) { i -> axis1Labels?.get(i)?.title ?: i.toString() }
                val data = Array(d[0]) { i ->
                    val offset = i * d[1]
                    val graphLabel = axis0Labels?.get(i)?.title ?: i.toString()
                    DatasetDouble(graphLabel, DoubleArray(d[1]) { i2 -> a.valueAtDouble(offset + i2, pos) })
                }
                Pair(labelNames, data)
            }
            else -> throwAPLException(InvalidDimensionsException("chart data must be 1- or 2-dimensional", pos))
        }
        val output = AdditionalOutput(ChartOutput(LineChartDescriptor(subtype(), horizontalAxisLabels, datasets)))
        val module = context.engine.findModule<JsChartModule>() ?: throw IllegalStateException("Chart module not found")
        module.sendMessageFn(output)
        return a
    }

    abstract fun subtype() : LineChartSubtype
}

class LineChartFunction : APLFunctionDescriptor {
    class LineChartFunctionImpl(pos: FunctionInstantiation) : GenericLineChartFunctionImpl(pos) {
        override fun subtype() = LineChartSubtype.LINE
    }

    override fun make(instantiation: FunctionInstantiation) = LineChartFunctionImpl(instantiation)
}

class BarChartFunction : APLFunctionDescriptor {
    class BarChartFunctionImpl(pos: FunctionInstantiation) : GenericLineChartFunctionImpl(pos) {
        override fun subtype() = LineChartSubtype.BAR
    }

    override fun make(instantiation: FunctionInstantiation) = BarChartFunctionImpl(instantiation)
}

class DoughnutChartFunction : APLFunctionDescriptor {
    class DoughnutChartFunctionImpl(pos: FunctionInstantiation) : GenericLineChartFunctionImpl(pos) {
        override fun subtype() = LineChartSubtype.DOUGHNUT
    }

    override fun make(instantiation: FunctionInstantiation) = DoughnutChartFunctionImpl(instantiation)
}

class PieChartFunction : APLFunctionDescriptor {
    class PieChartFunctionImpl(pos: FunctionInstantiation) : GenericLineChartFunctionImpl(pos) {
        override fun subtype() = LineChartSubtype.PIE
    }

    override fun make(instantiation: FunctionInstantiation) = PieChartFunctionImpl(instantiation)
}
