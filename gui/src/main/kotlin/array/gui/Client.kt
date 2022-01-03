package array.gui

import array.*
import array.gui.arrayedit.ArrayEditor
import array.gui.graphics.initGraphicCommands
import array.gui.settings.Settings
import array.gui.settings.loadSettings
import array.gui.settings.saveSettings
import array.gui.viewer.StructureViewer
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Stage
import java.io.File

class Client(val application: ClientApplication, val stage: Stage) {
    val renderContext: ClientRenderContext = ClientRenderContextImpl()
    val resultList: ResultList3
    val inputFont: Font
    val engine: Engine
    val calculationQueue: CalculationQueue
    val sourceEditors = ArrayList<SourceEditor>()
    private val functionListWindow: FunctionListWindow
    private val keyboardHelpWindow: KeyboardHelpWindow
    private val aboutWindow: AboutWindow
    private val settingsWindow: SettingsWindow
    private var settings: Settings
    private val directoryTextField = TextField()
    private val breakButton = Button("Stop")

    init {
        settings = loadSettings()

        engine = Engine()
        engine.addLibrarySearchPath("../array/standard-lib")
        initCustomFunctions()
        engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"), false)

        engine.standardOutput = SendToMainCharacterOutput()
        calculationQueue = CalculationQueue(engine)

        inputFont = javaClass.getResourceAsStream("fonts/DejaVuSansMono.ttf").use {
            Font.loadFont(it, 18.0) ?: throw IllegalStateException("Unable to load font")
        }

        resultList = ResultList3(this)

        stage.title = "Test ui"

        val border = BorderPane().apply {
            top = makeTopBar()
            center = resultList.getNode()
        }

        functionListWindow = FunctionListWindow.create(renderContext, engine)
        keyboardHelpWindow = KeyboardHelpWindow(renderContext)
        aboutWindow = AboutWindow()
        settingsWindow = SettingsWindow()

        settings.directory.let { dir ->
            if (dir == null) {
                updateWorkingDirectory(currentDirectory())
            } else {
                updateWorkingDirectory(dir)
            }
        }

        calculationQueue.start()
        stage.onCloseRequest = EventHandler { calculationQueue.stop() }

        stage.scene = Scene(border, 1000.0, 800.0)
        stage.show()

        resultList.requestFocus()
    }

    private fun makeTopBar(): VBox {
        val vbox = VBox()
        vbox.children.add(makeMenuBar())
        vbox.children.add(makeToolBar())
        return vbox
    }

    private fun makeMenuBar(): MenuBar {
        return MenuBar().apply {
            val fileMenu = Menu("File").apply {
                items.add(MenuItem("New").apply {
                    onAction = EventHandler { openNewFile() }
                })
                items.add(MenuItem("Open").apply {
                    onAction = EventHandler { selectAndEditFile() }
                })
                items.add(MenuItem("Settings").apply {
                    onAction = EventHandler { openSettingsWindow() }
                })
                items.add(MenuItem("Close").apply {
                    onAction = EventHandler { Platform.exit() }
                })
            }
            menus.add(fileMenu)

            val windowMenu = Menu("Window").apply {
                items.add(MenuItem("Keyboard").apply {
                    onAction = EventHandler { keyboardHelpWindow.show() }
                })
                items.add(MenuItem("Functions").apply {
                    onAction = EventHandler { functionListWindow.show() }
                })
                items.add(MenuItem("Array Editor").apply {
                    onAction = EventHandler { ArrayEditor.open(this@Client) }
                })
                items.add(MenuItem("Structure Viewer").apply {
                    onAction = EventHandler { StructureViewer.open(this@Client) }
                })
            }
            menus.add(windowMenu)

            val helpMenu = Menu("Help").apply {
                items.add(MenuItem("About").apply {
                    onAction = EventHandler { aboutWindow.show() }
                })
            }
            menus.add(helpMenu)
        }
    }

    private fun makeToolBar(): ToolBar {
        return ToolBar(makeWorkingDirectoryButton(), makeBreakButton())
    }

    private fun makeWorkingDirectoryButton(): Node {
        val hbox = HBox()
        hbox.alignment = Pos.BASELINE_LEFT

        val label = Label("Working directory:")
        label.onMouseClicked = EventHandler { selectWorkingDirectory() }
        hbox.children.add(label)
        HBox.setMargin(label, Insets(0.0, 5.0, 0.0, 5.0))

        directoryTextField.prefColumnCount = 20
        hbox.children.add(directoryTextField)

        return hbox
    }

