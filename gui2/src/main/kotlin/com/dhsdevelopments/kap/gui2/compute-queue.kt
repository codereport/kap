package com.dhsdevelopments.kap.gui2

import array.CharacterOutput
import array.Engine
import array.StringSourceLocation
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class ComputeQueue {
    private val engine = Engine()
    private val queue = ArrayDeque<Request>()
    private val queueLock = ReentrantLock()
    private val queueCond = queueLock.newCondition()
    private var stopped = false
    private val thread: Thread

    private val standardOutputListeners = CopyOnWriteArrayList<StdoutListener>()

    init {
        engine.addModule(Gui2Module())
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
        try {
            while (true) {
                val request = queueLock.withLock {
                    while (!stopped && queue.isEmpty()) {
                        queueCond.await()
                    }
                    if (stopped) {
                        null
                    } else {
                        queue.removeFirst()
                    }
                }
                if (request == null) {
                    break
                }
                request.fn(engine)
            }
        } catch (e: InterruptedException) {
            println("Interrupted, closing calculation queue")
        }
        println("Stopping engine")
        engine.close()
    }

    fun stop() {
        queueLock.withLock {
            queue.clear()
            stopped = true
            queueCond.signal()
        }
        thread.interrupt()
        engine.interruptEvaluation()
        thread.join()
    }

    fun requestJob(request: Request) {
        queueLock.withLock {
            if (!stopped) {
                queue.add(request)
                queueCond.signal()
            }
        }
    }

    fun addStandardOutputListener(listener: StdoutListener) = standardOutputListeners.add(listener)
    fun removeStandardOutputListener(listener: StdoutListener) = standardOutputListeners.remove(listener)
}

class Request(val fn: (Engine) -> Unit)

fun interface StdoutListener {
    fun writeString(s: String)
}
