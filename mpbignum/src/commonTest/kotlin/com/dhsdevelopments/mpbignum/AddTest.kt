package com.dhsdevelopments.mpbignum

import kotlin.test.Test
import kotlin.test.assertEquals

class AddTest {
    @Test
    fun addSmallValues() {
        val a = BigInt.of(10)
        val b = BigInt.of(100)
        val c = a + b
        assertEquals("110", c.toString())
    }

    @Test
    fun addBigintIntWithOverflow() {
        val a = BigInt.of(10)
        val b = BigInt.of(Int.MAX_VALUE)
        val c = a + b
        assertEquals("2147483657", c.toString())
    }

    @Test
    fun addBigIntToInt() {
        val a = 10
        val b = BigInt.of(Int.MAX_VALUE)
        val c = a + b
        assertEquals("2147483657", c.toString())
    }

    @Test
    fun addIntToBigInt() {
        val a = 10
        val b = BigInt.of(Int.MAX_VALUE)
        val c = b + a
        assertEquals("2147483657", c.toString())
    }

    @Test
    fun addBigIntToLong() {
        val a = 10L
        val b = BigInt.of(Int.MAX_VALUE)
        val c = a + b
        assertEquals("2147483657", c.toString())
    }

    @Test
    fun addLongToBigInt() {
        val a = 10L
        val b = BigInt.of(Int.MAX_VALUE)
        val c = b + a
        assertEquals("2147483657", c.toString())
    }
}
