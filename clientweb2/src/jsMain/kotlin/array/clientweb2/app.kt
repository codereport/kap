package array.clientweb2

import array.keyboard.ExtendedCharsKeyboardInput
import dev.fritz2.binding.RootStore
import dev.fritz2.binding.Store
import dev.fritz2.binding.storeOf
import dev.fritz2.dom.html.Input
import dev.fritz2.dom.html.RenderContext
import dev.fritz2.dom.html.render
import dev.fritz2.dom.values
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.Worker
import org.w3c.dom.events.KeyboardEvent

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

class EvalExceptionResult(val ex: EvalExceptionDescriptor) : Result {
    override fun render(context: RenderContext) {
        context.apply {
            div("exception-result") {
                +ex.message
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
        res
    }

    val finishOutput = handle<Result> { model, action ->
        if (model.isNotBlank()) {
            CurrentOutputStore.addString(model)
        }
        CurrentOutputStore.finishOutput(action)
        ProcessingStore.updateProcessing(false)
        ""
    }
}

object ProcessingStore : RootStore<Boolean>(false) {
    val updateProcessing = handle<Boolean> { model, action -> action }
}

fun main() {
    renderClient()
}

object Keymap {
    private val map = HashMap<String, String>()

    init {
        val keyboard = ExtendedCharsKeyboardInput()
        keyboard.keymap.forEach { (k, v) ->
            map[k.character] = v
        }
    }

    fun lookup(ch: String) = map[ch] ?: ch
}

fun RenderContext.inputField(store: Store<String>, enterPressCallback: () -> Unit = {}, content: Input.() -> Unit) {
    val prefixState = object : RootStore<Boolean>(false) {
        val prefixActive = handle<Boolean> { model, action ->
            action
        }
    }

    fun handleKeyPress(input: Input, event: KeyboardEvent) {
        if (prefixState.current) {
            println("Inserting special character")
            event.preventDefault()
            prefixState.prefixActive(false)

            val s = input.domNode.value
            val left = s.substring(0, input.domNode.selectionStart!!)
            val right = s.substring(input.domNode.selectionEnd!!)
            val sym = Keymap.lookup(event.key)
            input.domNode.value = "${left}${sym}${right}"
        } else if (event.key == "`") {
            event.preventDefault()
            prefixState.prefixActive(true)
        }
    }

    input(id = store.id) {
        type("text")
        value(store.data)
        inputs.values() handledBy store.update

        keypresss handledBy { event ->
            when (event.key) {
                "Enter" -> enterPressCallback()
                else -> handleKeyPress(this, event)
            }
        }

        content()
    }

    prefixState.data.render { active ->
        if (active) {
            span {
                +"Prefix active"
            }
        }
    }
}

fun renderClient() {
    val inputState = storeOf("")

    val worker = Worker("compute-queue-worker.js")
    worker.onmessage = { event ->
        when (val response = Json.decodeFromString<ResponseMessage>(event.data as String)) {
            is EvalResponse -> {
                CurrentOutputLine.finishOutput(ReturnValueResult(response.result))
            }
            is ExceptionDescriptor -> {
                CurrentOutputLine.finishOutput(ExceptionResult(response.message))
            }
            is EvalExceptionDescriptor -> {
                CurrentOutputLine.finishOutput(EvalExceptionResult(response))
            }
            is Output -> CurrentOutputLine.addString(response.text)
        }
        window.scrollTo(0.0, document.body!!.scrollHeight.toDouble())
    }

    fun sendCurrentInputState() {
        println("processing state: ${ProcessingStore.current}")
        if (!ProcessingStore.current) {
            println("Sending input state: '${inputState.current}'")
            if (inputState.current.isNotBlank()) {
                History.addEntry(PrevCommandResult(inputState.current))
                worker.postMessage(Json.encodeToString(EvalRequest(inputState.current)))
                ProcessingStore.updateProcessing(true)
            }
        }
    }

    render("#target") {
        h1 { +"KAP interpreter" }
        p {
            +"""
                This is a simple Javascript-based frontend to the KAP interpreter.
                KAP is written in multiplatform Kotlin, and this interpreter uses a version compiled to JS which is run
                inside the local browser.
            """.trimIndent()
        }
        p {
            +"""
                To type special characters, type a backquote (`) followed by the respective key. To type
                a backquote, type backquote followed by space.
            """.trimIndent()
        }
        div {
            History.data.renderEach { element ->
                div("result-element") {
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
                    span {
                        +element
                    }
                }
            }
        }
        div {
            inputField(inputState, ::sendCurrentInputState) {
                size(60)
            }
            button {
                +"Send"
                clicks handledBy { sendCurrentInputState() }
            }
        }
    }
}
