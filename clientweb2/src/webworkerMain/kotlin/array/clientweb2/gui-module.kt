package array.clientweb2

import array.*
import org.khronos.webgl.Uint8ClampedArray
import org.w3c.dom.ImageData

class JsGuiModule(val sendMessageFn: (dynamic) -> Unit) : KapModule {
    override val name = "gui"

    override fun init(engine: Engine) {
        val ns = engine.makeNamespace("gui")
        engine.registerFunction(ns.internAndExport("create"), JsGuiCreateFunction())
        engine.registerFunction(ns.internAndExport("draw"), JsGuiDrawFunction())
    }
}

class GuiWindow : APLSingleValue() {
    val id = currentId++

    override val aplValueType get() = APLValueType.INTERNAL
    override fun formatted(style: FormatStyle) = "guiWindow"
    override fun compareEquals(reference: APLValue) = reference is GuiWindow && id == reference.id
    override fun makeKey() = APLValueKeyImpl(this, id)

    companion object {
        var currentId = 0
    }
}

private fun guiWindowFromAPLValue(v: APLValue, pos: Position): GuiWindow {
    val v0 = v.collapse()
    if (v0 !is GuiWindow) {
        throwAPLException(APLIllegalArgumentException("Argument is not a gui reference", pos))
    }
    return v0
}

class JsGuiCreateFunction : APLFunctionDescriptor {
    class JsGuiCreateFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            println("create called: ${a}")
            if (!a.dimensions.compareEquals(dimensionsOfSize(2))) {
                throwAPLException(APLIllegalArgumentException("Argument must be a 1-dimensional array of size 2", pos))
            }
            val width = a.valueAtInt(0, pos)
            val height = a.valueAtInt(1, pos)
            val result = GuiWindow()
            val module = context.engine.findModule<JsGuiModule>() ?: throw IllegalStateException("Chart module not found")
            module.sendMessageFn(makeWindowCreatedMessage(result.id, width, height))
            return result
        }
    }

    override fun make(instantiation: FunctionInstantiation) = JsGuiCreateFunctionImpl(instantiation)
}

private fun makeWindowCreatedMessage(id: Int, width: Int, height: Int): dynamic {
    val res = js("{}")
    res.messageType = WINDOW_CREATED_TYPE
    res.id = id
    res.width = width
    res.height = height
    return res
}

class JsGuiDrawFunction : APLFunctionDescriptor {
    class JsGuiDrawFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
            val win = guiWindowFromAPLValue(a, pos)
            val b0 = b.collapse()
            val b0Dimensions = b0.dimensions
            if (b0Dimensions.size != 2) {
                throwAPLException(APLIllegalArgumentException("Only 2-dimensional arrays are currently supported", pos))
            }
            val height = b0Dimensions[0]
            val width = b0Dimensions[1]
            val content = Uint8ClampedArray(width * height * 4)
            var i = 0
            b0.iterateMembers { v ->
                @Suppress("UNUSED_VARIABLE")
                val member = (b0.valueAtDouble(i / 4, pos) * 255).toInt() and 0xFF
                js("content[i] = member; content[i+1] = member; content[i+2] = member; content[i+3] = 255")
                i += 4
            }
            val data = ImageData(content, width, height)
            val module = context.engine.findModule<JsGuiModule>() ?: throw IllegalStateException("Chart module not found")
            module.sendMessageFn(makeImageContentMessage(win.id, data))
            return b0
        }
    }

    override fun make(instantiation: FunctionInstantiation) = JsGuiDrawFunctionImpl(instantiation)
}

private fun makeImageContentMessage(id: Int, data: ImageData): dynamic {
    val res = js("{}")
    res.messageType = IMAGE_CONTENT_TYPE
    res.id = id
    res.data = data
    return res
}
