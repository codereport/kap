package array.gui.graphics

import array.*
import array.gui.ResizableCanvas
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.image.WritableImage
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import javafx.scene.text.Font
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

    fun updateContent(imageData: ImageData) {
        Platform.runLater {
            window.repaintContent(imageData)
        }
    }
}

class MakeGraphicFunction : APLFunctionDescriptor {
    class MakeGraphicFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
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

    override fun make(pos: Position) = MakeGraphicFunctionImpl(pos)
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
    class DrawGraphicFunctionImpl(pos: Position) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val v = a.unwrapDeferredValue()
            if (v !is GraphicWindowAPLValue) {
                throw APLIncompatibleDomainsException("Left argument must be a graphic object", pos)
            }
            val bDimensions = b.dimensions
            when (bDimensions.size) {
                2 -> {
                    v.updateContent(ImageData(b.toDoubleArray(pos), bDimensions[1], bDimensions[0], false))
                }
                3 -> {
                    if (bDimensions[2] != 3) {
                        throwAPLException(APLIllegalArgumentException("Colour arrays needs must have a third dimension of size 3", pos))
                    }
                    v.updateContent(ImageData(b.toDoubleArray(pos), bDimensions[1], bDimensions[0], true))
                }
                else -> {
                    throw InvalidDimensionsException("Right argument must be 2 or 3-dimensional", pos)
                }
            }

            return b
        }
    }

    override fun make(pos: Position) = DrawGraphicFunctionImpl(pos)
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

    private inner class Content(width: Int, height: Int) {
        val stage = Stage()
        val canvas: ResizableCanvas
        var image: WritableImage
        var array: ImageData? = null

        init {
            image = WritableImage(width, height)
            canvas = ResizableCanvas()
            canvas.width = width.toDouble()
            canvas.height = height.toDouble()
            canvas.addOnSizeChangeCallback {
                createImage(canvas.width.toInt(), canvas.height.toInt())
            }
            val border = BorderPane().apply {
                center = canvas
            }
            val scene = Scene(border, width.toDouble(), height.toDouble())
            scene.addEventFilter(KeyEvent.KEY_PRESSED, ::processKeyPress)
            stage.scene = scene
            stage.show()
        }

        private fun createImage(width: Int, height: Int) {
            image = WritableImage(width, height)
            array?.let { a ->
                updateArrayAndRepaint(a)
            }
        }

        fun updateArrayAndRepaint(imageData: ImageData) {
            array = imageData
            if (!imageData.arrayIs3D) {
                repaintCanvas2D()
            } else {
                repaintCanvas3D()
            }
        }

        private fun repaintCanvas2D() {
            val arrayValues = array!!.array
            val width = array!!.width
            val height = array!!.height

            assert(arrayValues.size == width * height)
            val imageWidth = image.width
            val imageHeight = image.height
            val xStride = width.toDouble() / imageWidth
            val yStride = height.toDouble() / imageHeight
            val pixelWriter = image.pixelWriter
            for (y in 0 until imageHeight.toInt()) {
                for (x in 0 until imageWidth.toInt()) {
                    val value = arrayValues[(y * yStride).toInt() * width + (x * xStride).toInt()]
                    val valueByte = min(max(value * 256, 0.0), 255.0).toInt() and 0xFF
                    val colour = (0xFF shl 24) or (valueByte shl 16) or (valueByte shl 8) or valueByte
                    pixelWriter.setArgb(x, y, colour)
                }
            }
            canvas.graphicsContext2D.drawImage(image, 0.0, 0.0)
            if(settings.labels) {
                drawCellValues()
            }
        }

        private fun repaintCanvas3D() {
            val imageData = array!!.array
            val width = array!!.width
            val height = array!!.height

            assert(imageData.size == width * height * 3)
            val lineWidth = width * 3
            val imageWidth = image.width
            val imageHeight = image.height
            val xStride = width.toDouble() / imageWidth
            val yStride = height.toDouble() / imageHeight
            val pixelWriter = image.pixelWriter
            for (y in 0 until imageHeight.toInt()) {
                for (x in 0 until imageWidth.toInt()) {
                    val offset = (y * yStride).toInt() * lineWidth + (x * xStride).toInt() * 3
                    val valueR = min(max((imageData[offset] * 255).toInt(), 0), 255) and 0xFF
                    val valueG = min(max((imageData[offset + 1] * 255).toInt(), 0), 255) and 0xFF
                    val valueB = min(max((imageData[offset + 2] * 255).toInt(), 0), 255) and 0xFF
                    val colour = (0xFF shl 24) or (valueR shl 16) or (valueG shl 8) or valueB
                    pixelWriter.setArgb(x, y, colour)
                }
            }
            canvas.graphicsContext2D.drawImage(image, 0.0, 0.0)
        }

        private fun drawCellValues() {
            val imageData = array!!.array
            val width = array!!.width
            val height = array!!.height

            val cellWidth = image.width / width
            val cellHeight = image.height / height
            val c = canvas.graphicsContext2D
            c.font = Font.font("Sans Serif", cellHeight / 2)
            if (cellWidth > 2 && cellHeight > 2) {
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val value = imageData[y * height + x]
                        c.fillText(String.format("%f", value), x * cellWidth, y * cellHeight)
                    }
                }
            }
        }

        private fun processKeyPress(event: KeyEvent) {
            publishEventIfEnabled(KapKeyEvent(event))
        }
    }

    fun repaintContent(imageData: ImageData) {
        content.get()?.updateArrayAndRepaint(imageData)
    }

    data class Settings(val labels: Boolean = false)
}

class GuiModule : KapModule {
    override val name get() = "gui"

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
}
