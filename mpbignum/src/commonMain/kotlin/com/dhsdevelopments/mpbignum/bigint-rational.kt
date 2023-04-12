package com.dhsdevelopments.mpbignum

interface Rational {
    val numerator: BigInt
    val denominator: BigInt

    operator fun plus(other: Rational): Rational
    operator fun minus(other: Rational): Rational
    operator fun times(other: Rational): Rational
    operator fun div(other: Rational): Rational
    operator fun unaryMinus(): Rational
    operator fun rem(other: Rational): Rational
    operator fun compareTo(other: Rational): Int

    fun pow(other: Long): Rational

    fun toDouble(): Double

    companion object {
    }
}

expect fun Rational.Companion.make(a: BigInt, b: BigInt): Rational
