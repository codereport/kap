@file:Suppress("NOTHING_TO_INLINE")

package com.dhsdevelopments.mpbignum

actual inline fun addExact(a: Long, b: Long): Long {
    return Math.addExact(a, b)
}

actual inline fun subExact(a: Long, b: Long): Long {
    return Math.subtractExact(a, b)
}

actual inline fun mulExact(a: Long, b: Long): Long {
    return Math.multiplyExact(a, b)
}

@Throws(LongExpressionOverflow::class)
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
        return Math.subtractExact(a, b)
    } catch (e: ArithmeticException) {
        throw LongExpressionOverflow(BigInt.of(a) - BigInt.of(b))
    }
}

@Throws(LongExpressionOverflow::class)
actual inline fun mulExactWrapped(a: Long, b: Long): Long {
    try {
        return mulExact(a, b)
    } catch (e: ArithmeticException) {
        throw LongExpressionOverflow(BigInt.of(a) * BigInt.of(b))
    }
}
