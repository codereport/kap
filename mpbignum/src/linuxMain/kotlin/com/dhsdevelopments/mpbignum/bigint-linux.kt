package com.dhsdevelopments.mpbignum

import gmp.*
import kotlinx.cinterop.*
import kotlin.native.internal.createCleaner

class MpzWrapper(val value: mpz_t) {
    companion object {
        fun allocMpzWrapper(): MpzWrapper {
            val result = nativeHeap.allocMpzStruct()
            return MpzWrapper(result)
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

internal inline fun NativePlacement.allocMpzStruct(): mpz_t = allocArray<__mpz_struct>(sizeOf<__mpz_struct>()).also { v -> mpz_init!!(v) }

actual value class BigInt actual constructor(actual val impl: Any) {
    override fun toString(): String {
        return this.toString(10)
    }

    val inner get() = (impl as MpzWrapper).value

    actual companion object {
    }
}

private inline fun BigInt.basicOperation(other: BigInt, fn: (result: mpz_t, a: mpz_t, b: mpz_t) -> Unit): BigInt {
    val a = this.inner
    val b = other.inner
    val result = MpzWrapper.allocMpzWrapper()
    fn(result.value, a, b)
    return BigInt(result)
}

private inline fun BigInt.basicOperation1Arg(fn: (result: mpz_t, a: mpz_t) -> Unit): BigInt {
    val a = this.inner
    val result = MpzWrapper.allocMpzWrapper()
    fn(result.value, a)
    return BigInt(result)
}

actual val BigInt.absoluteValue: BigInt
    get() = basicOperation1Arg { result, a -> mpz_abs!!(result, a) }

actual operator fun BigInt.plus(other: BigInt) = basicOperation(other) { result, a, b -> mpz_add!!(result, a, b) }
actual operator fun BigInt.minus(other: BigInt) = basicOperation(other) { result, a, b -> mpz_sub!!(result, a, b) }
actual operator fun BigInt.times(other: BigInt) = basicOperation(other) { result, a, b -> mpz_mul!!(result, a, b) }
actual operator fun BigInt.div(other: BigInt) = basicOperation(other) { result, a, b -> mpz_tdiv_q!!(result, a, b) }

actual operator fun BigInt.unaryMinus() = basicOperation1Arg { result, a -> mpz_neg!!(result, a) }

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
        linuxBigIntRem(result, a, b)
    }
}

internal fun linuxBigIntRem(result: mpz_t, a: mpz_t, b: mpz_t) {
    if (mpz_sgn_wrap(a) == -1) {
        memScoped {
            val res = allocMpzStruct()
            if (mpz_sgn_wrap(b) == -1) {
                val bAbs = allocMpzStruct()
                mpz_neg!!(bAbs, b)
                mpz_mod!!(res, a, b)
                mpz_sub!!(result, res, bAbs)
                mpz_clear!!(bAbs)
            } else {
                mpz_mod!!(res, a, b)
                mpz_sub!!(result, res, b)
            }
            mpz_clear!!(res)
        }
    } else {
        if (mpz_sgn_wrap(b) == -1) {
            memScoped {
                val bAbs = allocMpzStruct()
                mpz_neg!!(bAbs, b)
                mpz_mod!!(result, a, bAbs)
                mpz_clear!!(bAbs)
            }
        } else {
            mpz_mod!!(result, a, b)
        }
    }
}

actual operator fun BigInt.compareTo(other: BigInt): Int {
    val a = this.inner
    val b = other.inner
    return mpz_cmp!!(a, b)
}

actual fun BigInt.Companion.of(value: Short): BigInt {
    return BigInt.of(value.toString())
}

actual fun BigInt.Companion.of(value: Int): BigInt {
    return BigInt.of(value.toString())
}

actual fun BigInt.Companion.of(value: Long): BigInt {
    return BigInt.of(value.toString())
}

inline fun NativePlacement.allocString(s: String): CArrayPointer<ByteVar> {
    val utf = s.encodeToByteArray()
    val buf = allocArray<ByteVar>(utf.size + 1)
    utf.forEachIndexed { i, value ->
        buf[i] = value
    }
    buf[utf.size] = 0
    return buf
}

actual fun BigInt.Companion.of(s: String): BigInt {
    val result = MpzWrapper.allocMpzWrapper()
    memScoped {
        val buf = allocString(s)
        val res = mpz_set_str!!(result.value, buf, 10)
        if (res != 0) {
            throw NumberFormatException("Invalid number format: ${s}, result: ${res}")
        }
        return BigInt(result)
    }
}

actual infix fun BigInt.and(other: BigInt) = basicOperation(other) { result, a, b -> mpz_and!!(result, a, b) }
actual infix fun BigInt.or(other: BigInt) = basicOperation(other) { result, a, b -> mpz_ior!!(result, a, b) }
actual infix fun BigInt.xor(other: BigInt) = basicOperation(other) { result, a, b -> mpz_xor!!(result, a, b) }

actual fun BigInt.inv(): BigInt = basicOperation1Arg { result, a -> mpz_com!!(result, a) }

actual infix fun BigInt.shl(other: Long): BigInt {
    return when {
        other > 0 -> basicOperation1Arg { result, a ->
            mpz_mul_2exp!!(result, a, other.toULong())
        }
        other < 0 -> basicOperation1Arg { result, a ->
            mpz_fdiv_q_2exp!!(result, a, (-other).toULong())
        }
        else -> this
    }
}

actual infix fun BigInt.shr(other: Long): BigInt {
    return when {
        other > 0 -> basicOperation1Arg { result, a ->
            mpz_fdiv_q_2exp!!(result, a, other.toULong())
        }
        other < 0 -> basicOperation1Arg { result, a ->
            mpz_mul_2exp!!(result, a, (-other).toULong())
        }
        else -> this
    }
}

internal fun mpzToLong(value: mpz_t): Long {
    return if (mpz_sgn_wrap(value) == -1) {
        -mpz_get_ui!!(value).toLong()
    } else {
        mpz_get_ui!!(value).toLong()
    }
}

actual fun BigInt.toLong(): Long {
    return mpzToLong(this.inner)
}

actual fun BigInt.toDouble(): Double {
    return mpz_get_d!!(this.inner)
}

actual fun BigInt.signum(): Int {
    return mpz_sgn_wrap(this.inner)
}

actual fun BigInt.gcd(other: BigInt) = basicOperation(other) { result, a, b -> mpz_gcd!!(result, a, b) }

actual fun BigInt.toString(radix: Int): String {
    val m = inner
    val size = mpz_sizeinbase!!(m, radix)
    memScoped {
        val buf = allocArray<ByteVar>(size.toLong() + 2)
        mpz_get_str!!(buf, radix, m)
        return buf.toKString()
    }
}

actual fun BigInt.rangeInLong(): Boolean {
    return mpz_fits_slong_p!!(inner) != 0
}
