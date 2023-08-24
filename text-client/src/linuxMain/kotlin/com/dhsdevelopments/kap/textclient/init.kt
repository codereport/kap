@file:OptIn(ExperimentalForeignApi::class)

package com.dhsdevelopments.kap.textclient

import array.Engine
import array.repl.runRepl
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.staticCFunction
import platform.posix.LC_ALL
import platform.posix.SIGINT
import platform.posix.setlocale
import platform.posix.signal

private var engineInst: Engine? = null

@OptIn(ExperimentalForeignApi::class)
fun main(args: Array<String>) {
    setlocale(LC_ALL, "")
    runRepl(args, keyboardInput = LibinputKeyboardInput()) { engine ->
        memScoped {
            if (engineInst != null) {
                throw IllegalStateException("Multiple repls not allowed")
            }
            engineInst = engine
            signal(SIGINT, staticCFunction(::sigHandler))
        }
    }
}

private fun sigHandler(n: Int) {
    val engine = engineInst
    if (engine != null) {
        engine.interruptEvaluation()
    }
}
