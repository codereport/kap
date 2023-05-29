package array.clientweb2

import array.keyboard.ExtendedCharsKeyboardInput
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.dom.create
import kotlinx.html.js.onClickFunction
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.*

external fun decodeURIComponent(text: String): String
external fun encodeURIComponent(text: String): String

var useHtmlValueRenderer: Boolean = false

object Keymap {
    private val map = HashMap<String, String>()

    init {
        val keyboard = ExtendedCharsKeyboardInput()
        keyboard.keymap.forEach { (k, v) ->
            map[k.character] = v
        }
    }

    fun lookup(ch: String) = map[ch]
}

private fun initWorker(): Worker {
    val worker = Worker("compute-queue-worker.js")
    worker.onmessage = { event ->
        println("eventdata: ${event.data}")
        when (val response = Json.decodeFromString<ResponseMessage>(event.data as String)) {
            is EvalResponse -> addResponseToResultHistory(response)
            is ExceptionDescriptor -> addExceptionResultToResultHistory(response)
            is EvalExceptionDescriptor -> addEvalExceptionResultToResultHistory(response)
            is OutputDescriptor -> processOutput(response.text)
            is EngineStartedDescriptor -> engineAvailableCallback(worker)
        }
    }
    return worker
}

class CurrentOutput(val node: HTMLDivElement) {
    var inProgressNode: HTMLDivElement? = null

    fun appendText(text: String) {
        val parts = text.split("\n")
        val currNode = inProgressNode
        val remaining = if (currNode != null && parts.isNotEmpty()) {
            currNode.textContent = (currNode.textContent ?: "") + parts[0]
            parts.subList(1, parts.size)
        } else {
            parts
        }

        if (remaining.isNotEmpty()) {
            remaining.forEach { s ->
                val n = createDiv("output-result-element").also { n -> node.appendChild(n) }
                n.textContent = (n.textContent ?: "") + s
                inProgressNode = n
            }
        }
    }
}

private fun clearCurrentOutput() {
    currentOutput = null
}

private var currentOutput: CurrentOutput? = null

private fun processOutput(text: String) {
    val curr = currentOutput ?: createDiv("current-output").let { n ->
        n.classList.add("output-result-outer")
        findResultHistoryNode().appendChild(n)
        CurrentOutput(n).also { c -> currentOutput = c }
    }
    curr.appendText(text)
}

private fun findResultHistoryNode(): HTMLDivElement {
    return findElement("result-history")
}

private fun addResponseToResultHistory(response: EvalResponse) {
    val outer = document.create.div(classes = "return-result source-node") {
        when (response) {
            is StringResponse -> {
                +response.result
            }
            is DataResponse -> {
                formatResponse(response)
            }
        }
    }
    appendNodeToResultHistory(outer)
    window.scrollTo(0.0, document.body!!.scrollHeight.toDouble())
}

private fun addExceptionResultToResultHistory(response: ExceptionDescriptor) {
    val node = document.create.div(classes = "exception-result") {
        +response.message
    }
    appendNodeToResultHistory(node)
}

private fun addEvalExceptionResultToResultHistory(response: EvalExceptionDescriptor) {
    val node = document.create.div("exception-result") {
        +response.message
    }
    appendNodeToResultHistory(node)
}

private fun addCommandResultToResultHistory(command: String) {
    val node = document.create.div(classes = "command-result-outer") {
        div(classes = "command-result-inner") {
            span(classes = "command-result-text") {
                onClickFunction = { updateInputText(command) }
                +command
            }
            span(classes = "command-result-link") {
                +" "
                a("#${encodeURIComponent(command.trim())}") {
                    +"Link"
                }
            }
        }
    }

    appendNodeToResultHistory(node)
}

fun updateInputText(command: String) {
    val inputField = findElement<HTMLTextAreaElement>("input")
    inputField.value = command
}

private fun appendNodeToResultHistory(outer: HTMLElement) {
    findResultHistoryNode().appendChild(outer)
    clearCurrentOutput()
}

private fun sendCommand(worker: Worker, command: String) {
    println("Sending command: '${command}'")
    val rendererSelectorValue: dynamic = document.getElementsByName("experimentalRender")[0]
    worker.postMessage(Json.encodeToString(EvalRequest(command, if (rendererSelectorValue.checked) ResultType.JS else ResultType.FORMATTED_PRETTY)))
}

