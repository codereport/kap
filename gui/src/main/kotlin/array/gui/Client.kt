package array.gui

import array.*
import array.gui.arrayedit.ArrayEditor
import array.gui.graph.GraphModule
import array.gui.graphics.GuiModule
import array.gui.reporting.ReportingClient
import array.gui.settings.Settings
import array.gui.settings.loadSettings
import array.gui.settings.saveSettings
import array.gui.styledarea.TextStyle
import array.gui.viewer.StructureViewer
import array.keyboard.ExtendedCharsKeyboardInput
import com.panemu.tiwulfx.control.dock.DetachableTab
import javafx.application.Application.launch
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Orientation
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
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class Client(val stage: Stage, extraPaths: List<String>? = null) {
    val renderContext: ClientRenderContext = ClientRenderContextImpl()
    val resultList: ResultList3
    val engine: Engine
    val calculationQueue: CalculationQueue
    val sourceEditors = ArrayList<SourceEditor>()
    private var inputFont: Font
    private val functionListWindow: FunctionListController
    private val varListWindow: VariableListController
    private val keyboardHelpWindow: KeyboardHelpWindow
    private var leftDtPane: DetachablePaneWrapper
    private var rightDtPane: DetachablePaneWrapper
    private var bottomDtPane: DetachablePaneWrapper
    private val aboutWindow: AboutWindow
    var settings: Settings
        private set
    private val directoryTextField = TextField()
    private val breakButton = Button("Stop")
    private val stackTraceWindow: StackTrace

    init {
        java.lang.Long.numberOfLeadingZeros(1)
        settings = loadSettings()

        engine = Engine()
        engine.addLibrarySearchPath("../array/standard-lib")
        extraPaths?.forEach(engine::addLibrarySearchPath)
        initModules()
        engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"))

        engine.standardOutput = SendToMainCharacterOutput()
        calculationQueue = CalculationQueue(engine)

        val fontFiles = listOf("iosevka-fixed-regular.ttf", "iosevka-fixed-bold.ttf", "JuliaMono-Regular.ttf", "JuliaMono-Bold.ttf")
        fontFiles.forEach { fileName ->
            javaClass.getResourceAsStream("fonts/${fileName}").use { contentIn ->
                Font.loadFont(contentIn, 18.0) ?: throw IllegalStateException("Unable to load font: ${fileName}")
            }
        }

        inputFont = Font(settings.fontFamilyWithDefault(), settings.fontSizeWithDefault().toDouble())

        resultList = ResultList3(this)

        stage.title = "KAP"

        leftDtPane = DetachablePaneWrapper()
        rightDtPane = DetachablePaneWrapper()
        val horizSplitPane = SplitPane().apply {
            orientation = Orientation.HORIZONTAL
            items.add(BorderPane().also { p ->
                SplitPane.setResizableWithParent(p, false)
                p.center = leftDtPane.pane
            })
            items.add(BorderPane().also { p ->
                p.center = resultList.getNode()
            })
            items.add(BorderPane().also { p ->
                SplitPane.setResizableWithParent(p, false)
                p.center = rightDtPane.pane
            })
            setDividerPositions(0.0, 0.75)
        }

        bottomDtPane = DetachablePaneWrapper()
        val vertSplitPane = SplitPane().apply {
            orientation = Orientation.VERTICAL
            items.add(horizSplitPane)
            items.add(BorderPane().apply { center = bottomDtPane.pane })
            setDividerPosition(0, 0.75)
        }

        val border = BorderPane().apply {
            top = makeTopBar()
            center = vertSplitPane
        }

        stackTraceWindow = StackTrace.makeStackTraceWindow(this)
        bottomDtPane.pane.tabs.add(DetachableTab("Stack trace", stackTraceWindow.borderPane).apply { isClosable = false })

        functionListWindow = FunctionListController(engine)
        rightDtPane.pane.tabs.add(DetachableTab("Function list", functionListWindow.node).apply { isClosable = false })

        varListWindow = VariableListController(this)
        rightDtPane.pane.tabs.add(DetachableTab("Variable list", varListWindow.node).apply { isClosable = false })
        rightDtPane.pane.selectionModel.select(1)

        keyboardHelpWindow = KeyboardHelpWindow(renderContext)
        aboutWindow = AboutWindow()

        updateWorkingDirectory(settings.directory ?: currentDirectory())

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
                items.add(MenuItem("Array Editor").apply {
                    onAction = EventHandler { Platform.runLater{ ArrayEditor.open(this@Client) } }
                })
                items.add(MenuItem("Structure Viewer").apply {
                    onAction = EventHandler { Platform.runLater { StructureViewer.open(this@Client) } }
                })
                items.add(MenuItem("Create Report").apply {
                    onAction = EventHandler { createReport() }
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

    fun selectFile(forSave: Boolean = false, nameHeader: String = "Open KAP file"): File? {
        val fileSelector = FileChooser().apply {
            title = nameHeader
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

    private fun createReport() {
        ReportingClient.open()
    }

    private fun openSettingsWindow() {
        val dialog = SettingsDialog(stage, settings)
        dialog.showAndWait().ifPresent { result ->
            settings = result
            saveSettings(settings)
        }
    }

    fun evalSource(source: SourceLocation) {
        calculationQueue.pushRequest(source) { result ->
            if (result is Either.Right) {
                result.value.printStackTrace()
            }
            Platform.runLater { displayResult(result) }
        }
    }

    // Suppress here because of this issue https://youtrack.jetbrains.com/issue/KTIJ-20744
    @Suppress("USELESS_IS_CHECK")
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
                    }
                }
            }
        }
    }

    fun highlightSourceLocation(pos: Position, message: String? = null) {
        when (val sourceLocation = pos.source) {
            is SourceEditor.EditorSourceLocation -> {
                sourceLocation.editor?.let { editor ->
                    sourceEditors.forEach { e ->
                        if (e === editor) {
                            editor.highlightError(pos, message)
                        }
                    }
                }
            }
            is REPLSourceLocation -> {
                highlightErrorInRepl(sourceLocation, pos)
            }
        }
    }

    private fun highlightErrorInRepl(sourceLocation: REPLSourceLocation, pos: Position) {
        resultList.updateStyle(
            sourceLocation.tag,
            pos.line,
            pos.col,
            pos.computedEndLine,
            pos.computedEndCol,
            TextStyle(TextStyle.Type.SINGLE_CHAR_HIGHLIGHT))
    }

    private fun initModules() {
        engine.addModule(GuiModule())
        engine.addModule(GraphModule())
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

    @OptIn(ExperimentalContracts::class)
    fun withErrorDialog(name: String, details: String? = null, fn: () -> Unit) {
        contract { callsInPlace(fn, InvocationKind.EXACTLY_ONCE) }
        try {
            fn()
        } catch (e: Exception) {
            val dialog = Alert(Alert.AlertType.ERROR)
            dialog.initOwner(stage)
            dialog.title = "Error: ${name}"
            if (details != null) {
                dialog.dialogPane.contentText = details
            }
            dialog.showAndWait()
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(ClientApplication::class.java, *args)
        }
    }
}
