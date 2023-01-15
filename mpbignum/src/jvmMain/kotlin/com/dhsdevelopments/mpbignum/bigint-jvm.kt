package com.dhsdevelopments.mpbignum

import java.math.BigInteger

@JvmInline
actual value class BigInt(val impl: Any) {
    override fun toString(): String {
        return inner.toString()
    }

    internal val inner get() = impl as BigInteger

    actual companion object {
    }
}

actual fun Int.toBigint(): BigInt {
    return bigIntFromString(this.toString())
}

actual operator fun BigInt.plus(other: BigInt): BigInt {
    return BigInt(inner.plus(other.inner))
}

actual fun bigIntFromString(s: String): BigInt {
    return BigInt(BigInteger(s))
}
