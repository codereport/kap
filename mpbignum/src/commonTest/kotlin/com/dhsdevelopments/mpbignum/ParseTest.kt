package com.dhsdevelopments.mpbignum

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class ParseTest {
    @Test
    fun makeBignumFromInt() {
        val a = BigInt.of(10)
        assertEquals("10", a.toString())
    }

    @Test
    fun invalidNumber() {
        assertFailsWith<NumberFormatException> {
            BigInt.of("a")
        }
        assertFailsWith<NumberFormatException> {
            BigInt.of("")
        }
        assertFailsWith<NumberFormatException> {
            BigInt.of("123456w")
        }
    }

    @Test
    fun makeBignumFromString() {
        BigInt.of("100000000000000000000").let { a ->
            assertEquals("100000000000000000000", a.toString())
        }
        BigInt.of("1000000000000020").let { a ->
            assertEquals("1000000000000020", a.toString())
        }
    }

    @Test
    fun differentLengths() {
        repeat(200) { i ->
            val s = "1" + "0".repeat(i)
            val a = BigInt.of(s)
            assertEquals(s, a.toString(), "Failed at: ${i}")
        }
    }

    @Test
    fun convertToLong() {
        val a = BigInt.of(10)
        val b = a.toLong()
        assertEquals(10L, b)
    }

    @Test
    fun convertOverflowToLong() {
        val a = BigInt.of(Long.MAX_VALUE)
        val b = a + 3
        val c = b.toLong()
        assertEquals(Long.MIN_VALUE + 2, c)
    }

    @Test
    fun convertToLongNegative() {
        val a = BigInt.of(-2)
        val b = a.toLong()
        assertEquals(-2, b)
    }

    @Test
    fun convertToLongLargeNegative() {
        val a = BigInt.of(Long.MIN_VALUE)
        val b = a - 2
        val c = b.toLong()
        assertEquals(Long.MAX_VALUE - 1, c)
    }

    @Test
    fun convertToLongMinLong() {
        val a = BigInt.of(Long.MIN_VALUE)
        val b = a.toLong()
        assertEquals(Long.MIN_VALUE, b)
    }

    @Test
    fun convertToLongMaxLong() {
        val a = BigInt.of(Long.MAX_VALUE)
        val b = a.toLong()
        assertEquals(Long.MAX_VALUE, b)
    }

    @Test
    fun convertToDouble() {
        val a = BigInt.of(3)
        val b = a.toDouble()
        assertEquals(3.0, b)
    }

    @Test
    fun equalsTest() {
        val a = BigInt.of(10)
        val b = BigInt.of(10)
        assertEquals(a, b)
    }

    @Test
    fun notEquals() {
        val a = BigInt.of(10)
        val b = BigInt.of(11)
        assertNotEquals(a, b)
    }

    @Test
    fun testSignum() {
        assertEquals(-1, BigInt.of(-2).signum())
        assertEquals(1, BigInt.of(2).signum())
        assertEquals(0, BigInt.of(0).signum())
    }
}
