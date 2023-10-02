package array

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.LineUnavailableException
import javax.sound.sampled.SourceDataLine
import kotlin.math.max
import kotlin.math.min

class JvmAudioModule : KapModule {
    override val name get() = "jvmaudio"

    private var source: SourceDataLine? = null
    private var initHasBeenCalled = false

    override fun init(engine: Engine) {
        val ns = engine.makeNamespace("audio")
        engine.registerFunction(ns.internAndExport("play"), JvmPlayAudioFunction(this))
    }

    override fun close() {
        val src = source
        if (src != null) {
            src.stop()
            src.close()
        }
    }

    fun playBuffer(a: APLValue, pos: Position) {
        val d = a.dimensions
        if (d.size != 1) {
            throwAPLException(InvalidDimensionsException("Argument must be a 1-dimensional array", pos))
        }
        initAudio()
        val src = source
        if (src != null) {
            val size = d[0]
            val buf = ByteArray(size) { i ->
                val v = max(min(a.valueAtDouble(i, pos), 1.0), -1.0)
                (v * 127).toInt().toByte()
            }
            src.write(buf, 0, size)
        }
    }

    private fun initAudio() {
        if (!initHasBeenCalled) {
            initHasBeenCalled = true
            val format = AudioFormat(48000f, 8, 1, true, false)
            val s = try {
                AudioSystem.getSourceDataLine(format)
            } catch (e: LineUnavailableException) {
                println("Warning: Line unavailable")
                return
            } catch (e: IllegalArgumentException) {
                println("Warning: Illegal audio format selected")
                return
            }
            s.open(format)
            s.start()
            source = s
        }
    }
}

class JvmPlayAudioFunction(val owner: JvmAudioModule) : APLFunctionDescriptor {
    inner class JvmPlayAudioFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            owner.playBuffer(a, pos)
            return APLNullValue.APL_NULL_INSTANCE
        }
    }

    override fun make(instantiation: FunctionInstantiation) = JvmPlayAudioFunctionImpl(instantiation)
}
