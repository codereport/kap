package array

import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReferenceArray
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.regex.PatternSyntaxException
import kotlin.reflect.KClass

actual fun sleepMillis(time: Long) {
    Thread.sleep(time)
}

class JvmMPAtomicRefArray<T>(size: Int) : MPAtomicRefArray<T> {
    private val content = AtomicReferenceArray<T>(size)

    override operator fun get(index: Int): T? = content[index]

    override fun compareAndExchange(index: Int, expected: T?, newValue: T?): T? {
        return content.compareAndExchange(index, expected, newValue)
    }
}

actual fun <T> makeAtomicRefArray(size: Int): MPAtomicRefArray<T> {
    return JvmMPAtomicRefArray(size)
}

actual fun <T : Any> makeMPThreadLocalBackend(type: KClass<T>): MPThreadLocal<T> {
    return object : MPThreadLocal<T> {
        val tl = object : ThreadLocal<T?>() {
            override fun initialValue(): T? = null
        }

        override var value: T?
            get() = tl.get()
            set(newValue) = tl.set(newValue)
    }
}

actual fun Double.formatDouble() = this.toString()

actual fun currentTime(): Long {
    return System.currentTimeMillis()
}

actual fun toRegexpWithException(string: String, options: Set<RegexOption>): Regex {
    return try {
        string.toRegex(options)
    } catch (e: PatternSyntaxException) {
        throw RegexpParseException("Error parsing regexp: \"${string}\"", e)
    }
}

actual class MPLock actual constructor() {
    val impl: ReadWriteLock = ReentrantReadWriteLock()
}

actual inline fun <T> MPLock.withLocked(fn: () -> T): T {
    val lock = impl.writeLock()
    lock.lock()
    try {
        return fn()
    } finally {
        lock.unlock()
    }
}

actual fun numCores(): Int {
    return Runtime.getRuntime().availableProcessors()
}

class JvmMPThreadPoolExecutor(val maxNumParallel: Int) : MPThreadPoolExecutor {
    private val executor = Executors.newFixedThreadPool(maxNumParallel)
    override val numThreads get() = maxNumParallel

    override fun <T> start(fn: () -> T): BackgroundTask<T> {
        val future = executor.submit(fn)
        return JvmTExecutorTask(future)
    }

    override fun close() {
        executor.shutdown()
    }

    inner class JvmTExecutorTask<T>(val future: Future<T>) : BackgroundTask<T> {
        override fun await(): T {
            return future.get()
        }
    }
}

actual fun makeBackgroundDispatcher(numThreads: Int): MPThreadPoolExecutor {
    return JvmMPThreadPoolExecutor(numThreads)
}

class JvmWeakReference<T : Any>(ref: T) : MPWeakReference<T> {
    private val instance = WeakReference(ref)

    override val value: T? get() = instance.get()
}

actual fun <T : Any> MPWeakReference.Companion.make(ref: T): MPWeakReference<T> {
    return JvmWeakReference(ref)
}

class JvmNativeData : NativeData

actual fun makeNativeData(): NativeData = JvmNativeData()

@Suppress("NOTHING_TO_INLINE")
actual inline fun nativeUpdateBreakPending(engine: Engine, state: Boolean) {
}

@Suppress("NOTHING_TO_INLINE")
actual inline fun nativeBreakPending(engine: Engine): Boolean = false
