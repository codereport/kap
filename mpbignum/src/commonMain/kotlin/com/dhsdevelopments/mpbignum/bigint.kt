package com.dhsdevelopments.mpbignum

expect value class BigInt(val impl: Any) {
    companion object {
    }
}

expect operator fun BigInt.plus(other: BigInt): BigInt
expect operator fun BigInt.minus(other: BigInt): BigInt
expect operator fun BigInt.times(other: BigInt): BigInt
expect operator fun BigInt.div(other: BigInt): BigInt

expect fun BigInt.Companion.of(value: Int): BigInt
expect fun BigInt.Companion.of(s: String): BigInt

operator fun BigInt.plus(other: Int) = this + BigInt.of(other)
operator fun Int.plus(other: BigInt) = BigInt.of(this) + other
