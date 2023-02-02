package com.dhsdevelopments.mpbignum

import kotlin.test.Test
import kotlin.test.assertEquals

class MulTest {
    @Test
    fun multiplyBigInt() {
        val a = BigInt.of("100000000000000000000")
        val b = BigInt.of(12)
        val c = a * b
        assertEquals("1200000000000000000000", c.toString())
    }

    @Test
    fun multiplyBigIntWithInt() {
        val a = BigInt.of("100000000000000000000")
        val b = 12
        val c = a * b
        assertEquals("1200000000000000000000", c.toString())
    }

    @Test
    fun multiplyIntWithBigInt() {
        val a = BigInt.of("100000000000000000000")
        val b = 12
        val c = b * a
        assertEquals("1200000000000000000000", c.toString())
    }

    @Test
    fun multiplyBigIntWithLong() {
        val a = BigInt.of("100000000000000000000")
        val b = 12L
        val c = a * b
        assertEquals("1200000000000000000000", c.toString())
    }

    @Test
    fun multiplyLongWithBigInt() {
        val a = BigInt.of("100000000000000000000")
        val b = 12L
        val c = b * a
        assertEquals("1200000000000000000000", c.toString())
    }
}
