package com.dhsdevelopments.mpbignum

import org.apache.commons.math3.exception.ZeroException
import org.apache.commons.math3.fraction.BigFraction
import java.math.BigInteger

class JvmRational private constructor(val value: BigFraction) : Rational {
    constructor(a: BigInt, b: BigInt) : this(BigFraction(a.inner, b.inner))

    override val numerator get() = BigInt(value.numerator)
    override val denominator get() = BigInt(value.denominator)

    override fun plus(other: Rational): Rational {
        return JvmRational(value.add((other as JvmRational).value))
    }

    override fun minus(other: Rational): Rational {
        return JvmRational(value.subtract((other as JvmRational).value))
    }

    override fun times(other: Rational): Rational {
        return JvmRational(value.multiply((other as JvmRational).value))
    }

    override fun div(other: Rational): Rational {
        return JvmRational(value.divide((other as JvmRational).value))
    }

    override fun unaryMinus(): Rational {
        return JvmRational(value.negate())
    }

    override fun rem(other: Rational): Rational {
        val other0 = other as JvmRational
        val num0 = value.numerator
        val den0 = value.denominator
        val num1 = other0.value.numerator
        val den1 = other0.value.denominator
        val fraction = if (den0 == den1) {
            BigFraction(num0.rem(num1), den0)
        } else {
            val lcm = (den0 * den1) / den0.gcd(den1)
            val ra = lcm / den0
            val rb = lcm / den1
            BigFraction(((num0 * ra) % (num1 * rb)), lcm)
        }
        return JvmRational(fraction)
    }

    override fun compareTo(other: Rational): Int {
        return value.compareTo((other as JvmRational).value)
    }

    override fun pow(other: Long): Rational {
        return JvmRational(value.pow(other))
    }

    override fun signum(): Int {
        return when {
            this > 0 -> 1
            this < 0 -> -1
            else -> 0
        }
    }

    override fun ceil(): BigInt {
        return if (value.denominator == BigInteger.ONE) {
            BigInt(value.numerator)
        } else if (value.numerator < BigInteger.ZERO) {
            return BigInt(value.numerator / value.denominator)
        } else {
            return BigInt(value.numerator / value.denominator + BigInteger.ONE)
        }
    }

    override fun floor(): BigInt {
        return if (value.denominator == BigInteger.ONE) {
            BigInt(value.numerator)
        } else if (value.numerator < BigInteger.ZERO) {
            BigInt(value.numerator / value.denominator - BigInteger.ONE)
        } else {
            BigInt(value.numerator / value.denominator)
        }
    }

    override fun toLongTruncated(): Long {
        return value.toLong()
    }

    override fun toDouble(): Double {
        return value.toDouble()
    }

    override fun toString() = value.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || this::class != other::class) return false
        other as JvmRational
        return value.equals(other.value)
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}

actual fun Rational.Companion.make(a: BigInt, b: BigInt): Rational {
    try {
        return JvmRational(a, b)
    } catch (e: ZeroException) {
        throw ArithmeticException("Zero denominator")
    }
}

actual fun Rational.Companion.make(a: Long, b: Long): Rational {
    return make(a.toBigInt(), b.toBigInt())
}

actual fun Rational.Companion.make(a: String, b: String): Rational {
    return make(BigInt.of(a), BigInt.of(b))
}
