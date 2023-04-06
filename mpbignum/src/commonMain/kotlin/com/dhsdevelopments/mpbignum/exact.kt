package com.dhsdevelopments.mpbignum

class LongExpressionOverflow(val result: BigInt) : Exception()

expect inline fun addExact(a: Long, b: Long): Long
expect inline fun subExact(a: Long, b: Long): Long
expect inline fun mulExact(a: Long, b: Long): Long

@Throws(LongExpressionOverflow::class)
expect inline fun addExactWrapped(a: Long, b: Long): Long

@Throws(LongExpressionOverflow::class)
expect inline fun subExactWrapped(a: Long, b: Long): Long

@Throws(LongExpressionOverflow::class)
expect inline fun mulExactWrapped(a: Long, b: Long): Long
