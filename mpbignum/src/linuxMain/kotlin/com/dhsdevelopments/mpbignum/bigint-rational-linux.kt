package com.dhsdevelopments.mpbignum

import gmp.*
import kotlinx.cinterop.*
import kotlin.native.internal.createCleaner

internal inline fun NativePlacement.allocMpqStruct(): mpq_t = allocArray<__mpq_struct>(1).also { v -> mpq_init!!(v) }

class LinuxRational(val value: mpq_t) : Rational {

    companion object {
        fun make(a: BigInt, b: BigInt): LinuxRational {
            if (b == BigIntConstants.ZERO) {
                throw ArithmeticException("Denominator is zero")
            }
            val m = nativeHeap.allocMpqStruct()
            mpq_set_num!!(m, a.inner)
            mpq_set_den!!(m, b.inner)
            mpq_canonicalize!!(m)
            return LinuxRational(m)
        }

        fun make(a: Long, b: Long): LinuxRational {
            if (b == 0L) {
                throw ArithmeticException("Denominator is zero")
            }
            val m = nativeHeap.allocMpqStruct()
            if (b > 0) {
                mpq_set_si_wrap(m, a, b.toULong())
            } else {
                mpq_set_si_wrap(m, -a, (-b).toULong())
            }
            mpq_canonicalize!!(m)
            return LinuxRational(m)
        }

        fun make(s: String): LinuxRational {
            val m0 = memScoped {
                val m = nativeHeap.allocMpqStruct()
                val res = mpq_set_str!!(m, s.cstr.ptr, 10)
                if (res != 0) {
                    mpq_clear!!(m)
                    nativeHeap.free(m)
                    throw NumberFormatException("Invalid number format: ${s}, result: ${res}")
                }
                mpq_canonicalize!!(m)
                m
            }
            return LinuxRational(m0)
        }

        fun make(a: String, b: String): Rational {
            return make("${a}/${b}")
        }
    }

    override val absoluteValue: Rational
        get() {
            return if (mpq_sgn_wrap(value) == -1) {
                val result = nativeHeap.allocMpqStruct()
                mpq_neg!!(result, this.value)
                mpq_canonicalize!!(result)
                LinuxRational(result)
            } else {
                this
            }
        }

    @Suppress("unused")
    @OptIn(ExperimentalStdlibApi::class)
    private val cleaner = createCleaner(value) { obj ->
        mpq_clear!!(obj)
        nativeHeap.free(obj)
    }

    private inline fun LinuxRational.basicOperation(other: Rational, fn: (result: mpq_t, a: mpq_t, b: mpq_t) -> Unit): LinuxRational {
        val a = this.value
        val b = (other as LinuxRational).value
        val result = nativeHeap.allocMpqStruct()
        fn(result, a, b)
        return LinuxRational(result)
    }

    private inline fun LinuxRational.basicOperation1Arg(fn: (result: mpq_t, a: mpq_t) -> Unit): LinuxRational {
        val a = this.value
        val result = nativeHeap.allocMpqStruct()
        fn(result, a)
        return LinuxRational(result)
    }

    override val numerator: BigInt
        get() {
            val w = MpzWrapper.allocMpzWrapper()
            mpq_get_num!!(w.value, value)
            return BigInt(w)
        }

    override val denominator: BigInt
        get() {
            val w = MpzWrapper.allocMpzWrapper()
            mpq_get_den!!(w.value, value)
            return BigInt(w)
        }

    override fun plus(other: Rational) = basicOperation(other) { result, a, b ->
        mpq_add!!(result, a, b)
        mpq_canonicalize!!(result)
    }

    override fun minus(other: Rational) = basicOperation(other) { result, a, b ->
        mpq_sub!!(result, a, b)
        mpq_canonicalize!!(result)
    }

    override fun times(other: Rational) = basicOperation(other) { result, a, b ->
        mpq_mul!!(result, a, b)
        mpq_canonicalize!!(result)
    }

    override fun div(other: Rational): Rational {
        if (mpq_cmp_si_wrap((other as LinuxRational).value, 0, 1) == 0) {
            throw ArithmeticException("Division by zero")
        }
        return basicOperation(other) { result, a, b ->
            mpq_div!!(result, a, b)
            mpq_canonicalize!!(result)
        }
    }

    override fun unaryMinus() = basicOperation1Arg { result, a ->
        mpq_neg!!(result, a)
        mpq_canonicalize!!(result)
    }

    private inline fun <T> alignDenominator(other: LinuxRational, fn: (n0: mpz_t, n1: mpz_t, denominator: mpz_t) -> T): T {
        memScoped {
            val num0 = allocMpzStruct()
            mpq_get_num!!(num0, value)
            val den0 = allocMpzStruct()
            mpq_get_den!!(den0, value)
            val num1 = allocMpzStruct()
            mpq_get_num!!(num1, other.value)
            val den1 = allocMpzStruct()
            mpq_get_den!!(den1, other.value)
            val result = if (mpz_cmp!!(den0, den1) == 0) {
                fn(num0, num1, den0)
            } else {
                val common = allocMpzStruct()
                mpz_mul!!(common, den0, den1)

                val adjustedA = allocMpzStruct()
                mpz_mul!!(adjustedA, num0, den1)

                val adjustedB = allocMpzStruct()
                mpz_mul!!(adjustedB, num1, den0)

                val result0 = fn(adjustedA, adjustedB, common)

                mpz_clear!!(adjustedB)
                mpz_clear!!(adjustedA)
                mpz_clear!!(common)

                result0
            }
            mpz_clear!!(den1)
            mpz_clear!!(num1)
            mpz_clear!!(den0)
            mpz_clear!!(num0)
            return result
        }
    }

