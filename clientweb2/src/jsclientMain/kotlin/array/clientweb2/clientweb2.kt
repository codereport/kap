package array.clientweb2

import array.keyboard.ExtendedCharsKeyboardInput
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.dom.create
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.*

external fun decodeURIComponent(text: String): String
external fun encodeURIComponent(text: String): String

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
    val outer = createDiv("return-result")
    outer.classList.add("source-node")
    outer.innerText = response.result
    appendNodeToResultHistory(outer)
    window.scrollTo(0.0, document.body!!.scrollHeight.toDouble())
}

private fun addExceptionResultToResultHistory(response: ExceptionDescriptor) {
    val node = createDiv("exception-result")
    node.innerText = response.message
    appendNodeToResultHistory(node)
}

private fun addEvalExceptionResultToResultHistory(response: EvalExceptionDescriptor) {
    val node = createDiv("exception-result")
    node.innerText = response.message
    appendNodeToResultHistory(node)
}

private fun addCommandResultToResultHistory(command: String) {
    val node = document.create.div(classes = "command-result") {
        span(classes = "command-result-text") {
            +command
        }
        span(classes = "command-result-link") {
            +" "
            a("#${encodeURIComponent(command.trim())}") {
                +"Link"
            }
        }
    }

    appendNodeToResultHistory(node)
}

private fun appendNodeToResultHistory(outer: HTMLElement) {
    findResultHistoryNode().appendChild(outer)
    clearCurrentOutput()
}

private fun sendCommand(worker: Worker, command: String) {
    println("Sending command: '${command}'")
    worker.postMessage(Json.encodeToString(EvalRequest(command)))
}

private fun sendCommandFromField(worker: Worker) {
    val inputField = findElement<HTMLInputElement>("input")
    val command = inputField.value
    if (command.trim() != "") {
        addCommandResultToResultHistory(command)
        sendCommand(worker, command)
    }
}

private const val INPUT_PREFIX_STATE_KEY = "inputState"
private const val INPUT_PREFIX_SYM = "`"

fun configureAPLInputForField(inputField: HTMLInputElement, returnCallback: () -> Unit) {
    fun updateKeyState(value: Boolean) {
        inputField.setAttribute(INPUT_PREFIX_STATE_KEY, if (value) "1" else "0")
    }

    fun getKeyState(): Boolean {
        val value = inputField.getAttribute(INPUT_PREFIX_STATE_KEY)
        return value != null && value == "1"
    }

    inputField.onkeypress = { event ->
        val currState = getKeyState()
        if (event.key == "Enter") {
            event.preventDefault()
            updateKeyState(false)
            returnCallback()
        } else if (!currState && event.key == INPUT_PREFIX_SYM) {
            event.preventDefault()
            updateKeyState(true)
        } else if (currState) {
            updateKeyState(false)
            val sym = if (event.key == " ") INPUT_PREFIX_SYM else Keymap.lookup(event.key)
            if (sym != null) {
                event.preventDefault()
                val s = inputField.value
                val sel = inputField.selectionStart!!
                val left = s.substring(0, sel)
                val right = s.substring(inputField.selectionEnd!!)
                inputField.value = "${left}${sym}${right}"
                inputField.selectionStart = sel + 1
                inputField.selectionEnd = sel + 1
            }
        }
        Unit
    }
}

inline fun <reified T : HTMLElement> findElement(id: String): T {
    return getElementByIdOrFail(id) as T
}

fun createDiv(className: String? = null) = createElementWithClassName("div", className) as HTMLDivElement
fun createSpan(className: String? = null) = createElementWithClassName("span", className) as HTMLSpanElement
fun createHref(className: String? = null) = createElementWithClassName("a", className) as HTMLAnchorElement
fun createInput(className: String? = null) = createElementWithClassName("input", className) as HTMLAnchorElement

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

    val loadingElement = findElement<HTMLDivElement>("loading-message")
    loadingElement.remove()

    val topElement = findElement<HTMLDivElement>("top")
    val outer = document.create.div {
        div {
            id = "result-history"
        }
        div {
            input(classes = "kap-input") {
                id = "input"
                size = "60"
                type = InputType.text
            }
            button {
                id = "send-button"
                +"Send"
            }
        }
    }

    topElement.appendChild(outer)

    val inputField = findElement<HTMLInputElement>("input")
    configureAPLInputForField(inputField) { sendCommandFromField(worker) }
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