private fun sendCommandFromField(worker: Worker) {
    val inputField = findElement<HTMLTextAreaElement>("input")
    val command = inputField.value
    if (command.trim() != "") {
        addCommandResultToResultHistory(command)
        sendCommand(worker, command)
    }
}

private const val INPUT_PREFIX_STATE_KEY = "inputState"
private const val INPUT_PREFIX_SYM = "`"

fun configureAPLInputForField(inputField: HTMLTextAreaElement, returnCallback: () -> Unit) {
    fun updateKeyState(value: Boolean) {
        inputField.setAttribute(INPUT_PREFIX_STATE_KEY, if (value) "1" else "0")
    }

    fun getKeyState(): Boolean {
        val value = inputField.getAttribute(INPUT_PREFIX_STATE_KEY)
        return value != null && value == "1"
    }

    fun insertString(s: String) {
        val prevText = inputField.value
        val sel = inputField.selectionStart!!
        val left = prevText.substring(0, sel)
        val right = prevText.substring(inputField.selectionEnd!!)
        inputField.value = "${left}${s}${right}"
        inputField.selectionStart = sel + s.length
        inputField.selectionEnd = sel + s.length
    }

    inputField.onkeypress = { event ->
        val currState = getKeyState()
        when {
            event.key == "Enter" -> {
                event.preventDefault()
                updateKeyState(false)
                if (event.shiftKey) {
                    insertString("\n")
                } else {
                    returnCallback()
                }
            }
            !currState && event.key == INPUT_PREFIX_SYM -> {
                event.preventDefault()
                updateKeyState(true)
            }
            currState -> {
                updateKeyState(false)
                val sym = if (event.key == " ") INPUT_PREFIX_SYM else Keymap.lookup(event.key)
                if (sym != null) {
                    event.preventDefault()
                    insertString(sym)
                }
            }
        }
        Unit
    }
}

inline fun <reified T : HTMLElement> findElement(id: String): T {
    return getElementByIdOrFail(id) as T
}

fun createDiv(className: String? = null) = createElementWithClassName("div", className) as HTMLDivElement

fun createElementWithClassName(type: String, className: String?): HTMLElement {
    val element = document.createElement(type)
    if (className != null) {
        element.className = className
    }
    return element as HTMLElement
}

fun getElementByIdOrFail(id: String): Element {
    return document.getElementById(id) ?: throw IllegalStateException("element not found: ${id}")
}

fun main() {
    initWorker()
}

/*
    <input class="kap-input" type="text" id="input" size="60">
    <button id="send-button">Send</button>
 */

private var engineInit = false

fun engineAvailableCallback(worker: Worker) {
    if (engineInit) {
        throw IllegalStateException("Client already initialised")
    }

    engineInit = true

    val loadingElement = findElement<HTMLDivElement>("loading-message")
    loadingElement.remove()

    val topElement = findElement<HTMLDivElement>("top")
    val outer = document.create.div {
        div {
            +"Use experimental new renderer: "
            input(InputType.checkBox, name = "experimentalRender") {
                checked = false
            }
        }
        div {
            createKeyboardHelp()
        }
        div {
            id = "result-history"
        }
        div {
            textArea(classes = "kap-input") {
                id = "input"
                rows = "4"
                cols = "60"
            }
            +" "
            button {
                id = "send-button"
                +"Send"
            }
        }
    }

    topElement.appendChild(outer)

    val inputField = findElement<HTMLTextAreaElement>("input")
//    configureAPLInputForField(inputField) { sendCommandFromField(worker) }
    inputField.onkeypress = { event ->
        if (event.key == "Enter") {
            event.preventDefault()
            sendCommandFromField(worker)
        }
        Unit
    }

    val button = findElement<HTMLButtonElement>("send-button")
    button.onclick = { sendCommandFromField(worker) }

    val location = document.location
    if (location != null) {
        if (location.hash.startsWith("#")) {
            val initialCommand = decodeURIComponent(location.hash.substring(1))
            if (initialCommand.trim().isNotBlank()) {
                inputField.value = initialCommand
//                addCommandResultToResultHistory(initialCommand)
//                sendCommand(worker, initialCommand)
            }
        }
    }

    engineInit = true
}
