package com.dhsdevelopments.kap.textclient

import array.KeyboardInput
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import libedit.readline
import libedit.rl_pre_input_hook
import libedit.rl_startup_hook
import platform.posix.free

private var initialised = false

@OptIn(ExperimentalForeignApi::class)
class LibinputKeyboardInput : KeyboardInput {
    init {
        if (!initialised) {
            rl_startup_hook = staticCFunction(::libinputStartup)
            rl_pre_input_hook = staticCFunction(::handlePreInput)
//            rl_event_hook = staticCFunction(::handleEvent)
            initialised = true
        }
    }

    override fun readString(prompt: String): String? {
        val res = readline(prompt)
        return if (res == null) {
            null
        } else {
            try {
                res.toKString()
            } finally {
                free(res)
            }
        }
    }
}

private fun libinputStartup(): Int {
    return 0
}

private fun handlePreInput(): Int {
    println("Hook")
    return 0
}

private fun handleEvent(): Int {
    println("event")
    return 0
}
