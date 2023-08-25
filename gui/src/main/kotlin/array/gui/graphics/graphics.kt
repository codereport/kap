package array.gui.graphics

import array.*
import array.gui.ResizableCanvas
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.image.WritableImage
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

class ImageData(
    val array: DoubleArray,
    val width: Int,
    val height: Int,
    val arrayIs3D: Boolean
)

class GraphicWindowAPLValue(engine: Engine, width: Int, height: Int, settings: GraphicWindow.Settings) : APLSingleValue() {
    val window = GraphicWindow(engine, width, height, settings)

    override val aplValueType: APLValueType
        get() = APLValueType.INTERNAL

    override fun formatted(style: FormatStyle) = "graphic-window"
    override fun compareEquals(reference: APLValue) = reference is GraphicWindowAPLValue && window === reference.window
    override fun makeKey() = APLValueKeyImpl(this, window)

    init {
        val module = engine.findModuleOrError<GuiModule>()
        module.registeredWindows.add(this)
    }

    fun updateContent(image: WritableImage) {
        Platform.runLater {
            window.repaintContent(image)
        }
    }
}

class MakeGraphicFunction : APLFunctionDescriptor {
    class MakeGraphicFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            return makeWindow(a, context.engine, GraphicWindow.Settings())
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val keywords = arrayToKeywords(context.engine, a, pos, listOf("labels"))
            return makeWindow(b, context.engine, GraphicWindow.Settings(labels = keywords.contains("labels")))
        }

        private fun makeWindow(a: APLValue, engine: Engine, settings: GraphicWindow.Settings): GraphicWindowAPLValue {
            val aDimensions = a.dimensions
            if (aDimensions.size != 1 || aDimensions[0] != 2) {
                throw InvalidDimensionsException("Argument must be a two-element vector")
            }
            val width = a.valueAtInt(0, pos)
            val height = a.valueAtInt(1, pos)
            return GraphicWindowAPLValue(engine, width, height, settings)
        }
    }

    override fun make(instantiation: FunctionInstantiation) = MakeGraphicFunctionImpl(instantiation)
}

private fun arrayToKeywords(engine: Engine, a: APLValue, pos: Position, allowed: List<String>): Set<String> {
    val a0 = a.arrayify()
    val aDimensions = a0.dimensions
    if (aDimensions.size != 1) {
        throwAPLException(APLIllegalArgumentException("Left argument must be a scalar or 1-dimensional array", pos))
    }
    return a0.membersSequence().map { v ->
        val sym = v.ensureSymbol(pos).value
        if (sym.namespace != engine.keywordNamespace) {
            throwAPLException(APLIllegalArgumentException("Left arguments must be in keyword namespace: ${sym}", pos))
        }
        if (!allowed.contains(sym.symbolName)) {
            throwAPLException(APLIllegalArgumentException("Unexpected keyword: ${sym}", pos))
        }
        sym.symbolName
    }.toSet()
}

class DrawGraphicFunction : APLFunctionDescriptor {
    class DrawGraphicFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val module = context.engine.findModule<GuiModule>() ?: throwAPLException(APLEvalException("gui module not available", pos))
            val w = module.defaultGuiWindow
            val window = if (w == null) {
                val w0 = GraphicWindowAPLValue(context.engine, 400, 400, GraphicWindow.Settings())
                module.defaultGuiWindow = w0
                w0
            } else {
                w
            }
            renderArray(window, a)
            return a
        }

        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val v = a.unwrapDeferredValue()
            if (v !is GraphicWindowAPLValue) {
                throw APLIncompatibleDomainsException("Left argument must be a graphic object", pos)
            }
            renderArray(v, b)
            return b
        }

        private fun renderArray(v: GraphicWindowAPLValue, b: APLValue) {
            v.updateContent(renderArrayToImage(b, pos))
        }
    }

    override fun make(instantiation: FunctionInstantiation) = DrawGraphicFunctionImpl(instantiation)
}

private fun renderArrayToImage(b: APLValue, pos: Position): WritableImage {
    val bDimensions = b.dimensions
    return when (bDimensions.size) {
        2 -> {
            val width = bDimensions[1]
            val height = bDimensions[0]
            val image = WritableImage(width, height)
            val pixelWriter = image.pixelWriter
            var p = 0
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val value = b.valueAtDouble(p++, pos)
                    val valueByte = min(max(value * 256, 0.0), 255.0).toInt() and 0xFF
                    val colour = (0xFF shl 24) or (valueByte shl 16) or (valueByte shl 8) or valueByte
                    pixelWriter.setArgb(x, y, colour)
                }
            }
            image
        }
        3 -> {
            if (bDimensions[2] != 3) {
                throwAPLException(APLIllegalArgumentException("Colour arrays needs must have a third dimension of size 3", pos))
            }
            val width = bDimensions[1]
            val height = bDimensions[0]
            val image = WritableImage(width, height)
            val pixelWriter = image.pixelWriter
            var p = 0
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val d0 = b.valueAtDouble(p++, pos)
                    val d1 = b.valueAtDouble(p++, pos)
                    val d2 = b.valueAtDouble(p++, pos)
                    val valueR = min(max((d0 * 255).toInt(), 0), 255) and 0xFF
                    val valueG = min(max((d1 * 255).toInt(), 0), 255) and 0xFF
                    val valueB = min(max((d2 * 255).toInt(), 0), 255) and 0xFF
                    val colour = (0xFF shl 24) or (valueR shl 16) or (valueG shl 8) or valueB
                    pixelWriter.setArgb(x, y, colour)
                }
            }
            image
        }
        else -> throwAPLException(APLIllegalArgumentException("Right argument must be 2 or 3-dimensional", pos))
    }
}

