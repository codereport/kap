package com.dhsdevelopments.mpbignum

import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RationalTest {
    @Test
    fun negativeDenominator() {
        val a = mkrational("1", "-2")
        assertEquals(mkrational("-1", "2"), a)
    }

    @Test
    fun numAndDenNegative() {
        val a = mkrational("-1", "-2")
        assertEquals(mkrational("1", "2"), a)
    }

    @Test
    fun zeroDenominatorShouldFail() {
        assertFailsWith<IllegalArgumentException> {
            mkrational(5, 0)
        }
    }

    @Test
    fun addSimple() {
        val a = Rational.make(BigInt.of(10), BigInt.of(11))
        val b = Rational.make(BigInt.of(20), BigInt.of(12))
        val c = a + b
        assertEquals(Rational.make(BigInt.of(85), BigInt.of(33)), c)
    }

    @Test
    fun addZero() {
        val a = Rational.make(BigInt.of(10), BigInt.of(11))
        val b = Rational.make(BigInt.of(0), BigInt.of(1))
        val c = a + b
        assertEquals(Rational.make(BigInt.of(10), BigInt.of(11)), c)
    }

    @Test
    fun addNegativeToPositive() {
        val a = mkrational("-10", "3")
        val b = mkrational("1", "2")
        val c = a + b
        assertEquals(mkrational("-17", "6"), c)
    }

    @Test
    fun subtractSimple0() {
        val a = mkrational(3, 2)
        val b = mkrational(1, 2)
        val c = a - b
        assertEquals(mkrational(1, 1), c)
    }

    @Test
    fun subtractSimple1() {
        val a = mkrational(31, 9)
        val b = mkrational(2, 43)
        val c = a - b
        assertEquals(mkrational(1315, 387), c)
    }

    @Test
    fun subtractNegative() {
        val a = mkrational(-4, 3)
        val b = mkrational(-4, 9)
        val c = a - b
        assertEquals(mkrational(-8, 9), c)
    }

    @Test
    fun negateSimple() {
        val a = mkrational(3, 1)
        val b = -a
        assertEquals(mkrational(-3, 1), b)
    }

    @Test
    fun negateNegative() {
        val a = mkrational(-5, 4)
        val b = -a
        assertEquals(mkrational(5, 4), b)
    }

    @Test
    fun negateZero() {
        val a = mkrational(0, 1)
        val b = -a
        assertEquals(mkrational(0, 1), b)
    }

    @Test
    fun multiplePositive0() {
        val a = mkrational(1, 2)
        val b = mkrational(2, 5)
        val c = a * b
        assertEquals(mkrational(1, 5), c)
    }

    @Test
    fun multiplePositive1() {
        val a = mkrational(1, 2)
        val b = mkrational(4, 2)
        val c = a * b
        assertEquals(mkrational(1, 1), c)
    }

    @Test
    fun dividePositive0() {
        val a = mkrational(2, 1)
        val b = mkrational(1, 2)
        val c = a / b
        assertEquals(mkrational(4, 1), c)
    }

    @Test
    fun dividePositive1() {
        val a = mkrational(7, 1)
        val b = mkrational(1, 2)
        val c = a / b
        assertEquals(mkrational(14, 1), c)
    }

    @Test
    fun dividePositive3() {
        val a = mkrational(1, 1)
        val b = mkrational(1, 2)
        val c = a / b
        assertEquals(mkrational(2, 1), c)
    }

    @Test
    fun divideByZeroShouldThrow() {
        val a = mkrational(1, 1)
        val b = mkrational(0, 1)
        assertFailsWith<IllegalArgumentException> {
            a / b
        }
    }

    @Test
    fun testConvertToDouble0() {
        val a = mkrational(1, 2)
        val b = a.toDouble()
        assertDouble(0.5, 4, b)
    }

    @Test
    fun testConvertToDouble1() {
        val a = mkrational("10000000", "92")
        val b = a.toDouble()
        assertDouble(108695.65217391304, 4, b)
    }

    private fun assertDouble(expected: Double, precision: Int, result: Double) {
        val dist = 10.0.pow(-precision)
        assertTrue(expected > result - dist && expected < result + dist, "Expected=${expected}, result=${result}")

    }

    private fun mkrational(a: Long, b: Long): Rational {
        return Rational.make(BigInt.of(a), BigInt.of(b))
    }

    private fun mkrational(a: String, b: String): Rational {
        return Rational.make(BigInt.of(a), BigInt.of(b))
    }
}
