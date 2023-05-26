package com.dhsdevelopments.kap.gui2

import array.CharacterOutput
import array.Engine
import array.StringSourceLocation
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedTransferQueue

class ComputeQueue {
    private val engine = Engine()
    private val queue = LinkedTransferQueue<Request>()
    private val thread: Thread
    private val standardOutputListeners = CopyOnWriteArrayList<StdoutListener>()

    init {
        engine.addLibrarySearchPath("../array/standard-lib")
        engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"))
        engine.standardOutput = CharacterOutput(::writeStringToListeners)

        thread = object : Thread("ComputeQueue") {
            override fun run() {
                calcLoop()
            }
        }
        thread.start()
    }

    private fun writeStringToListeners(s: String) {
        standardOutputListeners.forEach { listener -> listener.writeString(s) }
    }

    private fun calcLoop() {
        println("Starting calculation loop")
        while (!Thread.interrupted()) {
            val request = queue.take()
            request.fn(engine)
        }
        println("Stopping calculation loop")
    }

    fun requestJob(request: Request) {
        queue.put(request)
    }

    fun addStandardOutputListener(listener: StdoutListener) = standardOutputListeners.add(listener)
    fun removeStandardOutputListener(listener: StdoutListener) = standardOutputListeners.remove(listener)
}

class Request(val fn: (Engine) -> Unit)

fun interface StdoutListener {
    fun writeString(s: String)
}
