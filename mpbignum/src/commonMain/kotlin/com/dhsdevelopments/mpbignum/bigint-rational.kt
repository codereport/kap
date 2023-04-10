package com.dhsdevelopments.mpbignum

interface Rational {
    val numerator: BigInt
    val denominator: BigInt

    operator fun plus(other: Rational): Rational

    companion object {
    }
}

expect fun Rational.Companion.make(a: BigInt, b: BigInt): Rational
