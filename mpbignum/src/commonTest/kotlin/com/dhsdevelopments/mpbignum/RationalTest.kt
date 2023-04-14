package com.dhsdevelopments.mpbignum

import kotlin.math.pow
import kotlin.test.*

class RationalTest {
    @Test
    fun negativeDenominator() {
        val a = mkrational("1", "-2")
        assertRationalInt("-1", "2", a)
    }

    @Test
    fun numAndDenNegative() {
        val a = mkrational("-1", "-2")
        assertRationalInt("1", "2", a)
    }

    @Test
    fun zeroDenominatorShouldFail() {
        assertFailsWith<ArithmeticException> {
            mkrational(5, 0)
        }
    }

    @Test
    fun createFromLong() {
        assertRationalInt(9, 8, Rational.make(9, 8))
        assertRationalInt(-3, 2, Rational.make(-3, 2))
        assertRationalInt(1, 8, Rational.make(-8, -64))
        assertRationalInt(-3, 4, Rational.make(6, -8))
    }

    @Test
    fun createFromString() {
        assertRationalInt(1, 2, Rational.make("1", "2"))
        assertRationalInt(-3, 2, Rational.make("-3", "2"))
        assertRationalInt(4, 3, Rational.make("-4", "-3"))
        assertRationalInt(-3, 4, Rational.make("3", "-4"))
    }

    @Test
    fun testDenormalised0() {
        val a = mkrational(4, 12)
        assertRationalInt(1, 3, a)
    }

    @Test
    fun testDenormalised1() {
        val a = mkrational(-4, 12)
        assertRationalInt(-1, 3, a)
    }

    @Test
    fun testDenormalised2() {
        val a = mkrational(4, -12)
        assertRationalInt(-1, 3, a)
    }

    @Test
    fun testDenormalised3() {
        val a = mkrational(-4, -12)
        assertRationalInt(1, 3, a)
    }

    @Test
    fun testDenormalised4() {
        val a = mkrational(-1, 3)
        assertRationalInt(-1, 3, a)
    }

    @Test
    fun testDenormalised5() {
        val a = mkrational(1, -3)
        assertRationalInt(-1, 3, a)
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
        assertRationalInt("-17", "6", c)
    }

    @Test
    fun subtractSimple0() {
        val a = mkrational(3, 2)
        val b = mkrational(1, 2)
        val c = a - b
        assertRationalInt(1, 1, c)
    }

    @Test
    fun subtractSimple1() {
        val a = mkrational(31, 9)
        val b = mkrational(2, 43)
        val c = a - b
        assertRationalInt(1315, 387, c)
    }

    @Test
    fun subtractNegative() {
        val a = mkrational(-4, 3)
        val b = mkrational(-4, 9)
        val c = a - b
        assertRationalInt(-8, 9, c)
    }

    @Test
    fun negateSimple() {
        val a = mkrational(3, 1)
        val b = -a
        assertRationalInt(-3, 1, b)
    }

    @Test
    fun negateNegative() {
        val a = mkrational(-5, 4)
        val b = -a
        assertRationalInt(5, 4, b)
    }

    @Test
    fun negateZero() {
        val a = mkrational(0, 1)
        val b = -a
        assertRationalInt(0, 1, b)
    }

    @Test
    fun multiplePositive0() {
        val a = mkrational(1, 2)
        val b = mkrational(2, 5)
        val c = a * b
        assertRationalInt(1, 5, c)
    }

    @Test
    fun multiplePositive1() {
        val a = mkrational(1, 2)
        val b = mkrational(4, 2)
        val c = a * b
        assertRationalInt(1, 1, c)
    }

    @Test
    fun dividePositive0() {
        val a = mkrational(2, 1)
        val b = mkrational(1, 2)
        val c = a / b
        assertRationalInt(4, 1, c)
    }

    @Test
    fun dividePositive1() {
        val a = mkrational(7, 1)
        val b = mkrational(1, 2)
        val c = a / b
        assertRationalInt(14, 1, c)
    }

    @Test
    fun dividePositive3() {
        val a = mkrational(1, 1)
        val b = mkrational(1, 2)
        val c = a / b
        assertRationalInt(2, 1, c)
    }

