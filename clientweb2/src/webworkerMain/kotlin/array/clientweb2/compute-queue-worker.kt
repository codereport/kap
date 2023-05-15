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
        "standard-lib/output3.kap",
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
            val message: ResponseMessage = OutputDescriptor(s)
            self.postMessage(Json.encodeToString(message))
        }
    }
    threadLocalStorageStackRef.value = StorageStack(engine.rootEnvironment)
    engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"), allocateThreadLocals = false)
    self.onmessage = { event ->
        val request = Json.decodeFromString<EvalRequest>(event.data as String)
        val result = try {
            val value = engine.parseAndEval(StringSourceLocation(request.src), allocateThreadLocals = false).collapse()
            when (request.resultType) {
                ResultType.FORMATTED_PRETTY -> StringResponse(value.formatted(FormatStyle.PRETTY))
                ResultType.FORMATTED_READABLE -> StringResponse(value.formatted(FormatStyle.READABLE))
                ResultType.JS -> DataResponse(formatValueToJs(value))
            }
        } catch (e: APLGenericException) {
            EvalExceptionDescriptor(e.formattedError(), makePosDescriptor(e.pos))
        } catch (e: Exception) {
            ExceptionDescriptor(e.message ?: "empty")
        }
        self.postMessage(Json.encodeToString(result))
    }
    val message: ResponseMessage = EngineStartedDescriptor("started")
    self.postMessage(Json.encodeToString(message))
}

fun makePosDescriptor(pos: Position?): PosDescriptor? {
    return if (pos == null) {
        null
    } else {
        PosDescriptor(pos.line, pos.col, pos.callerName)
    }
}
