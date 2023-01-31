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

    fun finalize() {
        println("finalising: ${BigInt(this).toString()}")
        mpz_clear!!(value)
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

private inline fun BigInt.basicOperation(other: BigInt, fn: (result: MpzWrapper, a: mpz_t, b: mpz_t) -> Unit): BigInt {
    val a = this.inner
    val b = other.inner
    val result = MpzWrapper.allocMpz()
    fn(result, a, b)
    return BigInt(result)
}

actual operator fun BigInt.plus(other: BigInt) = basicOperation(other) { result, a, b -> mpz_add!!(result.value, a, b) }
actual operator fun BigInt.minus(other: BigInt) = basicOperation(other) { result, a, b -> mpz_sub!!(result.value, a, b) }
actual operator fun BigInt.times(other: BigInt) = basicOperation(other) { result, a, b -> mpz_mul!!(result.value, a, b) }
actual operator fun BigInt.div(other: BigInt) = basicOperation(other) { result, a, b -> mpz_div!!(result.value, a, b) }

actual fun BigInt.Companion.of(value: Int): BigInt {
    return BigInt.of(value.toString())
}

actual fun BigInt.Companion.of(s: String): BigInt {
    val result = MpzWrapper.allocMpz()
    val m = result.value
    mpz_init!!(m)
    memScoped {
        val utf = s.encodeToByteArray()
        val buf = allocArray<ByteVar>(utf.size)
        utf.forEachIndexed { i, value ->
            buf[i] = value
        }
        val res = mpz_set_str!!(m, buf, 10)
        if (res != 0) {
            throw NumberFormatException("Invalid number format: ${s}")
        }
        return BigInt(result)
    }
}
