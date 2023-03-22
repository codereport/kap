package com.dhsdevelopments.mpbignum

expect value class BigInt(val impl: Any) {
    companion object {
    }
}

expect operator fun BigInt.plus(other: BigInt): BigInt
expect operator fun BigInt.minus(other: BigInt): BigInt
expect operator fun BigInt.times(other: BigInt): BigInt
expect operator fun BigInt.div(other: BigInt): BigInt

expect fun BigInt.pow(other: Long): BigInt

expect operator fun BigInt.compareTo(other: BigInt): Int

expect fun BigInt.Companion.of(value: Int): BigInt
expect fun BigInt.Companion.of(value: Long): BigInt
expect fun BigInt.Companion.of(s: String): BigInt

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
