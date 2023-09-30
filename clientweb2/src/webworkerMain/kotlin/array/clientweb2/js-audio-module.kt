package array.clientweb2

import array.*
import kotlin.math.max
import kotlin.math.min

class JsAudioModule(val sendMessageFn: (dynamic) -> Unit) : KapModule {
    override val name get() = "jsaudio"

    override fun init(engine: Engine) {
        val ns = engine.makeNamespace("audio")
        engine.registerFunction(ns.internAndExport("play"), PlayAudioFunction(this))
    }
}

class PlayAudioFunction(val owner: JsAudioModule) : APLFunctionDescriptor {
    inner class PlayAudioFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            val d = a.dimensions
            if (d.size != 1) {
                throwAPLException(InvalidDimensionsException("Argument must be a 1-dimensional array", pos))
            }
            @Suppress("UNUSED_VARIABLE")
            val size = d[0]
            val jsBuffer = js("new Array(size)")
            repeat(size) { i ->
                jsBuffer[i] = max(min(a.valueAtDouble(i, pos), 1.0), -1.0)
            }
            owner.sendMessageFn(makePlayAudioMessage(jsBuffer))
            return APLNullValue.APL_NULL_INSTANCE
        }
    }

    override fun make(instantiation: FunctionInstantiation) = PlayAudioFunctionImpl(instantiation)
}

private fun makePlayAudioMessage(buffer: dynamic): dynamic {
    val res = js("{}")
    res.messageType = PLAY_AUDIO_TYPE
    res.buffer = buffer
    return res
}
