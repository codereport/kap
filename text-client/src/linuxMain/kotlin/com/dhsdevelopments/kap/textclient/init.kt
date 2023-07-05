@file:OptIn(ExperimentalForeignApi::class)

package com.dhsdevelopments.kap.textclient

import array.repl.runRepl
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.LC_ALL
import platform.posix.setlocale

fun main(args: Array<String>) {
    setlocale(LC_ALL, "")
    runRepl(args, keyboardInput = LibinputKeyboardInput())
}
