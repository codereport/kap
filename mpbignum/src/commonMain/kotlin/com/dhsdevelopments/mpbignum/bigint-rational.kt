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
    fun signum(): Int
    fun ceil(): BigInt
    fun floor(): BigInt

    fun toDouble(): Double
    fun toLongTruncated(): Long
    fun isInteger(): Boolean = denominator == BigIntConstants.ONE

    fun rangeFitsInLong(): Boolean {
        if (!isInteger()) {
            return false
        }
        val d = denominator
        return d >= Long.MIN_VALUE && d <= Long.MAX_VALUE
    }

    companion object {
        val ZERO = Rational.make(BigIntConstants.ZERO, BigIntConstants.ONE)
        val ONE = Rational.make(BigIntConstants.ONE, BigIntConstants.ONE)
    }
}

expect fun Rational.Companion.make(a: BigInt, b: BigInt): Rational
expect fun Rational.Companion.make(a: Long, b: Long): Rational
expect fun Rational.Companion.make(a: String, b: String): Rational

fun Short.toRational() = Rational.make(this.toBigInt(), BigIntConstants.ONE)
fun Int.toRational() = Rational.make(this.toBigInt(), BigIntConstants.ONE)
fun Long.toRational() = Rational.make(this.toBigInt(), BigIntConstants.ONE)

operator fun Rational.plus(other: Long) = this + other.toRational()
operator fun Long.plus(other: Rational) = this.toRational() + other
operator fun Rational.minus(other: Long) = this - other.toRational()
operator fun Long.minus(other: Rational) = this.toRational() - other
operator fun Rational.times(other: Long) = this * other.toRational()
operator fun Long.times(other: Rational) = this.toRational() * other
operator fun Rational.div(other: Long) = this / other.toRational()
operator fun Long.div(other: Rational) = this.toRational() / other

operator fun Rational.compareTo(other: Long) = this.compareTo(other.toRational())
operator fun Rational.compareTo(other: Double) = this.toDouble().compareTo(other)
operator fun Long.compareTo(other: Rational) = this.toRational().compareTo(other)
operator fun Double.compareTo(other: Rational) = this.compareTo(other.toDouble())
