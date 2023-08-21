@file:OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)

package array

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.concurrent.AtomicReference
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.WeakReference
import kotlin.reflect.KClass

actual fun sleepMillis(time: Long) {
    memScoped {
        val tms = alloc<timespec>()
        tms.tv_sec = time / 1000
        tms.tv_nsec = time.rem(1000) * 1000L * 1000L
        nanosleep(tms.ptr, null)
    }
}

class LinuxMPAtomicRefArray<T>(size: Int) : MPAtomicRefArray<T> {
    private val content = ArrayList<AtomicReference<T?>>(size)

    init {
        repeat(size) { content.add(AtomicReference(null)) }
    }

    override operator fun get(index: Int): T? = content[index].value

    override fun compareAndExchange(index: Int, expected: T?, newValue: T?): T? {
        val reference = content[index]
        return reference.compareAndExchange(expected, newValue)
    }
}

actual fun <T> makeAtomicRefArray(size: Int): MPAtomicRefArray<T> {
    return LinuxMPAtomicRefArray(size)
}

actual fun <T : Any> makeMPThreadLocalBackend(type: KClass<T>): MPThreadLocal<T> {
    // Use the single-threaded implementation as the native version doesn't support multi-threading yet
    return object : MPThreadLocal<T> {
        override var value: T? = null
    }
}

actual fun Double.formatDouble() = this.toString()

actual fun currentTime(): Long {
    memScoped {
        val value = alloc<timeval>()
        if (gettimeofday(value.ptr, null) != 0) {
            throw RuntimeException("Error getting time: ${strerror(errno)?.toKString() ?: "null error"}")
        }
        return (value.tv_sec * 1000) + (value.tv_usec / 1000)
    }
}

actual fun toRegexpWithException(string: String, options: Set<RegexOption>): Regex {
    return try {
        string.toRegex(options)
    } catch (e: Exception) {
        throw RegexpParseException("Error parsing regexp: \"${string}\"", e)
    }
}

actual class MPLock

actual inline fun <T> MPLock.withLocked(fn: () -> T): T {
    return fn()
}

actual fun numCores() = 1

actual fun makeBackgroundDispatcher(numThreads: Int): MPThreadPoolExecutor {
    return SingleThreadedThreadPoolExecutor()
}

class LinuxWeakRef<T : Any>(ref: T) : MPWeakReference<T> {
    private val instance = WeakReference(ref)

    override val value: T? get() = instance.value
}

actual fun <T : Any> MPWeakReference.Companion.make(ref: T): MPWeakReference<T> {
    return LinuxWeakRef(ref)
}

actual fun makeTimerHandler(engine: Engine): TimerHandler? = null

class LinuxNativeData : NativeData

actual fun makeNativeData(): NativeData = LinuxNativeData()

actual inline fun nativeUpdateBreakPending(engine: Engine, state: Boolean) {}
actual inline fun nativeBreakPending(engine: Engine): Boolean = false
