package array.clientweb2

import array.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.DedicatedWorkerGlobalScope
import org.w3c.xhr.XMLHttpRequest

external val self: DedicatedWorkerGlobalScope

fun main() {
    loadLibraries()
}

var numOutstandingRequests = 0

fun loadLibraries() {
    loadLibFiles(
        "standard-lib/standard-lib.kap",
        "standard-lib/structure.kap",
        "standard-lib/math.kap",
        "standard-lib/io.kap",
        "standard-lib/output.kap",
        "standard-lib/time.kap",
        "standard-lib/regex.kap")
}

private fun loadLibFiles(vararg names: String) {
    numOutstandingRequests = names.size
    names.forEach { name ->
        val http = XMLHttpRequest()
        http.open("GET", name)
        http.onload = {
            if (http.readyState == 4.toShort() && http.status == 200.toShort()) {
                registeredFilesRoot.registerFile(name, http.responseText.encodeToByteArray())
            } else {
                console.log("Error loading library file: ${name}. Code: ${http.status}")
            }
            if (--numOutstandingRequests == 0) {
                initQueue()
            }
        }
        http.send()
    }
}

fun initQueue() {
    val engine = Engine()
    engine.addLibrarySearchPath("standard-lib")
    println("Starting listener: self = ${self}")
    engine.standardOutput = object : CharacterOutput {
        override fun writeString(s: String) {
            val message: ResponseMessage = Output(s)
            self.postMessage(Json.encodeToString(message))
        }
    }
    engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"), false)
    self.onmessage = { event ->
        val request = Json.decodeFromString<EvalRequest>(event.data as String)
        val result = try {
            val value = engine.withThreadLocalAssigned {
                engine.parseAndEval(StringSourceLocation(request.src), newContext = false).collapse()
            }
            EvalResponse(value.formatted(FormatStyle.PRETTY))
        } catch (e: APLGenericException) {
            EvalExceptionDescriptor(e.formattedError(), PosDescriptor.make(e.pos))
        } catch (e: Exception) {
            ExceptionDescriptor(e.message ?: "empty")
        }
        self.postMessage(Json.encodeToString(result))
    }
}
