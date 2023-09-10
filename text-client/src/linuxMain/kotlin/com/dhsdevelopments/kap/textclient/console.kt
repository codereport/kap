package com.dhsdevelopments.kap.textclient

import array.*
import kotlinx.cinterop.*
import ncurses.tigetstr
import ncurses.tputs
import platform.posix.write

class TerminalModule : KapModule {
    override val name: String get() = "term"

    override fun init(engine: Engine) {
        val ns = engine.makeNamespace("term")
        engine.registerFunction(ns.internAndExport("clear"), ClearFunction())
    }
}

class ClearFunction : APLFunctionDescriptor {
    @OptIn(ExperimentalForeignApi::class)
    class ClearFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
        override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
            if (consoleInit) {
                val clearScreen = tigetstr("clear")
                if (clearScreen != null) {
                    tputs(clearScreen.toKString(), 1, staticCFunction(::outputChar))
                }
            }
            return APLNullValue.APL_NULL_INSTANCE
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ClearFunctionImpl(instantiation)
}

@OptIn(ExperimentalForeignApi::class)
private fun outputChar(ch: Int): Int {
    memScoped {
        val buffer = allocArray<ByteVar>(1)
        buffer[0] = ch.toByte()
        write(1, buffer, 1UL)
        return 1
    }
}
