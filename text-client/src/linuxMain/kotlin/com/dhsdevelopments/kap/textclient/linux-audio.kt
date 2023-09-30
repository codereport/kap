package com.dhsdevelopments.kap.textclient

import alsa.*
import array.*
import kotlinx.cinterop.*
import kotlin.math.max
import kotlin.math.min

class AudioPlayerException(message: String, pos: Position? = null) : APLEvalException(message, pos)

@OptIn(ExperimentalForeignApi::class)
class LinuxAudioModule : KapModule {
    override val name get() = "linuxaudio"

    private var initRun = false
    private var handle: CPointer<snd_pcm_t>? = null

    override fun init(engine: Engine) {
        val ns = engine.makeNamespace("audio")
        engine.registerFunction(ns.internAndExport("play"), LinuxPlayAudioFunction(this))
    }

    override fun close() {
        if (handle != null) {
            snd_pcm_close(handle)
        }
    }

    private fun initAudio() {
        if (initRun) {
            return
        }
        initRun = true
        memScoped {
            val handleRef = allocArray<CPointerVarOf<CPointer<snd_pcm_t>>>(1)
            snd_pcm_open(handleRef, "default", SND_PCM_STREAM_PLAYBACK, 0).let { res ->
                if (res < 0) {
                    val message = snd_strerror(res)?.toKString() ?: "unknown"
                    println("Warning: Unable to initialise audio: ${message}")
                    return
                }
            }
            val handleResult = handleRef[0]
            snd_pcm_set_params(handleResult, SND_PCM_FORMAT_U8, SND_PCM_ACCESS_RW_INTERLEAVED, 1U, 48000U, 1, 500000U).let { res ->
                if (res < 0) {
                    val message = snd_strerror(res)?.toKString() ?: "unknown"
                    println("Warning: Failed to open audio output: ${message}")
                    return
                }
            }
            handle = handleResult
        }
    }

    fun playBuffer(a: APLValue, pos: Position) {
        initAudio()
        if (handle == null) {
            return
        }
        val d = a.dimensions
        if (d.size != 1) {
            throwAPLException(InvalidDimensionsException("Argument must be a 1-dimensional array", pos))
        }
        memScoped {
            val size = d[0]
            val buf = allocArray<UByteVar>(size)
            repeat(size) { i ->
                val v = max(min(a.valueAtDouble(i, pos), 1.0), -1.0)
                buf[i] = ((v * 127) + 128).toInt().toUByte()
            }
            val frames = snd_pcm_writei(handle, buf, size.toULong())
            if (frames < 0) {
                val f = snd_pcm_recover(handle, frames.toInt(), 0)
                if (f < 0) {
                    println("Warning: short write. expected ${size}, written ${f}")
                }
            }
            snd_pcm_drain(handle).let { res ->
                if (res < 0) {
                    val message = snd_strerror(res)?.toKString() ?: "unknown"
                    println("Warning: PCM drain failed: ${message}")
                    return
                }
            }
        }
    }
}

class LinuxPlayAudioFunction(val owner: LinuxAudioModule) : APLFunctionDescriptor {
    inner class LinuxPlayAudioFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            owner.playBuffer(a, pos)
            return APLNullValue.APL_NULL_INSTANCE
        }
    }

    override fun make(instantiation: FunctionInstantiation) = LinuxPlayAudioFunctionImpl(instantiation)
}
