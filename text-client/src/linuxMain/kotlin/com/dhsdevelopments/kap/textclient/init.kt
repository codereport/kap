package com.dhsdevelopments.kap.textclient

import array.repl.runRepl

fun main(args: Array<String>) {
    runRepl(args, keyboardInput = LibinputKeyboardInput())
}
