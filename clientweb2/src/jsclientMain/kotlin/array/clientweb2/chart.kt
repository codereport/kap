package array.clientweb2

import kotlinx.serialization.json.*
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement

@JsModule("chart.js/auto")
@JsNonModule
external class Chart(element: HTMLElement, dat: dynamic)

fun createTestChart() {
    val element = findElement<HTMLCanvasElement>("chart-test")

//    val data: dynamic = mapOf<String, dynamic>(
//        "type" to "bar",
//        "data" to mapOf<String, dynamic>(
//            "datasets" to arrayOf<dynamic>(
//                mapOf<String, dynamic>(
//                    "label" to "Test data",
//                    "data" to arrayOf(1, 2, 10, 11, 12, 21, 2, 3, 1, 3)))))

    val data = buildJsonObject {
        put("type", "bar")
        putJsonObject("data") {
            putJsonArray("labels") {
                add("Foo")
                add("Bar")
            }
            putJsonArray("datasets") {
                addJsonObject {
                    put("label", "Test data")
                    putJsonArray("data") {
                        arrayOf(1, 2).forEach(::add)
                    }
                }
            }
        }
    }
    println("==================FOO=========")
    val s = data.toString()
    console.log(s)
    val d = JSON.parse<dynamic>(s)
    console.log(d)
    val x = Chart(element, d)
}

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
    descriptor.type = "line"
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