    private fun makeBreakButton(): Node {
        breakButton.onMouseClicked = EventHandler { interruptEvaluation() }
        return breakButton
    }

    private fun interruptEvaluation() {
        engine.interruptEvaluation()
    }

    private fun selectWorkingDirectory() {
        val fileSelector = DirectoryChooser().apply {
            title = "Select working directory"
            val dirString = settings.directory
            if (dirString != null) {
                val dir = File(dirString)
                if (dir.isDirectory) {
                    initialDirectory = dir
                }
            }
        }
        val res = fileSelector.showDialog(stage)
        if (res != null) {
            if (!res.isDirectory) {
                val dialog = Dialog<ButtonType>().apply {
                    title = "Not a valid directory"
                    contentText = "The selected directory is not a directory, or is not readable."
                    dialogPane.buttonTypes.apply {
                        add(ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE))
                    }
                }
                dialog.showAndWait()
            } else {
                updateWorkingDirectory(res.path)
            }
        }
    }

    private fun updateWorkingDirectory(dir: String) {
        engine.workingDirectory = dir
        settings = settings.copy(directory = dir)
        directoryTextField.text = dir
    }

    private fun openNewFile() {
        val editor = SourceEditor(this)
        editor.show()
    }

    fun selectFile(forSave: Boolean = false): File? {
        val fileSelector = FileChooser().apply {
            title = "Open KAP file"
            selectedExtensionFilter = FileChooser.ExtensionFilter("KAP files", ".kap")
            val dir = settings.recentPath
            if (dir != null) {
                val file = File(dir)
                if (file.isDirectory) {
                    initialDirectory = file
                }
            }
        }
        val file = if (forSave) {
            fileSelector.showSaveDialog(stage)
        } else {
            fileSelector.showOpenDialog(stage)
        }
        if (file != null) {
            settings = settings.copy(recentPath = file.parent)
        }
        return file
    }

    private fun selectAndEditFile() {
        selectFile()?.let { file ->
            val editor = SourceEditor(this)
            editor.setFile(file)
            editor.show()
        }
    }

    private fun openSettingsWindow() {
        settingsWindow.show()
    }

    fun sendInput(text: String) {
        evalSource(StringSourceLocation(text))
    }

    fun evalSource(source: SourceLocation, linkNewContext: Boolean = false) {
        calculationQueue.pushRequest(source, linkNewContext) { result ->
            if (result is Either.Right) {
                result.value.printStackTrace()
            }
            Platform.runLater { displayResult(result) }
        }
    }

    val stackTraceWindow by lazy { StackTrace.makeStackTraceWindow(this) }

    private fun displayResult(result: Either<APLValue, Exception>) {
        when (result) {
            is Either.Left -> resultList.addResult(result.value)
            is Either.Right -> {
                val ex = result.value
                resultList.addExceptionResult(ex)
                if (ex is APLGenericException) {
                    if (ex.pos != null) {
                        val pos = ex.pos
                        if (pos != null) {
                            highlightSourceLocation(pos, ex.message ?: "no error message")
                        }
                    }
                    if (ex is APLEvalException) {
                        stackTraceWindow.updateException(ex)
                        stackTraceWindow.show()
                    }
                }
            }
        }
    }

    fun highlightSourceLocation(pos: Position, message: String? = null) {
        val sourceLocation = pos.source
        if (sourceLocation is SourceEditor.EditorSourceLocation) {
            sourceLocation.editor?.let { editor ->
                sourceEditors.forEach { e ->
                    if (e === editor) {
                        editor.highlightError(pos, message)
                    }
                }
            }
        }

    }

    private fun initCustomFunctions() {
        initGraphicCommands(this)
    }

    fun stopRequest() {
        calculationQueue.stop()
        saveSettings(settings)
    }

    private inner class ClientRenderContextImpl : ClientRenderContext {
        private val extendedInput = ExtendedCharsKeyboardInput()

        override fun engine() = engine
        override fun font() = inputFont
        override fun extendedInput() = extendedInput
    }

    private inner class SendToMainCharacterOutput : CharacterOutput {
        override fun writeString(s: String) {
            Platform.runLater {
                resultList.addOutput(s)
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ClientApplication.main(args)
        }
    }
}
