package com.dhsdevelopments.mpbignum

expect value class BigInt(val impl: Any) {
    companion object {
    }
}

object BigIntConstants {
    val ZERO = BigInt.of(0)
    val ONE = BigInt.of(1)
    val ALL_BITS_64 = BigInt.of("18446744073709551615")
    val LONG_MIN_VALUE = BigInt.of(Long.MIN_VALUE)
    val LONG_MAX_VALUE = BigInt.of(Long.MAX_VALUE)
    val INT_MIN_VALUE = BigInt.of(Int.MIN_VALUE)
    val INT_MAX_VALUE = BigInt.of(Int.MAX_VALUE)
}

expect val BigInt.absoluteValue: BigInt

expect operator fun BigInt.plus(other: BigInt): BigInt
expect operator fun BigInt.minus(other: BigInt): BigInt
expect operator fun BigInt.times(other: BigInt): BigInt
expect operator fun BigInt.div(other: BigInt): BigInt

expect operator fun BigInt.unaryMinus(): BigInt

expect fun BigInt.pow(other: Long): BigInt

expect operator fun BigInt.rem(other: BigInt): BigInt
operator fun BigInt.rem(other: Long): BigInt = this % BigInt.of(other)
operator fun Long.rem(other: BigInt): BigInt = BigInt.of(this) % other

expect operator fun BigInt.compareTo(other: BigInt): Int

expect fun BigInt.Companion.of(value: Short): BigInt
expect fun BigInt.Companion.of(value: Int): BigInt
expect fun BigInt.Companion.of(value: Long): BigInt
expect fun BigInt.Companion.of(s: String, radix: Int = 10): BigInt
expect fun BigInt.Companion.of(value: Double): BigInt

operator fun BigInt.plus(other: Int) = this + BigInt.of(other)
operator fun Int.plus(other: BigInt) = BigInt.of(this) + other
operator fun BigInt.plus(other: Long) = this + BigInt.of(other)
operator fun Long.plus(other: BigInt) = BigInt.of(this) + other

operator fun BigInt.minus(other: Int) = this - BigInt.of(other)
operator fun Int.minus(other: BigInt) = BigInt.of(this) - other
operator fun BigInt.minus(other: Long) = this - BigInt.of(other)
operator fun Long.minus(other: BigInt) = BigInt.of(this) - other

operator fun BigInt.times(other: Int) = this * BigInt.of(other)
operator fun Int.times(other: BigInt) = BigInt.of(this) * other
operator fun BigInt.times(other: Long) = this * BigInt.of(other)
operator fun Long.times(other: BigInt) = BigInt.of(this) * other

operator fun BigInt.div(other: Int) = this / BigInt.of(other)
operator fun Int.div(other: BigInt) = BigInt.of(this) / other
operator fun BigInt.div(other: Long) = this / BigInt.of(other)
operator fun Long.div(other: BigInt) = BigInt.of(this) / other

fun BigInt.pow(other: Int) = this.pow(other.toLong())

operator fun BigInt.compareTo(other: Int): Int = this.compareTo(BigInt.of(other))
operator fun BigInt.compareTo(other: Long): Int = this.compareTo(BigInt.of(other))
operator fun Int.compareTo(other: BigInt): Int = BigInt.of(this).compareTo(other)
operator fun Long.compareTo(other: BigInt): Int = BigInt.of(this).compareTo(other)

expect infix fun BigInt.and(other: BigInt): BigInt
expect infix fun BigInt.or(other: BigInt): BigInt
expect infix fun BigInt.xor(other: BigInt): BigInt

infix fun BigInt.and(other: Long): BigInt = this and BigInt.of(other)
infix fun Long.and(other: BigInt): BigInt = BigInt.of(this) and other
infix fun BigInt.or(other: Long): BigInt = this or BigInt.of(other)
infix fun Long.or(other: BigInt): BigInt = BigInt.of(this) or other
infix fun BigInt.xor(other: Long): BigInt = this xor BigInt.of(other)
infix fun Long.xor(other: BigInt): BigInt = BigInt.of(this) xor other

infix fun BigInt.and(other: Int): BigInt = this and BigInt.of(other)
infix fun Int.and(other: BigInt): BigInt = BigInt.of(this) and other
infix fun BigInt.or(other: Int): BigInt = this or BigInt.of(other)
infix fun Int.or(other: BigInt): BigInt = BigInt.of(this) or other
infix fun BigInt.xor(other: Int): BigInt = this xor BigInt.of(other)
infix fun Int.xor(other: BigInt): BigInt = BigInt.of(this) xor other

expect fun BigInt.inv(): BigInt

expect infix fun BigInt.shl(other: Long): BigInt
expect infix fun BigInt.shr(other: Long): BigInt

infix fun BigInt.shl(other: Int) = this shl other.toLong()
infix fun BigInt.shr(other: Int) = this shr other.toLong()

expect fun BigInt.toInt(): Int
expect fun BigInt.toLong(): Long
expect fun BigInt.toDouble(): Double

expect fun BigInt.signum(): Int

expect fun BigInt.gcd(other: BigInt): BigInt

fun Short.toBigInt() = BigInt.of(this)
fun Int.toBigInt() = BigInt.of(this)
fun Long.toBigInt() = BigInt.of(this)

expect fun BigInt.toString(radix: Int): String

fun max(a: BigInt, b: BigInt) = if (a > b) a else a
fun min(a: BigInt, b: BigInt) = if (a < b) a else b

expect fun BigInt.rangeInLong(): Boolean
expect fun BigInt.rangeInInt(): Boolean

fun BigInt.Companion.fromDoubleFloor(value: Double): BigInt {
    val remainder = value.rem(1)
    return if (remainder >= 0.0) {
        BigInt.of(value)
    } else {
        BigInt.of(value) - 1
    }
}

fun BigInt.Companion.fromDoubleCeil(value: Double): BigInt {
    val remainder = value.rem(1)
    return if (remainder <= 0.0) {
        BigInt.of(value)
    } else {
        BigInt.of(value) + 1
    }
}

val VALID_DIGITS_LOWER = (0..35).map { i -> if (i < 10) ('0'.code + i).toChar() else ('a'.code + (i - 10)).toChar() }
val VALID_DIGITS_UPPER = (0..35).map { i -> if (i < 10) ('0'.code + i).toChar() else ('A'.code + (i - 10)).toChar() }

fun standardParseWithBase(s: String, radix: Int): BigInt {
    fun throwDefault(): Nothing = throw NumberFormatException("Invalid decimal value: ${s}")

    val isNegative = s[0] == '-'
    val start = if (isNegative) 1 else 0
    if (s.length < start + 1) throwDefault()
    var curr = BigIntConstants.ZERO
    for (i in start until s.length) {
        val ch = s[i]
        val index = VALID_DIGITS_LOWER.indexOf(ch).let { v -> if (v == -1) VALID_DIGITS_UPPER.indexOf(ch) else v }
        if (index == -1 || index >= radix) throwDefault()
        curr = curr * radix + index
    }
    return if (isNegative) -curr else curr
}