    @Test
    fun divideByZeroShouldThrow() {
        val a = mkrational(1, 1)
        val b = mkrational(0, 1)
        assertFailsWith<ArithmeticException> {
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

    @Test
    fun testMod0() {
        val a = mkrational(7, 3)
        val b = mkrational(1, 5)
        val c = a % b
        assertRationalInt(2, 15, c)
    }

    @Test
    fun testMod1() {
        val a = mkrational(-100, 9)
        val b = mkrational(6, 7)
        val c = a % b
        assertRationalInt(-52, 63, c)
    }

    @Test
    fun testMod2() {
        val a = mkrational(100, 9)
        val b = mkrational(-6, 7)
        val c = a % b
        assertRationalInt(52, 63, c)
    }

    @Test
    fun testMod4() {
        val a = mkrational(-100, 9)
        val b = mkrational(-6, 7)
        val c = a % b
        assertRationalInt(-52, 63, c)
    }

    @Test
    fun testPower0() {
        val a = mkrational(1, 2)
        val b = a.pow(5)
        assertRationalInt(1, 32, b)
    }

    @Test
    fun testPower1() {
        val a = mkrational(3, 4)
        val b = -10L
        val c = a.pow(b)
        assertRationalInt(1048576, 59049, c)
    }

    @Test
    fun testPower2() {
        val a = mkrational(-8, 5)
        val b = 9L
        val c = a.pow(b)
        assertRationalInt(-134217728, 1953125, c)
    }

    @Test
    fun testPower3() {
        val a = mkrational(2, 3)
        val b = a.pow(0)
        assertRationalInt(1, 1, b)
    }

    @Test
    fun testPower4() {
        val a = mkrational(0, 1)
        val b = a.pow(2)
        assertRationalInt(0, 1, b)
    }

    @Test
    fun testCompare0() {
        val a = mkrational(1, 1)
        val b = mkrational(2, 1)
        val c = mkrational(1, 1)
        assertTrue(a < b)
        assertFalse(a > b)
        assertTrue(a <= b)
        assertFalse(a >= b)
        assertTrue(a <= c)
    }

    @Test
    fun testCompare1() {
        val a = mkrational(1, 2)
        val b = mkrational(1, 3)
        assertTrue(a > b)
    }

    @Test
    fun testCompare2() {
        val a = mkrational(10000, 3)
        val b = mkrational(-2, 5)
        assertTrue(a > b)
        assertFalse(a < b)
    }

    @Test
    fun testCompare3() {
        val a = mkrational(-2, 7)
        val b = mkrational(1000000, 12345)
        assertTrue(a < b)
        assertFalse(a > b)
    }

    @Test
    fun testEquals0() {
        val a = mkrational(3, 4)
        val b = mkrational(3, 4)
        assertEquals(a, b)
    }

    @Test
    fun testEquals1() {
        val a = mkrational(3, 4)
        val b = mkrational(-3, 4)
        assertNotEquals(a, b)
    }

    @Test
    fun testEquals2() {
        val a = mkrational(3, 4)
        val b = mkrational(3, -4)
        assertNotEquals(a, b)
    }

    @Test
    fun testEquals3() {
        val a = mkrational(3, -4)
        val b = mkrational(3, 4)
        assertNotEquals(a, b)
    }

    @Test
    fun testEquals4() {
        val a = mkrational(-3, 4)
        val b = mkrational(3, 4)
        assertNotEquals(a, b)
    }

    @Test
    fun testEquals5() {
        val a = mkrational(3, 4)
        val b = mkrational(5, 6)
        assertNotEquals(a, b)
    }

    @Test
    fun testEquals6() {
        val a = mkrational(0, 1)
        val b = mkrational(0, 1)
        assertEquals(a, b)
    }

    @Test
    fun testSignum0() {
        val a = mkrational(2, 1)
        val b = a.signum()
        assertEquals(1, b)
    }

    @Test
    fun testSignum1() {
        val a = mkrational(-2, 1)
        val b = a.signum()
        assertEquals(-1, b)
    }

    @Test
    fun testSignum2() {
        val a = mkrational(0, 1)
        val b = a.signum()
        assertEquals(0, b)
    }

    @Test
    fun testConvertToLong0() {
        val a = mkrational(2, 1)
        val b = a.toLongTruncated()
        assertEquals(2, b)
    }

    @Test
    fun testConvertToLong1() {
        val a = mkrational(14, 3)
        val b = a.toLongTruncated()
        assertEquals(4, b)
    }

    @Test
    fun testConvertToLong2() {
        val a = mkrational(-2, 1)
        val b = a.toLongTruncated()
        assertEquals(-2, b)
    }

    @Test
    fun testConvertToLong3() {
        val a = mkrational(-10, 3)
        val b = a.toLongTruncated()
        assertEquals(-3, b)
    }

    @Test
    fun testConvertToLong4() {
        val a = mkrational(Long.MAX_VALUE, 1) + 12
        val b = a.toLongTruncated()
        assertEquals(Long.MIN_VALUE + 11, b)
    }

    @Test
    fun ceilTest() {
        assertEquals(BigInt.of(2), mkrational(12, 11).ceil())
        assertEquals(BigInt.of(0), mkrational(-1, 3).ceil())
        assertEquals(BigInt.of(5), mkrational(14, 3).ceil())
        assertEquals(BigInt.of(-4), mkrational(-14, 3).ceil())
        assertEquals(BigInt.of(12), mkrational(12, 1).ceil())
        assertEquals(BigInt.of(-12), mkrational(-12, 1).ceil())
        assertEquals(BigInt.of(3), mkrational(11, 4).ceil())
        assertEquals(BigInt.of(-2), mkrational(-11, 4).ceil())
    }

    @Test
    fun floorTest() {
        assertEquals(BigInt.of(-3), mkrational(-27, 11).floor())
        assertEquals(BigInt.of(0), mkrational(1, 3).floor())
        assertEquals(BigInt.of(4), mkrational(21, 5).floor())
        assertEquals(BigInt.of(4), mkrational(4, 1).floor())
        assertEquals(BigInt.of(-4), mkrational(-4, 1).floor())
        assertEquals(BigInt.of(-5), mkrational(-14, 3).floor())
    }

    @Test
    fun absTest() {
        assertRationalInt(3, 4, mkrational(3, 4).absoluteValue)
        assertRationalInt(3, 4, mkrational(-3, 4).absoluteValue)
        assertRationalInt(0, 1, mkrational(0, 1).absoluteValue)
        assertRationalInt("61728394506172839450617283900", "1", mkrational("123456789012345678901234567800", "2").absoluteValue)
        assertRationalInt("2057613150205761315020576130", "1", mkrational("-12345678901234567890123456780", "6").absoluteValue)
        assertRationalInt("12345678901234567890123456788", "11", mkrational("-12345678901234567890123456788", "11").absoluteValue)
    }

    private fun assertRationalInt(a: Long, b: Long, result: Rational) {
        assertEquals(a.toBigInt(), result.numerator)
        assertEquals(b.toBigInt(), result.denominator)
    }

    private fun assertRationalInt(a: String, b: String, result: Rational) {
        assertEquals(BigInt.of(a), result.numerator)
        assertEquals(BigInt.of(b), result.denominator)
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
