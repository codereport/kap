package com.dhsdevelopments.mpbignum

import kotlin.test.Test
import kotlin.test.assertEquals

class SubTest {
    @Test
    fun subtractBigIntWithOverflow() {
        val a = BigInt.of(10)
        val b = BigInt.of(Int.MIN_VALUE)
        val c = b - a
        assertEquals("-2147483658", c.toString())
    }

    @Test
    fun subtractIntFromBignum() {
        val a = BigInt.of("1000000000000020")
        val b = 20
        val c = a - b
        assertEquals("1000000000000000", c.toString())
    }

    @Test
    fun subtractBignumFromInt() {
        val a = BigInt.of("10")
        val b = 30
        val c = b - a
        assertEquals("20", c.toString())
    }

    @Test
    fun subtractLongFromBignum() {
        val a = BigInt.of("1000000000000020")
        val b = 20L
        val c = a - b
        assertEquals("1000000000000000", c.toString())
    }

    @Test
    fun subtractBignumFromLong() {
        val a = BigInt.of("10")
        val b = 30L
        val c = b - a
        assertEquals("20", c.toString())
    }

    @Test
    fun negate0() {
        val a = BigInt.of(10)
        val b = -a
        assertEquals("-10", b.toString())
    }

    @Test
    fun negate1() {
        val a = BigInt.of(-10)
        val b = -a
        assertEquals("10", b.toString())
    }
}
