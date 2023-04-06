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
    return a * b
}

actual inline fun addExactWrapped(a: Long, b: Long): Long {
    try {
        return addExact(a, b)
    } catch (e: ArithmeticException) {
        throw LongExpressionOverflow(BigInt.of(a) + BigInt.of(b))
    }
}

@Throws(LongExpressionOverflow::class)
actual inline fun subExactWrapped(a: Long, b: Long): Long {
    try {
        return subExact(a, b)
    } catch (e: ArithmeticException) {
        throw LongExpressionOverflow(BigInt.of(a) - BigInt.of(b))
    }
}

actual inline fun mulExactWrapped(a: Long, b: Long): Long {
    try {
        return mulExact(a, b)
    } catch (e: ArithmeticException) {
        throw LongExpressionOverflow(BigInt.of(a) * BigInt.of(b))
    }
}
