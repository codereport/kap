package array.msofficereader

import array.*
import array.builtins.TagCatch
import array.builtins.makeBoolean
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

class ExcelFileWrapper(private val workbook: Workbook) : NativeCloseable {
    val sheetCount: Int get() = workbook.numberOfSheets

    fun sheetName(index: Int): String {
        return workbook.getSheetAt(index).sheetName
    }

    override fun close() {
        workbook.close()
    }

    fun parseSheet(sheetIndex: Int): APLValue {
        val evaluator = workbook.creationHelper.createFormulaEvaluator()
        val sheet = workbook.getSheetAt(sheetIndex)
        return readSheet(sheet, evaluator)
    }
}

fun loadExcelFileWrapper(name: String): ExcelFileWrapper {
    return ExcelFileWrapper(WorkbookFactory.create(File(name), null, true))
}

fun readExcelFile(name: String): APLValue {
    WorkbookFactory.create(File(name), null, true).use { workbook ->
        val evaluator = workbook.creationHelper.createFormulaEvaluator()
        val sheet = workbook.getSheetAt(0)
        return readSheet(sheet, evaluator)
    }
}

fun readSheet(sheet: Sheet, evaluator: FormulaEvaluator): APLValue {
    if (sheet.physicalNumberOfRows == 0) {
        return APLNullValue.APL_NULL_INSTANCE
    }

    val lastRowIndex = sheet.lastRowNum
    val rows = ArrayList<List<APLValue>>()
    for (i in 0..lastRowIndex) {
        val row = sheet.getRow(i)
        val parsedRow = if (row == null) {
            listOf(APLNullValue.APL_NULL_INSTANCE)
        } else {
            readRow(row, evaluator)
        }
        rows.add(parsedRow)
    }

    val width = rows.maxValueBy { it.size }
    return APLArrayImpl.make(dimensionsOfSize(rows.size, width)) { i ->
        val rowIndex = i / width
        val colIndex = i % width
        val row = rows[rowIndex]
        if (colIndex < row.size) {
            row[colIndex]
        } else {
            APLLONG_0
        }
    }
}

fun readRow(row: Row, evaluator: FormulaEvaluator): List<APLValue> {
    val cellList = ArrayList<APLValue>()
    val lastCellIndex = row.lastCellNum
    var numPendingNulls = 0
    for (i in 0 until lastCellIndex) {
        val cell = row.getCell(i)
        if (cell == null) {
            numPendingNulls++
        } else {
            repeat(numPendingNulls) {
                cellList.add(APLLONG_0)
            }
            numPendingNulls = 0
            cellList.add(cellToAPLValue(cell, evaluator))
        }
    }
    return cellList
}

fun cellToAPLValue(cell: Cell, evaluator: FormulaEvaluator): APLValue {
    return when (cell.cellType) {
        CellType.FORMULA -> parseEvaluatedCell(cell, evaluator)
        CellType.BOOLEAN -> makeBoolean(cell.booleanCellValue)
        CellType.BLANK -> APLLONG_0
        CellType.NUMERIC -> APLDouble(cell.numericCellValue)
        CellType.STRING -> APLString.make(cell.stringCellValue)
        CellType.ERROR -> APLString.make("ERROR:${FormulaError.forInt(cell.errorCellValue).string}")
        else -> throw IllegalStateException("Unknown cell type: ${cell.cellType}")
    }
}

fun parseEvaluatedCell(cell: Cell, evaluator: FormulaEvaluator): APLValue {
    val v = evaluator.evaluate(cell)
    return when (cell.cellType) {
        CellType.FORMULA -> APLString.make("FORMULA:${v.stringValue}")
        CellType.BOOLEAN -> (if (v.booleanValue) 1 else 0).makeAPLNumber()
        CellType.BLANK -> APLLONG_0
        CellType.NUMERIC -> v.numberValue.makeAPLNumber()
        CellType.STRING -> APLString.make(v.stringValue)
        else -> throw IllegalStateException("Unknown cell type: ${v.cellType}")
    }
}

fun saveExcelFile(value: APLValue, fileName: String) {
    val dimensions = value.dimensions
    val width = when (dimensions.size) {
        1 -> 1
        2 -> dimensions[1]
        else -> throw IllegalArgumentException("Array must be rank 1 or 2. Dimensions: $dimensions")
    }
    val workbook: Workbook = HSSFWorkbook()
    val sheet = workbook.createSheet()
    var rowIndex = 0
    var colIndex = 0
    var row: Row? = null
    value.iterateMembers { v ->
        if (colIndex == 0) {
            row = sheet.createRow(rowIndex)
        }
        val cell = row!!.createCell(colIndex)
        fillInCell(cell, v)
        if (++colIndex >= width) {
            rowIndex++
            colIndex = 0
        }
    }
    FileOutputStream(fileName).use { s ->
        workbook.write(s)
    }
}

fun fillInCell(cell: Cell, v: APLValue) {
    when {
        v is APLNumber -> if (v.isComplex()) cell.setCellValue("complex") else cell.setCellValue(v.asDouble())
        v.isStringValue() -> cell.setCellValue(v.toStringValue())
        else -> cell.setCellValue("error")
    }
}

class LoadExcelFileFunction : APLFunctionDescriptor {
    class LoadExcelFileFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val filename = a.toStringValue(pos)
            try {
                return readExcelFile(filename)
            } catch (e: FileNotFoundException) {
                throwAPLException(
                    TagCatch(
                        APLSymbol(context.engine.internSymbol("fileNotFound", context.engine.keywordNamespace)),
                        APLString(filename),
                        "File not found: ${filename}",
                        pos))
            }
        }
    }

    override fun make(instantiation: FunctionInstantiation) = LoadExcelFileFunctionImpl(instantiation)
}

class SaveExcelFileFunction : APLFunctionDescriptor {
    class SaveExcelFileFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val fileName = a.toStringValue(pos)
            saveExcelFile(b, fileName)
            return b
        }
    }

    override fun make(instantiation: FunctionInstantiation) = SaveExcelFileFunctionImpl(instantiation)
}


class MsOfficeModule : KapModule {
    override val name get() = "msoffice"

    override fun init(engine: Engine) {
        val ns = engine.makeNamespace("msoffice")
        engine.registerFunction(ns.internAndExport("read"), LoadExcelFileFunction())
        engine.registerFunction(ns.internAndExport("write"), SaveExcelFileFunction())
    }
}
