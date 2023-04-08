package com.dhsdevelopments.mpbignum

import gmp.*
import kotlinx.cinterop.*
import kotlin.native.internal.createCleaner

class MpzWrapper(val value: mpz_t) {
    companion object {
        fun allocMpzWrapper(): MpzWrapper {
            return MpzWrapper(nativeHeap.allocMpzStruct())
        }

    }

    override fun equals(other: Any?): Boolean {
        if (other !is MpzWrapper) {
            return false
        }
        return mpz_cmp!!(this.value, other.value) == 0
    }

    override fun hashCode(): Int {
        return mpz_get_si!!(this.value).toInt()
    }

    @Suppress("unused")
    @OptIn(ExperimentalStdlibApi::class)
    private val cleaner = createCleaner(value) { obj ->
        mpz_clear!!(obj)
        nativeHeap.free(obj)
    }
}

inline fun NativePlacement.allocMpzStruct(): mpz_t = allocArray<__mpz_struct>(sizeOf<__mpz_struct>())

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
    val result = MpzWrapper.allocMpzWrapper()
    fn(result, a, b)
    return BigInt(result)
}

private inline fun BigInt.basicOperation1Arg(fn: (result: MpzWrapper, a: mpz_t) -> Unit): BigInt {
    val a = this.inner
    val result = MpzWrapper.allocMpzWrapper()
    fn(result, a)
    return BigInt(result)
}

actual val BigInt.absoluteValue: BigInt
    get() = basicOperation1Arg { result, a -> mpz_abs!!(result.value, a) }

actual operator fun BigInt.plus(other: BigInt) = basicOperation(other) { result, a, b -> mpz_add!!(result.value, a, b) }
actual operator fun BigInt.minus(other: BigInt) = basicOperation(other) { result, a, b -> mpz_sub!!(result.value, a, b) }
actual operator fun BigInt.times(other: BigInt) = basicOperation(other) { result, a, b -> mpz_mul!!(result.value, a, b) }
actual operator fun BigInt.div(other: BigInt) = basicOperation(other) { result, a, b -> mpz_div!!(result.value, a, b) }

actual operator fun BigInt.unaryMinus() = basicOperation1Arg { result, a -> mpz_neg!!(result.value, a) }

actual fun BigInt.pow(other: Long): BigInt {
    if (other < 0) {
        throw IllegalArgumentException("Negative power: ${other}")
    }
    val a = this.inner
    val result = MpzWrapper.allocMpzWrapper()
    mpz_pow_ui!!(result.value, a, other.toULong())
    return BigInt(result)
}

actual operator fun BigInt.rem(other: BigInt): BigInt {
    return basicOperation(other) { result, a, b ->
        if (mpz_sgn_wrap(a) == -1) {
            memScoped {
                val res = allocMpzStruct()
                if (mpz_sgn_wrap(b) == -1) {
                    val bAbs = allocMpzStruct()
                    mpz_neg!!(bAbs, b)
                    mpz_mod!!(res, a, b)
                    mpz_sub!!(result.value, res, bAbs)
                    mpz_clear!!(bAbs)
                } else {
                    mpz_mod!!(res, a, b)
                    mpz_sub!!(result.value, res, b)
                }
                mpz_clear!!(res)
            }
        } else {
            if (mpz_sgn_wrap(b) == -1) {
                memScoped {
                    val bAbs = allocMpzStruct()
                    mpz_neg!!(bAbs, b)
                    mpz_mod!!(result.value, a, bAbs)
                    mpz_clear!!(bAbs)
                }
            } else {
                mpz_mod!!(result.value, a, b)
            }
        }
    }
}

actual operator fun BigInt.compareTo(other: BigInt): Int {
    val a = this.inner
    val b = other.inner
    return mpz_cmp!!(a, b)
}

actual fun BigInt.Companion.of(value: Int): BigInt {
    return BigInt.of(value.toString())
}

actual fun BigInt.Companion.of(value: Long): BigInt {
    return BigInt.of(value.toString())
}

actual fun BigInt.Companion.of(s: String): BigInt {
    val result = MpzWrapper.allocMpzWrapper()
    val m = result.value
    mpz_init!!(m)
    memScoped {
        val utf = s.encodeToByteArray()
        val buf = allocArray<ByteVar>(utf.size + 1)
        utf.forEachIndexed { i, value ->
            buf[i] = value
        }
        buf[utf.size] = 0
        val res = mpz_set_str!!(m, buf, 10)
        if (res != 0) {
            throw NumberFormatException("Invalid number format: ${s}, result: ${res}")
        }
        return BigInt(result)
    }
}

actual infix fun BigInt.and(other: BigInt) = basicOperation(other) { result, a, b -> mpz_and!!(result.value, a, b) }
actual infix fun BigInt.or(other: BigInt) = basicOperation(other) { result, a, b -> mpz_ior!!(result.value, a, b) }
actual infix fun BigInt.xor(other: BigInt) = basicOperation(other) { result, a, b -> mpz_xor!!(result.value, a, b) }

actual infix fun BigInt.shl(other: Long): BigInt {
    return when {
        other > 0 -> basicOperation1Arg { result, a ->
            mpz_mul_2exp!!(result.value, a, other.toULong())
        }
        other < 0 -> basicOperation1Arg { result, a ->
            mpz_fdiv_q_2exp!!(result.value, a, (-other).toULong())
        }
        else -> this
    }
}

actual infix fun BigInt.shr(other: Long): BigInt {
    return when {
        other > 0 -> basicOperation1Arg { result, a ->
            mpz_fdiv_q_2exp!!(result.value, a, other.toULong())
        }
        other < 0 -> basicOperation1Arg { result, a ->
            mpz_mul_2exp!!(result.value, a, (-other).toULong())
        }
        else -> this
    }
}

actual fun BigInt.toLong(): Long {
    return if (this.signum() == -1) {
        -mpz_get_ui!!(this.inner).toLong()
    } else {
        mpz_get_ui!!(this.inner).toLong()
    }
}

actual fun BigInt.toDouble(): Double {
    return mpz_get_d!!(this.inner)
}

actual fun BigInt.signum(): Int {
    return mpz_sgn_wrap(this.inner)
}
