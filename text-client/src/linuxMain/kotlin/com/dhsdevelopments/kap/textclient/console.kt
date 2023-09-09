package com.dhsdevelopments.kap.textclient

import array.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import ncurses.putp
import ncurses.tigetstr

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
                    putp(clearScreen.toKString())
                }
            }
            return APLNullValue.APL_NULL_INSTANCE
        }
    }

    override fun make(instantiation: FunctionInstantiation) = ClearFunctionImpl(instantiation)
}
