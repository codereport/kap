package com.dhsdevelopments.mpbignum

import java.math.BigInteger

@JvmInline
actual value class BigInt(val impl: Any) {
    override fun toString(): String {
        return inner.toString()
    }

    internal val inner get() = impl as BigInteger

    actual companion object {
    }
}

actual operator fun BigInt.plus(other: BigInt) = BigInt(inner.plus(other.inner))
actual operator fun BigInt.minus(other: BigInt) = BigInt(inner.minus(other.inner))
actual operator fun BigInt.times(other: BigInt) = BigInt(inner.times(other.inner))
actual operator fun BigInt.div(other: BigInt) = BigInt(inner.div(other.inner))

actual fun BigInt.pow(other: Long): BigInt {
    if (other < 0 || other >= Int.MAX_VALUE) {
        throw IllegalArgumentException("Argument to pow must be a positive number that fits in 32 bits: ${other}")
    }
    return BigInt(inner.pow(other.toInt()))
}

actual fun BigInt.Companion.of(value: Int): BigInt {
    return BigInt.of(value.toString())
}

actual fun BigInt.Companion.of(value: Long): BigInt {
    return BigInt.of(value.toString())
}

actual fun BigInt.Companion.of(s: String): BigInt {
    return BigInt(BigInteger(s))
}