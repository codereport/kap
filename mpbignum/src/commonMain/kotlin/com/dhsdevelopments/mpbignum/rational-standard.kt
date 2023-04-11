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
                throw IllegalArgumentException("Zero denominator")
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

    override operator fun plus(other: Rational): Rational {
        return if (this.denominator == other.denominator) {
            RationalStandard(this.numerator + other.numerator, this.denominator, true)
        } else {
            val lcm = (this.denominator * other.denominator) / this.denominator.gcd(other.denominator)
            val ra = lcm / this.denominator
            val rb = lcm / other.denominator
            RationalStandard(numerator * ra + other.numerator * rb, lcm)
        }
    }

    override operator fun minus(other: Rational): Rational {
        return if (this.denominator == other.denominator) {
            RationalStandard(this.numerator - other.numerator, this.denominator)
        } else {
            val lcm = (this.denominator * other.denominator) / this.denominator.gcd(other.denominator)
            val ra = lcm / this.denominator
            val rb = lcm / other.denominator
            RationalStandard(numerator * ra - other.numerator * rb, lcm)
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

    /*
    let f = (a,b) => {
      let [sa,sb] = [a,b].map(c=>Math.max(0,c.toString(2).length-64));
      return Number(a>>BigInt(sa)) / Number(b>>BigInt(sb)) * Math.pow(2, sa-sb);
    }
    console.log(f(3n * 3n**1000n, 10n * 3n**1000n))
    console.log(f(3n**100n, 2n))
    */

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
