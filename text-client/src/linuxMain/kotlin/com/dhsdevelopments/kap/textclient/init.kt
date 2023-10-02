@file:OptIn(ExperimentalForeignApi::class)

package com.dhsdevelopments.kap.textclient

import array.Engine
import array.repl.runRepl
import kotlinx.cinterop.*
import ncurses.OK
import ncurses.setupterm
import platform.posix.*

private var engineInst: Engine? = null
var consoleInit: Boolean = false

@OptIn(ExperimentalForeignApi::class)
fun main(args: Array<String>) {
    setlocale(LC_ALL, "")
    initConsole()
    runRepl(args, keyboardInput = LibinputKeyboardInput()) { engine ->
        if (engineInst != null) {
            throw IllegalStateException("Multiple repls not allowed")
        }
        engine.addModule(TerminalModule())
        engineInst = engine
        signal(SIGINT, staticCFunction(::sigHandler))
    }
}

private fun sigHandler(@Suppress("UNUSED_PARAMETER") n: Int) {
    engineInst?.interruptEvaluation()
}

@OptIn(ExperimentalForeignApi::class)
private fun initConsole() {
    memScoped {
        val ret = allocArray<IntVar>(1)
        val termValue = getenv("TERM")
        if (termValue != null) {
            if (setupterm(termValue.toKString(), 1, ret) == OK) {
                consoleInit = true
            }
        }
    }
}
