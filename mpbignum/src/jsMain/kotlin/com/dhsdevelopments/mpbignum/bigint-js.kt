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

actual operator fun BigInt.plus(other: BigInt): BigInt {
    @Suppress("UNUSED_VARIABLE")
    val a = this.inner

    @Suppress("UNUSED_VARIABLE")
    val b = other.inner
    return BigInt.makeFromJs(js("a+b"))
}

actual operator fun BigInt.minus(other: BigInt): BigInt {
    @Suppress("UNUSED_VARIABLE")
    val a = this.inner

    @Suppress("UNUSED_VARIABLE")
    val b = other.inner
    return BigInt.makeFromJs(js("a-b"))
}

actual operator fun BigInt.times(other: BigInt): BigInt {
    @Suppress("UNUSED_VARIABLE")
    val a = this.inner

    @Suppress("UNUSED_VARIABLE")
    val b = other.inner
    return BigInt.makeFromJs(js("a*b"))
}

actual operator fun BigInt.div(other: BigInt): BigInt {
    @Suppress("UNUSED_VARIABLE")
    val a = this.inner

    @Suppress("UNUSED_VARIABLE")
    val b = other.inner
    return BigInt.makeFromJs(js("a/b"))
}

actual fun BigInt.Companion.of(value: Int): BigInt {
    @Suppress("UNUSED_VARIABLE")
    val stringified = value.toString()
    return BigInt.of(stringified)
}

actual fun BigInt.Companion.of(s: String): BigInt {
    val regex = "^-?[0-9]+$".toRegex()
    if (!regex.matches(s)) {
        throw NumberFormatException("Invalid decimal value: ${s}")
    }

    @Suppress("UNUSED_VARIABLE")
    val stringified = s
    return makeFromJs(js("BigInt(stringified)"))
}
