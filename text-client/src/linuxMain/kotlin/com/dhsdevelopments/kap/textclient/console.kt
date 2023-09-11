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
        engine.registerFunction(ns.internAndExport("home"), HomeFunction())
        engine.registerFunction(ns.internAndExport("bold"), EnableBoldFunction())
        engine.registerFunction(ns.internAndExport("inverse"), EnableInverseFunction())
        engine.registerFunction(ns.internAndExport("norm"), ResetAttrsFunction())
    }
}

@OptIn(ExperimentalForeignApi::class)
abstract class TerminalOpFunctionImpl(pos: FunctionInstantiation) : NoAxisAPLFunction(pos) {
    override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
        if (consoleInit) {
            val res = tigetstr(capability())
            if (res != null) {
                tputs(res.toKString(), 1, staticCFunction(::outputChar))
            }
        }
        return APLNullValue.APL_NULL_INSTANCE
    }

    abstract fun capability(): String
}

class ClearFunction : APLFunctionDescriptor {
    class ClearFunctionImpl(pos: FunctionInstantiation) : TerminalOpFunctionImpl(pos) {
        override fun capability() = "clear"
    }

    override fun make(instantiation: FunctionInstantiation) = ClearFunctionImpl(instantiation)
}

class HomeFunction : APLFunctionDescriptor {
    class HomeFunctionImpl(pos: FunctionInstantiation) : TerminalOpFunctionImpl(pos) {
        override fun capability() = "home"
    }

    override fun make(instantiation: FunctionInstantiation) = HomeFunctionImpl(instantiation)
}

class EnableBoldFunction : APLFunctionDescriptor {
    class EnableBoldFunctionImpl(pos: FunctionInstantiation) : TerminalOpFunctionImpl(pos) {
        override fun capability() = "bold"
    }

    override fun make(instantiation: FunctionInstantiation) = EnableBoldFunctionImpl(instantiation)
}

class EnableInverseFunction : APLFunctionDescriptor {
    class EnableInverseFunctionImpl(pos: FunctionInstantiation) : TerminalOpFunctionImpl(pos) {
        override fun capability() = "rev"
    }

    override fun make(instantiation: FunctionInstantiation) = EnableInverseFunctionImpl(instantiation)
}

class ResetAttrsFunction : APLFunctionDescriptor {
    class ResetAttrsFunctionImpl(pos: FunctionInstantiation) : TerminalOpFunctionImpl(pos) {
        override fun capability() = "sgr0"
    }

    override fun make(instantiation: FunctionInstantiation) = ResetAttrsFunctionImpl(instantiation)
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
