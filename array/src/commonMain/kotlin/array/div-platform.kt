package array

import kotlin.reflect.KClass

expect fun sleepMillis(time: Long)

interface MPAtomicRefArray<T> {
    operator fun get(index: Int): T?

    fun compareAndExchange(index: Int, expected: T?, newValue: T?): T?
}

inline fun <T> MPAtomicRefArray<T>.checkOrUpdate(index: Int, fn: () -> T): T {
    val old = get(index)
    if (old != null) {
        return old
    }
    val update = fn()
    return compareAndExchange(index, null, update) ?: update
}

expect fun <T> makeAtomicRefArray(size: Int): MPAtomicRefArray<T>

interface MPThreadLocal<T> {
    var value: T?
}

expect fun <T : Any> makeMPThreadLocalBackend(type: KClass<T>): MPThreadLocal<T>

inline fun <reified T : Any> makeMPThreadLocal(): MPThreadLocal<T> {
    return makeMPThreadLocalBackend(T::class)
}

/**
 * Format a double in a standardised way. A value with zero decimal part should be rendered as 4.0.
 * This is needed because Javascript does not include the decimal by default.
 */
expect fun Double.formatDouble(): String

/**
 * Return the current time in number of milliseconds
 */
expect fun currentTime(): Long

class RegexpParseException(message: String, cause: Throwable) : Exception(message, cause)

expect fun toRegexpWithException(string: String, options: Set<RegexOption>): Regex

expect class MPLock()

expect inline fun <T> MPLock.withLocked(fn: () -> T): T

expect fun numCores(): Int

interface BackgroundTask<T> {
    fun await(): T
}

interface MPThreadPoolExecutor {
    val numThreads: Int
    fun <T> start(fn: () -> T): BackgroundTask<T>
    fun close()
}

class SingleThreadedThreadPoolExecutor : MPThreadPoolExecutor {
    override val numThreads get() = 1

    override fun <T> start(fn: () -> T): BackgroundTask<T> {
        return object : BackgroundTask<T> {
            override fun await(): T {
                withThreadLocalsUnassigned {
                    return fn()
                }
            }
        }
    }

    override fun close() {}
}

expect fun makeBackgroundDispatcher(numThreads: Int): MPThreadPoolExecutor

interface MPWeakReference<T> {
    val value: T?

    companion object
}

expect fun <T : Any> MPWeakReference.Companion.make(ref: T): MPWeakReference<T>

interface TimerHandler {
    fun registerTimer(delays: IntArray, callbacks: List<LambdaValue>): APLValue
}

expect fun makeTimerHandler(engine: Engine): TimerHandler?

interface NativeData

expect fun makeNativeData(): NativeData

expect inline fun nativeUpdateBreakPending(engine: Engine, state: Boolean)
expect inline fun nativeBreakPending(engine: Engine): Boolean
