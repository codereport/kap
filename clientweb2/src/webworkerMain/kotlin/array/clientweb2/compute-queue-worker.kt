package array.clientweb2

import array.*
import array.csv.CsvParseException
import array.csv.readCsv
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
        "standard-lib/math-kap.kap",
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
    println("Starting listener: self = ${self}")
    val engine = Engine()
    engine.addLibrarySearchPath("standard-lib")
    val sendMessageFn = { msg: dynamic -> self.postMessage(msg) }
    engine.standardOutput = object : CharacterOutput {
        override fun writeString(s: String) {
            sendMessageFn(OutputDescriptor(s))
        }
    }
    engine.addModule(JsChartModule(sendMessageFn))
    engine.addModule(JsGuiModule(sendMessageFn))
    engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"))
    self.onmessage = { event ->
        when (val request = Json.decodeFromString<Request>(event.data as String)) {
            is EvalRequest -> processEvalRequest(engine, request)
            is ImportRequest -> processImportRequest(engine, request)
            is ImportCsvRequest -> processImportCsvRequest(engine, request)
        }
    }
    val message: dynamic = makeEngineStartedDescriptor()
    self.postMessage(message)
}

private fun makeEngineStartedDescriptor(): dynamic {
    val result: dynamic = js("{}")
    result.messageType = ENGINE_STARTED_TYPE
    result.text = "started"
    return result
}

private fun processEvalRequest(engine: Engine, request: EvalRequest) {
    val result = try {
        engine.parseAndEvalWithPostProcessing(StringSourceLocation(request.src)) { _, context, result ->
            val collapsed = result.collapse()
            when (request.resultType) {
                ResultType.FORMATTED_PRETTY -> StringResponse(formatResultToStrings(renderResult(context, collapsed)).joinToString("\n"))
                ResultType.FORMATTED_READABLE -> StringResponse(collapsed.formatted(FormatStyle.READABLE))
                ResultType.JS -> DataResponse(formatValueToJs(collapsed))
            }
        }
    } catch (e: APLGenericException) {
        EvalExceptionDescriptor(e.formattedError(), makePosDescriptor(e.pos))
    } catch (e: Exception) {
        ExceptionDescriptor(e.message ?: "empty")
    }
    self.postMessage(Json.encodeToString(result))
}

private fun processImportRequest(engine: Engine, request: ImportRequest) {
    importIntoVariable(engine, request.varname) {
        formatJsToValue(request.data)
    }
}

fun processImportCsvRequest(engine: Engine, request: ImportCsvRequest) {
    importIntoVariable(engine, request.varname) {
        try {
            readCsv(StringCharacterProvider(request.data))
        } catch (e: CsvParseException) {
            throw ImportFailedException("Error importing CSV: ${e.message}")
        }
    }
}

class ImportFailedException(val responseMessage: String) : Exception("Import function failed: ${responseMessage}")

private fun importIntoVariable(engine: Engine, varname: String, fn: () -> APLValue) {
    val sym = engine.currentNamespace.internSymbol(varname)
    val env = engine.rootEnvironment
    val binding = env.findBinding(sym) ?: env.bindLocal(sym)
    engine.withThreadLocalAssigned {
        engine.recomputeRootFrame()
        val importResult = try {
            engine.rootStackFrame.storageList[binding.storage.index].updateValue(fn())
            ImportResult("Data imported into: ${varname}")
        } catch (e: ImportFailedException) {
            ImportResult(e.responseMessage)
        }
        self.postMessage(Json.encodeToString(importResult as ResponseMessage))
    }
}

fun makePosDescriptor(pos: Position?): PosDescriptor? {
    return if (pos == null) {
        null
    } else {
        PosDescriptor(pos.line, pos.col, pos.callerName)
    }
}
