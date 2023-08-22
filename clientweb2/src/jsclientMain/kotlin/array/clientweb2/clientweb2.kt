package array.clientweb2

import array.SharedArrayBuffer
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.dom.create
import kotlinx.html.js.onClickFunction
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.khronos.webgl.Uint8Array
import org.w3c.dom.*

external fun decodeURIComponent(text: String): String
external fun encodeURIComponent(text: String): String

private fun initWorker(): Worker {
    val worker = Worker("compute-queue-worker.js")
    worker.onmessage = { event ->
        val msg: dynamic = event.data
        console.log("Got msg: ${msg}")
        if (msg.messageType) {
            println("message type = ${msg.messageType}")
            when (msg.messageType) {
                ENGINE_STARTED_TYPE -> engineAvailableCallback(worker, msg)
                OUTPUT_DESCRIPTOR_TYPE -> processOutput(msg)
                IMAGE_CONTENT_TYPE -> updateImage(msg)
                WINDOW_CREATED_TYPE -> openWindow(msg)
            }
        } else {
            println("no message type")
            when (val response = Json.decodeFromString<ResponseMessage>(event.data as String)) {
                is EvalResponse -> addResponseToResultHistory(response)
                is ExceptionDescriptor -> addExceptionResultToResultHistory(response)
                is EvalExceptionDescriptor -> addEvalExceptionResultToResultHistory(response)
                is OutputDescriptor -> error("not used")
                is EngineStartedDescriptor -> error("not used")
                is AdditionalOutput -> processAdditionalOutput(response)
                is ImportResult -> processImportException(response)
            }
        }
    }
    return worker
}

private fun processAdditionalOutput(response: AdditionalOutput) {
    when (val content = response.content) {
        is ChartOutput -> displayChart(content)
        is WindowCreated -> error("not used")
    }
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

    fun appendNode(newNode: HTMLElement) {
        inProgressNode = null
//        val n = createDiv("output-result-element").also { n -> node.appendChild(n) }
//        n.appendChild(newNode)
        node.appendChild(newNode)
    }
}

private fun clearCurrentOutput() {
    currentOutput = null
}

private var currentOutput: CurrentOutput? = null

fun findCurrentOutput(): CurrentOutput {
    return currentOutput ?: createDiv("current-output").let { n ->
        n.classList.add("output-result-outer")
        findResultHistoryNode().appendChild(n)
        CurrentOutput(n).also { c -> currentOutput = c }
    }
}

private fun processOutput(msg: dynamic) {
    val text = msg.text as String
    findCurrentOutput().appendText(text)
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

fun processImportException(response: ImportResult) {
    (document.getElementById("import-result") as HTMLDivElement).textContent = response.message
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
    val rendererSelectorValue: dynamic = document.getElementById("experimental-render")
    worker.postMessage(
        Json.encodeToString(
            EvalRequest(
                command,
                if (rendererSelectorValue.checked) ResultType.JS else ResultType.FORMATTED_PRETTY) as Request))
}

private fun sendCommandFromField(worker: Worker) {
    val inputField = findElement<HTMLTextAreaElement>("input")
    val command = inputField.value
    if (command.trim() != "") {
        addCommandResultToResultHistory(command)
        sendCommand(worker, command)
    }
}

private fun interruptEvaluation() {
    val ba = breakBufferArray
    if (ba != null) {
        js("Atomics.store(ba, 0, 1)")
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
        if (event.key == "Enter") {
            event.preventDefault()
            if (event.shiftKey) {
                insertString("\n")
            } else {
                returnCallback()
            }
        }
//        val currState = getKeyState()
//        when {
//            event.key == "Enter" -> {
//                event.preventDefault()
//                updateKeyState(false)
//                if (event.shiftKey) {
//                    insertString("\n")
//                } else {
//                    returnCallback()
//                }
//            }
//            !currState && event.key == INPUT_PREFIX_SYM -> {
//                event.preventDefault()
//                updateKeyState(true)
//            }
//            currState -> {
//                updateKeyState(false)
//                val sym = if (event.key == " ") INPUT_PREFIX_SYM else Keymap.lookup(event.key)
//                if (sym != null) {
//                    event.preventDefault()
//                    insertString(sym)
//                }
//            }
//        }
        Unit
    }
}

inline fun <reified T : HTMLElement> findElement(id: String): T {
    return getElementByIdOrFail(id) as T
}

fun createDiv(className: String? = null) = createElementWithClassName("div", className) as HTMLDivElement
fun createCanvas(className: String? = null) = createElementWithClassName("canvas", className) as HTMLCanvasElement

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
    val worker = initWorker()
    initFileUpload(worker)
}

/*
    <input class="kap-input" type="text" id="input" size="60">
    <button id="send-button">Send</button>
 */

private var engineInit = false
private var breakData: SharedArrayBuffer? = null
private var breakBufferArray: Uint8Array? = null

fun engineAvailableCallback(worker: Worker, msg: dynamic) {
    if (engineInit) {
        throw IllegalStateException("Client already initialised")
    }

    engineInit = true
    breakData = msg.breakData
    val b = breakData
    if (b != null) {
        breakBufferArray = js("new Uint8Array(b)") as Uint8Array
    }

    val loadingElement = findElement<HTMLDivElement>("loading-message")
    loadingElement.remove()

    val topElement = findElement<HTMLDivElement>("top")
    val outer = document.create.div {
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
            +" "
            button {
                id = "stop-button"
                +"Stop"
            }
        }
    }

    topElement.appendChild(outer)

    val inputField = findElement<HTMLTextAreaElement>("input")
    configureAPLInputForField(inputField) { sendCommandFromField(worker) }

    val sendButton = findElement<HTMLButtonElement>("send-button")
    sendButton.onclick = { sendCommandFromField(worker) }

    val stopButton = findElement<HTMLButtonElement>("stop-button")
    stopButton.onclick = { interruptEvaluation() }

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
