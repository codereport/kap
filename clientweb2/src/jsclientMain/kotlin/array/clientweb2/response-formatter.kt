package array.clientweb2

import kotlinx.html.*

interface KapValueRenderer {
    val styleClass: String? get() = null
    fun FlowContent.render()
}

class JsKapStringRenderer(val result: JsKapString) : KapValueRenderer {
    override val styleClass get() = "kapString"

    override fun FlowContent.render() {
        apply { +result.value }
    }
}

class JsKapIntegerRenderer(val result: JsKapInteger) : KapValueRenderer {
    override val styleClass get() = "kapInteger kapNumber"

    override fun FlowContent.render() {
        +result.value
    }
}

class JsKapDoubleRenderer(val result: JsKapDouble) : KapValueRenderer {
    override val styleClass get() = "kapDouble kapNumber"

    override fun FlowContent.render() {
        +result.value.toString()
    }
}

class JsKapArrayRenderer(val result: JsKapArray) : KapValueRenderer {
    override val styleClass get() = "kapArray"

    override fun FlowContent.render() {
        formatArray(result)
    }
}

class JsKapDefaultRenderer(val result: JsKapValue) : KapValueRenderer {
    override fun FlowContent.render() {
        +"unknown value: $result"
    }
}

fun findRenderer(result: JsKapValue) = when (result) {
    is JsKapString -> JsKapStringRenderer(result)
    is JsKapInteger -> JsKapIntegerRenderer(result)
    is JsKapDouble -> JsKapDoubleRenderer(result)
    is JsKapArray -> JsKapArrayRenderer(result)
    else -> JsKapDefaultRenderer(result)
}

fun FlowContent.formatResponse(response: DataResponse) {
    val renderer = findRenderer(response.result)
    div(renderer.styleClass) {
        renderer.apply { render() }
    }
}

fun FlowContent.formatArray(result: JsKapArray) {
    if (result.dimensions.size == 2) {
        val rowCount = result.dimensions[0]
        val colCount = result.dimensions[1]
        table("array2Table") {
            tbody {
                repeat(rowCount) { rowIndex ->
                    tr {
                        repeat(colCount) { colIndex ->
                            val renderer = findRenderer(result.values[colCount * rowIndex + colIndex])
                            td(renderer.styleClass) {
                                renderer.apply { render() }
                            }
                        }
                    }
                }
            }
        }
    } else {
        +"can only render rank 2 arrays"
    }
}
