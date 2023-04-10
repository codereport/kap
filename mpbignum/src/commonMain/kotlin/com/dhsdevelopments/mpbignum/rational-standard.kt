package com.dhsdevelopments.mpbignum

class RationalStandard(numeratorInt: BigInt, denominatorInt: BigInt, isNormalised: Boolean = false) : Rational {
    override val numerator: BigInt
    override val denominator: BigInt

    init {
        if (isNormalised) {
            numerator = numeratorInt
            denominator = denominatorInt
        } else {
            val v = numeratorInt.gcd(denominatorInt)
            if (v == BigIntConstants.ONE) {
                numerator = numeratorInt
                denominator = denominatorInt
            } else {
                numerator = numeratorInt / v
                denominator = denominatorInt / v
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
            val newNumerator = this.numerator * ra + other.numerator * rb
            val newDenominator = lcm
            RationalStandard(newNumerator, newDenominator)
        }
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
