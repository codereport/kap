package array.gui.reporting

import array.*
import array.gui.CalculationQueue
import array.gui.Client
import array.gui.reporting.edit.ResultEditor
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.geometry.Side
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.util.Callback

class ReportingClient {
    lateinit var client: Client
    lateinit var namespace: Namespace
    lateinit var variableList: ListView<Formula>
    lateinit var reportingHolder: Pane

    lateinit var editorWrapper: VBox
    lateinit var resultEditor: ResultEditor

    fun setupClient(client: Client) {
        this.client = client
        val engine = client.engine
        namespace = engine.makeNamespace("report")

        variableList.cellFactory = FormulaCellFactory(this)
        variableList.items = FXCollections.observableArrayList()
        variableList.items.addAll(
            listOf(
                Formula(namespace.internAndExport("foo"), "1"),
                Formula(namespace.internAndExport("blah"), "1+2")))

        resultEditor = ResultEditor.make()
        VBox.setVgrow(resultEditor.root, Priority.ALWAYS)
        editorWrapper.children.add(resultEditor.root)
    }

    private fun createDynamicBinding(engine: Engine, formulaResult: FormulaEditor.FormulaEditorResult) {
        val res = try {
            engine.withCurrentNamespace(namespace) {
                val src = "${formulaResult.name} dynamicequal (${formulaResult.expr}) ◊ '${formulaResult.name}"
                println("Creating binding using expression: ${src}")
                val result = engine.parseAndEval(
                    StringSourceLocation(src))
                if (result !is APLSymbol) {
                    println("Result is not an APLSymbol: ${result}")
                    return
                }
                result
            }
        } catch(e: APLGenericException) {
            println("Exception when creating dynamic binding")
            e.printStackTrace()
            return
        }
        Platform.runLater {
            val formula = Formula(res.value, formulaResult.expr)
            variableList.items.add(formula)
        }
    }

    fun addFormulaClicked(@Suppress("UNUSED_PARAMETER") actionEvent: ActionEvent) {
        val formulaResult = FormulaEditor.open(this)
        println("formula = ${formulaResult}")
        if (formulaResult != null) {
            client.calculationQueue.pushJobToQueue { engine ->
                engine.withCurrentNamespace(namespace) {
                    createDynamicBinding(engine, formulaResult)
                    System.gc()
                }
            }
        }
    }

    fun addVariableToPanel(formula: Formula) {
        val label = Label("foo")
        registerVariableListener(formula.name) { result ->
            label.text = result.formatted(FormatStyle.PRETTY)

        }
        reportingHolder.children.add(label)
        label.addEventHandler(MouseEvent.MOUSE_CLICKED, null)
    }

    fun addVariableToEditor(formula: Formula) {
        resultEditor.addInlineValue(this, formula)
    }

    fun registerVariableListener(name: Symbol, fn: (APLValue) -> Unit) {
        client.calculationQueue.pushJobToQueue(RegisterListenerRequest(name, fn))
    }

    class RegisterListenerRequest(val name: Symbol, val fn: (APLValue) -> Unit) : CalculationQueue.Request {
        override fun processRequest(engine: Engine) {
            engine.rootEnvironment.findBinding(name)?.let { b ->
                val holder = currentStack().findStorage(StackStorageRef(b))
                val current = holder.value()
                if (current != null) {
                    Platform.runLater {
                        fn(current)
                    }
                }
                holder.registerListener { newValue, oldValue ->
                    println("value updated: new=${newValue}, old=${oldValue}")
                    val result = newValue.collapse()
                    Platform.runLater {
                        fn(result)
                    }
                }
            }
        }
    }

    companion object {
        fun open(client: Client) {
            val loader = FXMLLoader(ReportingClient::class.java.getResource("reporting-client.fxml"))
            val root: Parent = loader.load()
            val controller: ReportingClient = loader.getController()
            controller.setupClient(client)

            val stage = Stage()
            val scene = Scene(root, 800.0, 800.0)
            stage.title = "Report"
            stage.scene = scene
            stage.show()
        }
    }
}

class Formula(val name: Symbol, val expr: String) {
    override fun toString() = "Formula(name=$name, expr='$expr')"
}

class FormulaListCellController {
    lateinit var reportingClient: ReportingClient
    lateinit var root: Parent
    lateinit var nameLabel: Label
    lateinit var expressionLabel: Label

    var content: Formula? = null

    fun initialize() {
        root.setOnContextMenuRequested { event ->
            makeContextMenu().show(root, Side.RIGHT, 0.0, 0.0)
        }
    }

    private fun makeContextMenu(): ContextMenu {
        val menu = ContextMenu()
        menu.items.addAll(
            MenuItem("Add to panel").apply {
                onAction = EventHandler {
                    content?.let(reportingClient::addVariableToPanel)
                }
            },
            MenuItem("Add to editor").apply {
                onAction = EventHandler {
                    content?.let(reportingClient::addVariableToEditor)
                }
            })
        return menu
    }

    fun updateContent(formula: Formula) {
        content = formula
        nameLabel.text = formula.name.symbolName
        expressionLabel.text = formula.expr
    }

    fun clearContent() {
        content = null
        nameLabel.text = ""
        expressionLabel.text = ""
    }
}

class FormulaListCell(reportingClient: ReportingClient) : ListCell<Formula>() {
    val controller: FormulaListCellController

    init {
        val loader = loader()
        val root = loader.load<Parent>()
        controller = loader.getController()
        controller.reportingClient = reportingClient
        graphic = root
    }

    override fun updateItem(item: Formula?, empty: Boolean) {
        super.updateItem(item, empty)
        if (empty) {
            controller.clearContent()
        } else {
            assertx(item != null)
            controller.updateContent(item)
        }
    }

    companion object {
        fun loader() = FXMLLoader(FormulaListCell::class.java.getResource("formula-list-cell.fxml"))
    }
}

class FormulaCellFactory(val reportingClient: ReportingClient) : Callback<ListView<Formula>, ListCell<Formula>> {
    override fun call(param: ListView<Formula>?): ListCell<Formula> {
        return FormulaListCell(reportingClient)
    }
}
