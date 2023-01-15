package com.dhsdevelopments.mpbignum

class BigIntWrapper(val value: dynamic)

actual value class BigInt(val impl: Any) {
    override fun toString(): String = inner.toString()

    internal val inner: dynamic
        get() {
            return (impl as BigIntWrapper).value
        }

    actual companion object {
        fun makeFromJs(v: dynamic): BigInt {
            return BigInt(BigIntWrapper(v))
        }
    }
}

actual fun Int.toBigint(): BigInt {
    @Suppress("UNUSED_VARIABLE")
    val stringified = this.toString()
    return bigIntFromString(stringified)
}

actual operator fun BigInt.plus(other: BigInt): BigInt {
    @Suppress("UNUSED_VARIABLE")
    val a = this.inner

    @Suppress("UNUSED_VARIABLE")
    val b = other.inner
    return BigInt.makeFromJs(js("a+b"))
}

actual fun bigIntFromString(s: String): BigInt {
    @Suppress("UNUSED_VARIABLE")
    val stringified = s
    return BigInt.makeFromJs(js("BigInt(stringified)"))
}
