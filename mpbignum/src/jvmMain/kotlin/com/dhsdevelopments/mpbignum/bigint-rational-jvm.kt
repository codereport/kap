package com.dhsdevelopments.mpbignum

actual fun Rational.Companion.make(a: BigInt, b: BigInt): Rational {
    return RationalStandard(a, b)
}
