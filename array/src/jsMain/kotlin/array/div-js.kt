package array

import org.khronos.webgl.Uint8Array
import kotlin.js.Date
import kotlin.reflect.KClass

actual fun sleepMillis(time: Long) {
    TODO("not implemented")
}

class JsAtomicRefArray<T>(size: Int) : MPAtomicRefArray<T> {
    private val content: MutableList<T?>

    init {
        content = ArrayList()
        repeat(size) {
            content.add(null)
        }
    }

    override fun get(index: Int) = content[index]

    override fun compareAndExchange(index: Int, expected: T?, newValue: T?): T? {
        val v = content[index]
        if (v == expected) {
            content[index] = newValue
        }
        return v
    }
}

actual fun <T> makeAtomicRefArray(size: Int): MPAtomicRefArray<T> {
    return JsAtomicRefArray(size)
}

actual fun <T : Any> makeMPThreadLocalBackend(type: KClass<T>): MPThreadLocal<T> {
    return object : MPThreadLocal<T> {
        override var value: T? = null
    }
}

private val NON_SCIENTIFIC_INTEGER_REGEX = "^-?[0-9]+$".toRegex()

actual fun Double.formatDouble(): String {
    val s = this.toString()
    return if (NON_SCIENTIFIC_INTEGER_REGEX.matches(s)) {
        "${s}.0"
    } else {
        s
    }
}

actual fun currentTime(): Long {
    return Date.now().toLong()
}


actual fun toRegexpWithException(string: String, options: Set<RegexOption>): Regex {
    return try {
        string.toRegex(options)
    } catch (e: Throwable) {
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

class JsWeakReference<T : Any>(ref: T) : MPWeakReference<T> {
    private val instance: dynamic

    init {
        @Suppress("UNUSED_VARIABLE")
        val inst = ref
        instance = js("new WeakRef(inst)")
    }

    override val value: T?
        get() {
            @Suppress("UNUSED_VARIABLE")
            val inst = instance
            val v = js("var a = inst.deref(); if(a) { return a; } else { return null; }")
            return v as T?
        }
}

actual fun <T : Any> MPWeakReference.Companion.make(ref: T): MPWeakReference<T> {
    return JsWeakReference(ref)
}

actual fun makeTimerHandler(engine: Engine): TimerHandler? = null

external class SharedArrayBuffer(size: Int)

private fun crossOriginIsolatedAndDefined(): Boolean {
    return js("typeof(crossOriginIsolated) !== \"undefined\" && crossOriginIsolated")
}

class JsNativeData : NativeData {
    val breakBufferData: SharedArrayBuffer?
    val breakBufferArray: Uint8Array?

    init {
        if (crossOriginIsolatedAndDefined()) {
            breakBufferData = SharedArrayBuffer(1)
            @Suppress("UNUSED_VARIABLE")
            val bd = breakBufferData
            breakBufferArray = js("new Uint8Array(bd)") as Uint8Array
            @Suppress("UNUSED_VARIABLE")
            val barray = breakBufferArray
            js("Atomics.store(barray, 0, 0)")
        } else {
            breakBufferData = null
            breakBufferArray = null
        }
    }
}

actual fun makeNativeData(): NativeData = JsNativeData()

@Suppress("NOTHING_TO_INLINE")
actual inline fun nativeUpdateBreakPending(engine: Engine, state: Boolean) {
    val jsNativeData = engine.nativeData as JsNativeData
    val b = jsNativeData.breakBufferArray
    if (b != null) {
        @Suppress("UNUSED_VARIABLE")
        val newState = if (state) 1 else 0
        js("Atomics.store(b, 0, newState)")
    }
}

@Suppress("NOTHING_TO_INLINE")
actual inline fun nativeBreakPending(engine: Engine): Boolean {
    val jsNativeData = engine.nativeData as JsNativeData
    val b = jsNativeData.breakBufferArray
    return if (b != null) {
        js("Atomics.load(b, 0) == 1") as Boolean
    } else {
        false
    }
}

actual fun findInstallPath(): String? = null
