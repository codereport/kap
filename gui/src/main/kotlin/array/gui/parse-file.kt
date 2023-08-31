package array.gui

import array.APLValue
import array.msofficereader.ExcelFileWrapper
import array.msofficereader.loadExcelFileWrapper
import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ListView
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import java.io.IOException

fun readExcelFileAndSelectSheet(parent: Stage, filename: String, loadedCallback: (APLValue) -> Unit) {
    val workbook = try {
        loadExcelFileWrapper(filename)
    } catch (e: IOException) {
        displayErrorWithStage(parent, "Error reading Excel file", "Error reading ${filename}: ${e.message}")
        return
    }
    when (workbook.sheetCount) {
        0 -> {
            displayErrorWithStage(parent, "Document does not contain any sheets")
        }
        1 -> {
            try {
                val sheet = workbook.parseSheet(0)
                loadedCallback(sheet)
            } finally {
                workbook.close()
            }
        }
        else -> {
            openSelectSheet(workbook, loadedCallback)
        }
    }
}

class SheetName(val name: String, val index: Int) {
    override fun toString() = "${index}: ${name}"
}

class SheetSelect {
    lateinit var stage: Stage
    lateinit var workbook: ExcelFileWrapper
    lateinit var loadedCallback: (APLValue) -> Unit
    lateinit var sheetList: ListView<SheetName>
    lateinit var ok: Button
    lateinit var cancel: Button

    fun updateWorkbook(w: ExcelFileWrapper) {
        workbook = w
        repeat(w.sheetCount) { i ->
            sheetList.items.add(SheetName(w.sheetName(i), i))
        }
    }

    fun loadClicked(@Suppress("UNUSED_PARAMETER") event: ActionEvent) {
        try {
            val selected = sheetList.selectionModel.selectedItem
            val sheet = workbook.parseSheet(selected.index)
            loadedCallback(sheet)
            stage.close()
        } finally {
            workbook.close()
        }
    }

    fun cancelClicked(@Suppress("UNUSED_PARAMETER") event: ActionEvent) {
        stage.close()
    }
}

fun openSelectSheet(workbook: ExcelFileWrapper, loadedCallback: (APLValue) -> Unit) {
    val loader = FXMLLoader(SheetSelect::class.java.getResource("sheet-select.fxml"))
    val root = loader.load<Parent>()
    val controller = loader.getController<SheetSelect>()
    controller.updateWorkbook(workbook)
    controller.loadedCallback = loadedCallback

    val stage = Stage(StageStyle.UTILITY).apply {
        initModality(Modality.APPLICATION_MODAL)
        title = "Select sheet"
    }

    stage.scene = Scene(root)
    stage.show()
    stage.requestFocus()
    controller.stage = stage
}
