package array

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TimerJob

class TimerThread(val handler: JvmTimerHandler) : Thread("Timer thread") {
    val queue = ArrayDeque<TimerJob>()

    override fun run() {
        var stopped = false
        while (!stopped) {
            val job = handler.timerThreadLock.withLock {
                val element = queue.removeFirstOrNull()
                if (element == null) {
                    handler.timerThread = null
                    stopped = true
                }
                element
            }
            // Ugly double check because we can't break out of the loop above since we're in an inline block
            if (job == null) {
                break
            }

        }
    }

    fun registerJob(engine: Engine, delays: IntArray, callbacks: List<LambdaValue>) {
        TODO("not implemented")
    }
}

class JvmTimerHandler(val engine: Engine) : TimerHandler {
    val timerThreadLock = ReentrantLock()
    var timerThread: TimerThread? = null

    override fun registerTimer(delays: IntArray, callbacks: List<LambdaValue>): APLValue {
        val timer = timerThreadLock.withLock {
            val ref = timerThread
            val th = if (ref == null) {
                val v = TimerThread(this)
                timerThread = v
                v.start()
                v
            } else {
                ref
            }
            th.registerJob(engine, delays, callbacks)
        }
        TODO("need to implement")
    }
}

actual fun makeTimerHandler(engine: Engine): TimerHandler? = JvmTimerHandler(engine)
