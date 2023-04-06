package com.dhsdevelopments.mpbignum

class APLValueAtOverflow(val result: BigInt) : Exception()

expect inline fun addExact(a: Long, b: Long): Long
expect inline fun subExact(a: Long, b: Long): Long
expect inline fun mulExact(a: Long, b: Long): Long

@Throws(APLValueAtOverflow::class)
expect inline fun addExactWrapped(a: Long, b: Long): Long

@Throws(APLValueAtOverflow::class)
expect inline fun mulExactWrapped(a: Long, b: Long): Long
