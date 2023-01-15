package com.dhsdevelopments.mpbignum

expect value class BigInt(val impl: Any) {
    companion object {
    }
}

expect fun bigIntFromString(s: String): BigInt
expect fun Int.toBigint(): BigInt
expect operator fun BigInt.plus(other: BigInt): BigInt
