package com.dhsdevelopments.mpbignum

import kotlin.test.Test
import kotlin.test.assertEquals

class DivTest {
    @Test
    fun divideBigInt() {
        val a = BigInt.of("100000000000000000000")
        val b = BigInt.of(20)
        val c = a / b
        assertEquals("5000000000000000000", c.toString())
    }

    @Test
    fun divideBigIntWithTruncate() {
        val a = BigInt.of("100000000000000000000000000")
        val b = BigInt.of(3)
        val c = a / b
        assertEquals("33333333333333333333333333", c.toString())
    }

    @Test
    fun divideBigIntWithInt() {
        val a = BigInt.of("100")
        val b = 20
        val c = a / b
        assertEquals("5", c.toString())
    }

    @Test
    fun divideIntWithBigInt() {
        val a = 100
        val b = BigInt.of("20")
        val c = a / b
        assertEquals("5", c.toString())
    }

    @Test
    fun divideBigIntWithLong() {
        val a = BigInt.of("100")
        val b = 20L
        val c = a / b
        assertEquals("5", c.toString())
    }

    @Test
    fun divideLongWithBigInt() {
        val a = 100L
        val b = BigInt.of("20")
        val c = a / b
        assertEquals("5", c.toString())
    }

    @Test
    fun divideWithFractionalResult() {
        val a = BigInt.of(1000)
        val b = BigInt.of(21)
        val c = a / b
        assertEquals("47", c.toString())
    }

    @Test
    fun divideNegativeValue() {
        val a = BigInt.of(-20)
        val b = BigInt.of(4)
        val c = a / b
        assertEquals("-5", c.toString())
    }
}
