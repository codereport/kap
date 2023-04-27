@file:Suppress("NOTHING_TO_INLINE")

package com.dhsdevelopments.mpbignum

actual inline fun addExact(a: Long, b: Long): Long {
    if (a > 0 && b > 0) {
        if (a > Long.MAX_VALUE - b) {
            throw ArithmeticException()
        }
    } else if (a < 0 && b < 0) {
        if (a < Long.MIN_VALUE - b) {
            throw ArithmeticException()
        }
    }
    return a + b
}

actual inline fun subExact(a: Long, b: Long): Long {
    if ((b <= 0 && a > Long.MAX_VALUE + b) || (b > 0 && a < Long.MIN_VALUE + b)) {
        throw ArithmeticException()
    }
    return a - b
}

actual inline fun mulExact(a: Long, b: Long): Long {
    if ((a or b) and -0x80000000 != 0L) {
        if (a > 0) {
            if (b > 0) {
                if (a > Long.MAX_VALUE / b) {
                    throw ArithmeticException()
                }
            } else {
                if (b < Long.MIN_VALUE / a) {
                    throw ArithmeticException()
                }
            }
        } else {
            if (b > 0) {
                if (a < Long.MIN_VALUE / b) {
                    throw ArithmeticException()
                }
            } else {
                if (a != 0L && b < Long.MAX_VALUE / a) {
                    throw ArithmeticException()
                }
            }
        }
    }
    return a * b
}

// This avoids an unnecessary object creation in the resulting Javascript
val LONG_0 = 0L

actual inline fun addExactWrapped(a: Long, b: Long): Long {
    val r = a + b
    if ((a xor r) and (b xor r) < LONG_0) {
        throw LongExpressionOverflow(BigInt.of(a) + BigInt.of(b))
    }
    return r
}

actual inline fun subExactWrapped(a: Long, b: Long): Long {
    val r = a - b
    // HD 2-12 Overflow iff the arguments have different signs and
    // the sign of the result is different from the sign of x
    if (a xor b and (a xor r) < 0) {
        throw LongExpressionOverflow(BigInt.of(a) - BigInt.of(b))
    }
    return r
}

actual inline fun mulExactWrapped(a: Long, b: Long): Long {
    if ((a or b) and -0x80000000 != 0L) {
        if (a > 0) {
            if (b > 0) {
                if (a > Long.MAX_VALUE / b) {
                    throw LongExpressionOverflow(BigInt.of(a) * BigInt.of(b))
                }
            } else {
                if (b < Long.MIN_VALUE / a) {
                    throw LongExpressionOverflow(BigInt.of(a) * BigInt.of(b))
                }
            }
        } else {
            if (b > 0) {
                if (a < Long.MIN_VALUE / b) {
                    throw LongExpressionOverflow(BigInt.of(a) * BigInt.of(b))
                }
            } else {
                if (a != 0L && b < Long.MAX_VALUE / a) {
                    throw LongExpressionOverflow(BigInt.of(a) * BigInt.of(b))
                }
            }
        }
    }
    return a * b
}
