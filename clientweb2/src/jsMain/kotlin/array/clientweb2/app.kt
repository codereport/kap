package array.clientweb2

import dev.fritz2.binding.RootStore
import dev.fritz2.binding.Store
import dev.fritz2.binding.storeOf
import dev.fritz2.dom.html.Input
import dev.fritz2.dom.html.Pre
import dev.fritz2.dom.html.RenderContext
import dev.fritz2.dom.html.render
import dev.fritz2.dom.values
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.Worker

interface Result {
    fun render(context: RenderContext)
}

class PrevCommandResult(val src: String) : Result {
    override fun render(context: RenderContext) {
        context.apply {
            div("command-result") {
                +src
            }
        }
    }
}

class ReturnValueResult(val text: String) : Result {
    override fun render(context: RenderContext) {
        context.apply {
            div("return-result") {
                +text
            }
        }
    }
}

class ExceptionResult(val message: String) : Result {
    override fun render(context: RenderContext) {
        context.apply {
            div("exception-result") {
                +message
            }
        }
    }
}

class OutputResult(val text: List<String>) : Result {
    override fun render(context: RenderContext) {
        context.apply {
            div("output-result-outer") {
                text.forEach { string ->
                    div("output-result-element") {
                        +string
                    }
                }
            }
        }
    }
}

object History : RootStore<List<Result>>(emptyList()) {
    val addEntry = handle<Result> { model, action -> model + action }
}

object CurrentOutputStore : RootStore<List<String>>(emptyList()) {
    val addString = handle<String> { model, action ->
        println("OUTSTORE adding string: '${action}' : after add: ${model + action}")
        model + action
    }

    val finishOutput = handle<Result> { model, action ->
        History.addEntry(OutputResult(model))
        History.addEntry(action)
        emptyList()
    }
}

object CurrentOutputLine : RootStore<String>("") {
    val addString = handle<String> { model, action ->
        val parts = action.split("\n")
        println("COL parts: ${parts} (prev content: '${model}')")
        val res = when (parts.size) {
            0 -> model
            1 -> "${model}${parts[0]}"
            else -> {
                CurrentOutputStore.addString("${model}${parts[0]}")
                for (i in 1 until (parts.size - 1)) {
                    CurrentOutputStore.addString(parts[i])
                }
                parts[parts.size - 1]
            }
        }
        println("COL updated to: '${res}'")
        res
    }

    val finishOutput = handle<Result> { model, action ->
        if (model.isNotBlank()) {
            CurrentOutputStore.addString(model)
        }
        CurrentOutputStore.finishOutput(action)
        ""
    }
}

fun main() {
    renderClient()
}

fun RenderContext.outputList() {
    div {

    }
}

fun RenderContext.inputField(store: Store<String>, content: Input.() -> Unit) {
    input(id = store.id) {
        type("text")
        value(store.data)
        changes.values() handledBy store.update
        content()
    }
}

class StyledContent(val text: String, val cssClass: String)

fun renderClient() {
    val inputState = storeOf("")

    val worker = Worker(makeWorkerUrl())
    worker.onmessage = { event ->
        println("Got response from worker: ${event.data}")
        when (val response = Json.decodeFromString<ResponseMessage>(event.data as String)) {
            is EvalResponse -> {
                CurrentOutputLine.finishOutput(ReturnValueResult(response.result))
            }
            is ExceptionDescriptor -> {
                CurrentOutputLine.finishOutput(ExceptionResult(response.message))
            }
            is Output -> CurrentOutputLine.addString(response.text)
        }
    }

    render("#target") {
        h1 { +"Test page" }
        div {
            History.data.renderEach { element ->
                pre {
                    element.render(this)
                }
            }
        }
        div {
            CurrentOutputStore.data.renderEach { element ->
                div("current-output-list") {
                    +element
                }
            }
            div("current-output-line") {
                CurrentOutputLine.data.render { element ->
                    println("Rendering COL: '${element}'")
                    span {
                        +element
                    }
                }
            }
        }
        div {
            inputField(inputState) {
                size(60)
            }
            button {
                +"Send event"
                clicks handledBy {
                    println("Sending message to worker: '${inputState.current}'")
                    worker.postMessage(Json.encodeToString(EvalRequest(inputState.current)))
                }
            }
        }
    }
}

fun makeTestString(): String {
    val b = StringBuilder()
    b.append("foo")
    b.append("xyztestfootest")
    return b.toString()
}

fun makeWorkerUrl(): String {
    return "compute-queue-worker.js"
}
