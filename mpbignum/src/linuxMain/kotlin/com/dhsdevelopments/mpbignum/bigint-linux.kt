package com.dhsdevelopments.mpbignum

import gmp.*
import kotlinx.cinterop.*

class MpzWrapper(val value: mpz_t) {
    companion object {
        fun allocMpz(): MpzWrapper {
            val m: mpz_t = nativeHeap.allocArray(sizeOf<__mpz_struct>())
            return MpzWrapper(m)
        }
    }
}

actual value class BigInt actual constructor(actual val impl: Any) {
    override fun toString(): String {
        val m = inner
        val size = mpz_sizeinbase!!(m, 10)
        memScoped {
            val buf = allocArray<ByteVar>(size.toLong() + 2)
            mpz_get_str!!(buf, 10, m)
            return buf.toKString()
        }
    }

    val inner get() = (impl as MpzWrapper).value

    actual companion object {
    }
}

actual fun Int.toBigint(): BigInt {
    return bigIntFromString(this.toString())
}

actual operator fun BigInt.plus(other: BigInt): BigInt {
    val a = this.inner
    val b = other.inner
    val result = MpzWrapper.allocMpz()
    mpz_add!!(result.value, a, b)
    return BigInt(result)
}

actual fun bigIntFromString(s: String): BigInt {
    val result = MpzWrapper.allocMpz()
    val m = result.value
    mpz_init!!(m)
    memScoped {
        val utf = s.encodeToByteArray()
        val buf = allocArray<ByteVar>(utf.size)
        utf.forEachIndexed { i, value ->
            buf[i] = value
        }
        mpz_set_str!!(m, buf, 10)
        return BigInt(result)
    }
}
