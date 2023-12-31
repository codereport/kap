package array.clientweb2

import kotlinx.browser.document
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.Worker
import org.w3c.dom.events.Event
import org.w3c.files.File
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlin.math.max

private fun processUpload(worker: Worker, event: Event) {
    val varname = (document.getElementById("import-destination") as HTMLInputElement).value
    if (varname.isBlank()) {
        console.log("No variable name")
        return
    }
    val varnameTrimmed = varname.trim()

    val files = (event.target as HTMLInputElement).files!!
    if (files.length != 1) {
        console.log("Only one file currently supported> Got ${files.length}")
        return
    }

    val file = files[0]!!
    val importType = (document.getElementById("import-type") as HTMLSelectElement).value
    println("Import type: '$importType'")
    when (importType) {
        "csv" -> readCsvInput(worker, varnameTrimmed, file)
        "xlsx" -> readXlsInput(worker, varnameTrimmed, file)
    }
}

fun readCsvInput(worker: Worker, varnameTrimmed: String, file: File) {
    val reader = FileReader()
    reader.onload = { event ->
        worker.postMessage(Json.encodeToString(ImportCsvRequest(varnameTrimmed, reader.result) as Request))
    }
    reader.readAsText(file)
}

private fun readXlsInput(worker: Worker, varnameTrimmed: String, file: File) {
    val reader = FileReader()
    reader.onload = { event ->
        val options = js("{}")
        options["dense"] = true
        val workbook = XLSX.read(reader.result, options)
        val sheetList = workbook.Sheets
        val sheet = sheetList[objectKeys(sheetList)[0]]
        worker.postMessage(Json.encodeToString(ImportRequest(varnameTrimmed, convertSheetToKap(sheet)) as Request))
    }
    reader.readAsArrayBuffer(file)
}

private fun convertSheetToKap(sheet: Array<Array<dynamic>>): JsKapValue {
    val rowCount = sheet.size
    if (rowCount == 0) {
        return JsKapArray(listOf(0, 0), emptyList())
    }
    var colCount = 0
    repeat(rowCount) { rowIndex ->
        val row = sheet[rowIndex]
        if (row != undefined) {
            colCount = max(colCount, row.size)
        }
    }
    val values = ArrayList<JsKapValue>()
    repeat(rowCount) { rowIndex ->
        val row = sheet[rowIndex]
        if (row == undefined) {
            repeat(colCount) {
                values.add(JsKapInteger("0"))
            }
        } else {
            repeat(colCount) { colIndex ->
                val v = row[colIndex]
                if (v == undefined) {
                    values.add(JsKapInteger("0"))
                } else {
                    values.add(xlsValueToKap(v))
                }
            }
        }
    }
    return JsKapArray(listOf(rowCount, colCount), values)
}

private fun xlsValueToKap(value: dynamic): JsKapValue {
    return when (value.t) {
        "s" -> JsKapString(value.v)
        "n" -> xlsNumberToKap(value.w)
        else -> JsKapUndefined("unknown type:${value.t}")
    }
}

private fun xlsNumberToKap(s: String): JsKapValue {
    return if (s.contains(".")) {
        JsKapDouble(s.toDouble())
    } else {
        JsKapInteger(s)
    }
}

fun initFileUpload(worker: Worker) {
    document.getElementById("import-file-input")!!.addEventListener("change", { event -> processUpload(worker, event) })
}

private fun objectKeys(@Suppress("UNUSED_PARAMETER") obj: dynamic): Array<String> {
    return js("Object.keys(obj)") as Array<String>
}

@JsModule("xlsx")
@JsNonModule
external object XLSX {
    fun read(data: dynamic, opts: dynamic): dynamic
}