typealias EventType = KClass<out KapWindowEvent>

class GraphicWindow(val engine: Engine, width: Int, height: Int, val settings: Settings) {
    private var content = AtomicReference<Content?>()
    private val events = LinkedList<KapWindowEvent>()
    private val enabledEvents = HashSet<EventType>()
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    init {
        Platform.runLater {
            content.set(Content(width, height))
        }
    }

    fun close() {
        Platform.runLater {
            val c = content.get()
            if (c != null) {
                c.stage.close()
            }
        }
    }

    fun nextEvent(): KapWindowEvent? {
        return lock.withLock {
            events.poll()
        }
    }

    fun waitForEvent(): KapWindowEvent {
        lock.withLock {
            while (events.isEmpty()) {
                condition.await()
            }
            return events.removeFirst()
        }
    }

    fun addEnabledEvent(v: EventType): Boolean {
        return lock.withLock {
            enabledEvents.add(v)
        }
    }

    fun removeEnabledEvent(v: EventType): Boolean {
        return lock.withLock {
            enabledEvents.remove(v)
        }
    }

    fun publishEventIfEnabled(event: KapWindowEvent) {
        lock.withLock {
            if (enabledEvents.contains(event::class)) {
                events.add(event)
                condition.signal()
            }
        }
    }

    /*
    L ⇐ {⊃1 ⍵ ∨.∧ 3 4 = +/ , ¯1 0 1 ⊖⌻ ¯1 0 1 ⌽¨ ⊂⍵}
    curr←?1000 1000⍴2 ◊ while(1) { g gui:draw curr←L curr ◊ time:sleep 0.001 }
     */

    private inner class Content(width: Int, height: Int) {
        val stage = Stage()
        val canvas: ResizableCanvas
        var currentImage: WritableImage? = null

        init {
            canvas = ResizableCanvas()
            canvas.width = width.toDouble()
            canvas.height = height.toDouble()
            canvas.addOnSizeChangeCallback {
                repaintImage()
            }
            val border = BorderPane().apply {
                center = canvas
            }
            val scene = Scene(border, width.toDouble(), height.toDouble())
            scene.addEventFilter(KeyEvent.KEY_PRESSED, ::processKeyPress)
            stage.scene = scene
            stage.show()
        }

        fun updateArrayAndRepaint(image: WritableImage) {
            currentImage = image
            repaintImage()
        }

        fun repaintImage() {
            val image = currentImage
            if (image != null) {
                canvas.graphicsContext2D.isImageSmoothing = false
                canvas.graphicsContext2D.drawImage(image, 0.0, 0.0, image.width, image.height, 0.0, 0.0, canvas.width, canvas.height)
            }
        }

//        private fun drawCellValues() {
//            val imageData = array!!.array
//            val width = array!!.width
//            val height = array!!.height
//
//            val cellWidth = image.width / width
//            val cellHeight = image.height / height
//            val c = canvas.graphicsContext2D
//            c.font = Font.font("Sans Serif", cellHeight / 2)
//            if (cellWidth > 2 && cellHeight > 2) {
//                for (y in 0 until height) {
//                    for (x in 0 until width) {
//                        val value = imageData[y * height + x]
//                        c.fillText(String.format("%f", value), x * cellWidth, y * cellHeight)
//                    }
//                }
//            }
//        }

        private fun processKeyPress(event: KeyEvent) {
            publishEventIfEnabled(KapKeyEvent(event))
        }
    }

    fun repaintContent(image: WritableImage) {
        content.get()?.updateArrayAndRepaint(image)
    }

    data class Settings(val labels: Boolean = false)
}

class GuiModule : KapModule {
    override val name get() = "gui"
    var defaultGuiWindow: GraphicWindowAPLValue? = null
    val registeredWindows = ArrayList<GraphicWindowAPLValue>()

    override fun init(engine: Engine) {
        val guiNamespace = engine.makeNamespace("gui")

        fun addFn(name: String, fn: APLFunctionDescriptor) {
            engine.registerFunction(guiNamespace.internAndExport(name), fn)
        }

        addFn("create", MakeGraphicFunction())
        addFn("draw", DrawGraphicFunction())
        addFn("nextEvent", ReadEventFunction())
        addFn("nextEventBlocking", ReadEventBlockingFunction())
        addFn("enableEvents", EnableEventsFunction())
        addFn("disableEvents", DisableEventsFunction())
    }

    override fun close() {
        registeredWindows.forEach { v ->
            v.window.close()
        }
    }
}
