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

actual val BigInt.absoluteValue: BigInt
    get() = BigInt(inner.abs())

actual operator fun BigInt.plus(other: BigInt) = BigInt(inner.plus(other.inner))
actual operator fun BigInt.minus(other: BigInt) = BigInt(inner.minus(other.inner))
actual operator fun BigInt.times(other: BigInt) = BigInt(inner.times(other.inner))
actual operator fun BigInt.div(other: BigInt) = BigInt(inner.div(other.inner))

actual operator fun BigInt.unaryMinus() = BigInt(-inner)

actual fun BigInt.pow(other: Long): BigInt {
    if (other < 0 || other >= Int.MAX_VALUE) {
        throw IllegalArgumentException("Argument to pow must be a positive number that fits in 32 bits: ${other}")
    }
    return BigInt(inner.pow(other.toInt()))
}

actual operator fun BigInt.rem(other: BigInt): BigInt {
    return BigInt(inner.rem(other.inner))
}

actual operator fun BigInt.compareTo(other: BigInt) = inner.compareTo(other.inner)

actual fun BigInt.Companion.of(value: Short): BigInt {
    return BigInt.of(value.toString())
}

actual fun BigInt.Companion.of(value: Int): BigInt {
    return BigInt.of(value.toString())
}

actual fun BigInt.Companion.of(value: Long): BigInt {
    return BigInt.of(value.toString())
}

actual fun BigInt.Companion.of(s: String, radix: Int): BigInt {
    return BigInt(BigInteger(s, radix))
}

actual infix fun BigInt.and(other: BigInt): BigInt {
    return BigInt(inner and other.inner)
}

actual infix fun BigInt.or(other: BigInt): BigInt {
    return BigInt(inner or other.inner)
}

actual fun BigInt.Companion.of(value: Double): BigInt {
    return BigInt(value.toBigDecimal().toBigInteger())
}

actual infix fun BigInt.xor(other: BigInt): BigInt {
    return BigInt(inner xor other.inner)
}

actual fun BigInt.inv(): BigInt {
    return BigInt(inner.inv())
}

actual infix fun BigInt.shl(other: Long): BigInt {
    if (other < Int.MIN_VALUE || other >= Int.MAX_VALUE) {
        throw IllegalArgumentException("Argument to shl must be a positive number that fits in 32 bits: ${other}")
    }
    return BigInt(inner shl other.toInt())
}

actual infix fun BigInt.shr(other: Long): BigInt {
    if (other < Int.MIN_VALUE || other >= Int.MAX_VALUE) {
        throw IllegalArgumentException("Argument to shr must be a positive number that fits in 32 bits: ${other}")
    }
    return BigInt(inner shr other.toInt())
}

actual fun BigInt.toInt(): Int {
    return inner.toInt()
}

actual fun BigInt.toLong(): Long {
    return inner.toLong()
}

actual fun BigInt.toDouble(): Double {
    return inner.toDouble()
}

actual fun BigInt.signum(): Int {
    return inner.signum()
}

actual fun BigInt.gcd(other: BigInt): BigInt {
    return BigInt(inner.gcd(other.inner))
}

actual fun BigInt.toString(radix: Int): String {
    return inner.toString(radix)
}

actual fun BigInt.rangeInLong(): Boolean {
    return inner.bitLength() <= 63
}

actual fun BigInt.rangeInInt(): Boolean {
    return inner.bitLength() <= 31
}
