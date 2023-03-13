package array

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

actual fun Double.formatDouble(): String {
    return if (this.rem(1.0) == 0.0) {
        "${this}.0"
    } else {
        this.toString()
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
