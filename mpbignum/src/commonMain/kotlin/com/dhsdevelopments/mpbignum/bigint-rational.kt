package com.dhsdevelopments.mpbignum

interface Rational {
    val numerator: BigInt
    val denominator: BigInt

    operator fun plus(other: Rational): Rational
    operator fun minus(other: Rational): Rational
    operator fun times(other: Rational): Rational
    operator fun div(other: Rational): Rational
    operator fun unaryMinus(): Rational

    fun toDouble(): Double

    companion object {
    }
}

expect fun Rational.Companion.make(a: BigInt, b: BigInt): Rational