    fun printMpz(m: mpz_t): String {
        val size = mpz_sizeinbase!!(m, 10)
        memScoped {
            val buf = allocArray<ByteVar>(size.toLong() + 2)
            mpz_get_str!!(buf, 10, m)
            return buf.toKString()
        }
    }

    override fun rem(other: Rational): Rational {
        val result = nativeHeap.allocMpqStruct()
        alignDenominator(other as LinuxRational) { n0, n1, lcm ->
            memScoped {
                val r = allocMpzStruct()
                linuxBigIntRem(r, n0, n1)
                mpq_set_num!!(result, r)
                mpq_set_den!!(result, lcm)
                mpq_canonicalize!!(result)
                mpz_clear!!(r)
            }
        }
        return LinuxRational(result)
    }

    private fun mpzIsZero(v: mpz_t): Boolean {
        return mpz_cmpabs_ui!!(v, 0UL) == 0
    }

    override fun pow(other: Long): Rational {
        memScoped {
            val num0 = allocMpzStruct()
            mpq_get_num!!(num0, value)
            val den0 = allocMpzStruct()
            mpq_get_den!!(den0, value)

            val resultNum = allocMpzStruct()
            val resultDen = allocMpzStruct()

            when {
                other == 0L -> {
                    mpz_set_si!!(resultNum, 1)
                    mpz_set_si!!(resultDen, 1)
                }
                mpzIsZero(num0) -> {
                    mpz_set!!(resultNum, num0)
                    mpz_set!!(resultDen, den0)
                }
                other < 0 -> {
                    mpz_pow_ui!!(resultNum, den0, (-other).toULong())
                    mpz_pow_ui!!(resultDen, num0, (-other).toULong())
                }
                else -> {
                    mpz_pow_ui!!(resultNum, num0, other.toULong())
                    mpz_pow_ui!!(resultDen, den0, other.toULong())
                }
            }

            val result = nativeHeap.allocMpqStruct()
            mpq_set_num!!(result, resultNum)
            mpq_set_den!!(result, resultDen)
            mpq_canonicalize!!(result)

            mpz_clear!!(resultDen)
            mpz_clear!!(resultNum)
            mpz_clear!!(den0)
            mpz_clear!!(num0)

            return LinuxRational(result)
        }
    }

    override fun signum(): Int {
        return mpq_sgn_wrap(value)
    }

    override fun ceil(): BigInt {
        val num = numerator
        val den = denominator
        return if (mpz_cmp_si_wrap(den.inner, 1) == 0) {
            num
        } else {
            val res = MpzWrapper.allocMpzWrapper()
            mpz_cdiv_q!!(res.value, num.inner, den.inner)
            BigInt(res)
        }
    }

    override fun floor(): BigInt {
        val num = numerator
        val den = denominator
        return if (mpz_cmp_si_wrap(den.inner, 1) == 0) {
            num
        } else {
            val res = MpzWrapper.allocMpzWrapper()
            mpz_fdiv_q!!(res.value, num.inner, den.inner)
            BigInt(res)
        }
    }

    override fun toLongTruncated(): Long {
        memScoped {
            val num = numerator.inner
            val den = denominator.inner
            return when {
                mpz_cmp_si_wrap(den, 1) == 0 -> {
                    mpzToLong(num)
                }
                else -> {
                    val a = allocMpzStruct()
                    mpz_tdiv_q!!(a, num, den)
                    val result = mpzToLong(a)
                    mpz_clear!!(a)
                    result
                }
            }
        }
    }

    override fun toDouble(): Double {
        return mpq_get_d!!(value)
    }

    override fun compareTo(other: Rational): Int {
        val result = mpq_cmp!!(value, (other as LinuxRational).value)
        return when {
            result < 0 -> -1
            result > 0 -> 1
            else -> 0
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || this::class != other::class) return false
        other as LinuxRational
        return mpq_equal!!(value, other.value) != 0
    }

    override fun hashCode(): Int {
        memScoped {
            val num = allocMpzStruct()
            mpq_get_num!!(num, value)
            val numAsInt = mpz_get_si!!(num).toInt()
            val den = allocMpzStruct()
            mpq_get_den!!(den, value)
            val denAsInt = mpz_get_si!!(den).toInt()
            val result = 31 * numAsInt + denAsInt
            mpz_clear!!(den)
            mpz_clear!!(num)
            return result
        }
    }

    override fun toString(): String {
        return toString(10)
    }

    fun toString(radix: Int): String {
        memScoped {
            val num = allocMpzStruct()
            mpq_get_num!!(num, value)
            val den = allocMpzStruct()
            mpq_get_den!!(den, value)
            val size = mpz_sizeinbase!!(num, radix) + mpz_sizeinbase!!(den, radix)
            val buf = allocArray<ByteVar>(size.toLong() + 3)
            mpq_get_str!!(buf, radix, value)
            val result = buf.toKString()
            mpz_clear!!(den)
            mpz_clear!!(num)
            return result
        }
    }
}

actual fun Rational.Companion.make(a: BigInt, b: BigInt): Rational {
    return LinuxRational.make(a, b)
}

actual fun Rational.Companion.make(a: Long, b: Long): Rational {
    return LinuxRational.make(a, b)
}

actual fun Rational.Companion.make(a: String, b: String): Rational {
    return LinuxRational.make(a, b)
}
