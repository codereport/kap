package array.clientweb2

import org.w3c.dom.HTMLElement

@JsModule("chart.js/auto")
@JsNonModule
external class Chart(element: HTMLElement, data: dynamic)

fun displayChart(chartOutput: ChartOutput) {
    when (val content = chartOutput.content) {
        is LineChartDescriptor -> displayLineChart(content)
    }
}

fun displayLineChart(content: LineChartDescriptor) {
    val element = createDiv("output-chart")
    val canvas = createCanvas("output-chart-inner")
    element.appendChild(canvas)
    findCurrentOutput().appendNode(element)

    val descriptor: dynamic = js("{}")
    descriptor.type = when (content.subtype) {
        LineChartSubtype.LINE -> "line"
        LineChartSubtype.BAR -> "bar"
        LineChartSubtype.DOUGHNUT -> "doughnut"
        LineChartSubtype.PIE -> "pie"
    }
    descriptor.data = makeData(content)
    console.log(descriptor)
    Chart(canvas, descriptor)
}

private fun makeData(content: LineChartDescriptor): dynamic {
    val data = js("{}")
    val labels = js("[]")
    content.horizontalAxisLabels.forEach { label ->
        labels.push(label)
    }
    data.labels = labels
    data.datasets = js("[]")
    content.data.forEach { e ->
        val dataset = js("{}")
        dataset.label = e.name
        val list = js("[]")
        e.data.forEach { value -> list.push(value) }
        dataset.data = list
        data.datasets.push(dataset)
    }
    return data
}
