package com.dhsdevelopments.mpbignum

actual fun Rational.Companion.make(a: BigInt, b: BigInt): Rational {
    return RationalStandard(a, b)
}

actual fun Rational.Companion.make(a: Long, b: Long): Rational {
    return RationalStandard(a.toBigInt(), b.toBigInt())
}

actual fun Rational.Companion.make(a: String, b: String): Rational {
    return RationalStandard(BigInt.of(a), BigInt.of(b))
}
