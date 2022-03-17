package array.clientweb2

import array.keyboard.ExtendedCharsKeyboardInput
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.*

external fun decodeURIComponent(text: String): String

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
        when (val response = Json.decodeFromString<ResponseMessage>(event.data as String)) {
            is EvalResponse -> addResponseToResultHistory(response)
            is ExceptionDescriptor -> addExceptionResultToResultHistory(response)
            is EvalExceptionDescriptor -> addEvalExceptionResultToResultHistory(response)
            is Output -> processOutput(response.text)
        }
        window.scrollTo(0.0, document.body!!.scrollHeight.toDouble())
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

private fun addCommandResultToResultHIstory(command: String) {
    val node = createDiv("command-result")
    node.innerText = command
    appendNodeToResultHistory(node)
}

private fun appendNodeToResultHistory(outer: HTMLDivElement) {
    findResultHistoryNode().appendChild(outer)
    clearCurrentOutput()
}

private fun sendCommand(worker: Worker, command: String) {
    println("Sending command: '${command}'")
    worker.postMessage(Json.encodeToString(EvalRequest(command)))
}

private fun initClient(worker: Worker) {
    val inputField = findElement<HTMLInputElement>("input")
    configureAPLInputForField(inputField) { sendCommandFromField(worker) }

    val button = findElement<HTMLButtonElement>("send-button")
    button.onclick = { sendCommandFromField(worker) }
}

private fun sendCommandFromField(worker: Worker) {
    val inputField = findElement<HTMLInputElement>("input")
    val command = inputField.value
    addCommandResultToResultHIstory(command)
    if (command.trim() != "") {
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
    initClient(worker)

    val location = document.location
    if (location != null) {
        if (location.hash.startsWith("#")) {
            val initialCommand = decodeURIComponent(location.hash.substring(1))
            if (initialCommand.trim().isNotBlank()) {
                val inputField = findElement<HTMLInputElement>("input")
                inputField.value = initialCommand
                sendCommand(worker, initialCommand)
            }
        }
    }
}
