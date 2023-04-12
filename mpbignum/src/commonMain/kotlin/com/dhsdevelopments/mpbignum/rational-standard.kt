package com.dhsdevelopments.mpbignum

import kotlin.math.max
import kotlin.math.pow

class RationalStandard(numeratorInt: BigInt, denominatorInt: BigInt, isNormalised: Boolean = false) : Rational {
    override val numerator: BigInt
    override val denominator: BigInt

    init {
        val numeratorInt0: BigInt
        val denominatorInt0: BigInt
        when (denominatorInt.signum()) {
            1 -> {
                numeratorInt0 = numeratorInt
                denominatorInt0 = denominatorInt
            }
            -1 -> {
                numeratorInt0 = -numeratorInt
                denominatorInt0 = -denominatorInt
            }
            else -> {
                throw ArithmeticException("Zero denominator")
            }
        }

        if (isNormalised) {
            numerator = numeratorInt0
            denominator = denominatorInt0
        } else {
            val v = numeratorInt0.gcd(denominatorInt0)
            if (v == BigIntConstants.ONE) {
                numerator = numeratorInt0
                denominator = denominatorInt0
            } else {
                numerator = numeratorInt0 / v
                denominator = denominatorInt0 / v
            }
        }
    }

    private inline fun <T> alignDenominator(other: Rational, fn: (n0: BigInt, n1: BigInt, denominator: BigInt) -> T): T {
        val num0 = numerator
        val den0 = denominator
        val num1 = other.numerator
        val den1 = other.denominator
        return if (den0 == den1) {
            fn(num0, num1, den0)
        } else {
            val common = den0 * den1
            fn(num0 * den1, num1 * den0, common)
        }
    }

    override operator fun plus(other: Rational): Rational {
        return alignDenominator(other) { n0, n1, den ->
            RationalStandard(n0 + n1, den)
        }
    }

    override operator fun minus(other: Rational): Rational {
        return alignDenominator(other) { n0, n1, den ->
            RationalStandard(n0 - n1, den)
        }
    }

    override fun times(other: Rational): Rational {
        return RationalStandard(this.numerator * other.numerator, this.denominator * other.denominator)
    }

    override fun div(other: Rational): Rational {
        return RationalStandard(this.numerator * other.denominator, this.denominator * other.numerator)
    }

    override fun unaryMinus(): Rational {
        return RationalStandard(-this.numerator, this.denominator)
    }

    override fun rem(other: Rational): Rational {
        return alignDenominator(other) { n0, n1, den ->
            RationalStandard(n0 % n1, den)
        }
    }

    override fun compareTo(other: Rational): Int {
        return alignDenominator(other) { n0, n1, _ ->
            n0.compareTo(n1)
        }
    }

    override fun pow(other: Long): Rational {
        return when {
            other == 0L -> RationalStandard(BigIntConstants.ONE, BigIntConstants.ONE)
            numerator == BigIntConstants.ZERO -> this
            other < 0 -> RationalStandard(denominator.pow(-other), numerator.pow(-other))
            else -> RationalStandard(numerator.pow(other), denominator.pow(other))
        }
    }

    /*
        let f = (a,b) => {
          let [sa,sb] = [a,b].map(c=>Math.max(0,c.toString(2).length-64));
          return Number(a>>BigInt(sa)) / Number(b>>BigInt(sb)) * Math.pow(2, sa-sb);
        }
        console.log(f(3n * 3n**1000n, 10n * 3n**1000n))
        console.log(f(3n**100n, 2n))
        */

    // Better version:
    // https://github.com/neelance/go/blob/4d23cbc67100c1ce50b7d4fcc67e50091f92eb5b/src/math/big/rat.go#L169

    override fun toDouble(): Double {
        val sa = max(0, numerator.toString(2).length - 64)
        val sb = max(0, denominator.toString(2).length - 64)
        return ((numerator shr sa).toDouble() / (denominator shr sb).toDouble()) * 2.0.pow(sa - sb)
    }

    override fun toString(): String {
        return "Rational[${numerator}, ${denominator}]"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as RationalStandard

        if (numerator != other.numerator) return false
        return denominator == other.denominator
    }

    override fun hashCode(): Int {
        var result = numerator.hashCode()
        result = 31 * result + denominator.hashCode()
        return result
    }
}
