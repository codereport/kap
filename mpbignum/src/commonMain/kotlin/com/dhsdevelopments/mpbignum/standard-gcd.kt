package com.dhsdevelopments.mpbignum

fun standardGcd(a: BigInt, b: BigInt): BigInt {
    if (b.signum() == 0) {
        return a.absoluteValue
    }
    if (a.signum() == 0) {
        return b.absoluteValue
    }

    var a0 = a
    var b0 = b
    var z: BigInt
    while (b0.signum() != 0) {
        z = b0
        b0 = a0 % b0
        a0 = z
    }
    return a0.absoluteValue
}
