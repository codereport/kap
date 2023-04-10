package com.dhsdevelopments.mpbignum

fun standardGcd(a: BigInt, b: BigInt): BigInt {
    if (b == BigIntConstants.ZERO) {
        return a.absoluteValue
    }
    if (a == BigIntConstants.ZERO) {
        return b.absoluteValue
    }

    var a0 = a
    var b0 = b
    var z: BigInt
    while (b0 != BigIntConstants.ZERO) {
        z = b0
        b0 = a0 % b0
        a0 = z
    }
    return a0
}
