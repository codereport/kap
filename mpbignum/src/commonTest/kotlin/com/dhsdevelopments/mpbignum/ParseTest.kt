package com.dhsdevelopments.mpbignum

import kotlin.test.*

class ParseTest {
    @Test
    fun makeBignumFromInt() {
        val a = BigInt.of(10)
        assertEquals("10", a.toString())
    }

    @Test
    fun parseHexNumber() {
        val a = BigInt.of("a", 16)
        assertEquals("10", a.toString())
    }

    @Test
    fun parseUppercaseHex() {
        val a = BigInt.of("ABCDEFabcdef", 16)
        assertEquals("abcdefabcdef", a.toString(16))
    }

    @Test
    fun parseWithDigitOutsideRange() {
        assertFailsWith<NumberFormatException> {
            BigInt.of("m", 20)
        }
    }

    @Test
    fun parseBase36() {
        val a = BigInt.of("z0", 36)
        assertEquals("1260", a.toString())
    }

    @Test
    fun parseLargeHex() {
        val a = BigInt.of("abcdefabcdefabcdef12345678901234567890", 16)
        assertEquals("3831367003417710754922696341706783590538705040", a.toString())
    }

    @Test
    fun parseNegativeHex() {
        val a = BigInt.of("-123456789abcdefabcdef98765", 16)
        assertEquals("-1442304682926341000888262952805", a.toString())
    }

    @Test
    fun parseFullRangeBase36() {
        val a = BigInt.of("9876543210abcdefghijklmnopqrstuvwxyz123", 36)
        assertEquals("1272304576354861089273994929606034399355711171729583749543451", a.toString())
    }

    @Test
    fun parseFullRangeNegativeBase36() {
        val a = BigInt.of("-9876543210abcdefghijklmnopqrstuvwxyz123", 36)
        assertEquals("-1272304576354861089273994929606034399355711171729583749543451", a.toString())
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
    fun convertToInt() {
        val a = BigInt.of(123)
        val b = a.toInt()
        assertEquals(123, b)
    }

    @Test
    fun convertToIntNegative() {
        val a = BigInt.of(-2)
        val b = a.toInt()
        assertEquals(-2, b)
    }

    @Test
    fun convertToIntMinInt() {
        val a = BigInt.of(Int.MIN_VALUE)
        val b = a.toInt()
        assertEquals(Int.MIN_VALUE, b)
    }

    @Test
    fun convertToIntMaxInt() {
        val a = BigInt.of(Int.MAX_VALUE)
        val b = a.toInt()
        assertEquals(Int.MAX_VALUE, b)
    }

    @Test
    fun convertOverflowToInt() {
        val a = BigInt.of(Int.MAX_VALUE)
        val b = a + 3
        val c = b.toInt()
        assertEquals(Int.MIN_VALUE + 2, c)
    }

    @Test
    fun convertToIntZero() {
        assertEquals(0, BigInt.of("0").toInt())
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

    @Test
    fun testRangeInLong() {
        assertTrue(BigInt.of("9223372036854775807").rangeInLong())
        assertFalse(BigInt.of("9223372036854775808").rangeInLong())
        assertTrue(BigInt.of("2147483647").rangeInLong())
        assertTrue(BigInt.of("2147483648").rangeInLong())
        assertTrue(BigInt.of("0").rangeInLong())
        assertTrue(BigInt.of("1").rangeInLong())
        assertTrue(BigInt.of("-2147483648").rangeInLong())
        assertTrue(BigInt.of("2147483649").rangeInLong())
        assertTrue(BigInt.of("-9223372036854775808").rangeInLong())
        assertFalse(BigInt.of("-9223372036854775809").rangeInLong())
        assertFalse(BigInt.of("-1000000000000000000000000000").rangeInLong())
        assertFalse(BigInt.of("1000000000000000000000000000").rangeInLong())
    }

    @Test
    fun convertFromDoubleStandard() {
        assertEquals(BigInt.of(1), BigInt.of(1.0))
        assertEquals(BigInt.of(1), BigInt.of(1.1))
        assertEquals(BigInt.of(0), BigInt.of(0.0))
        assertEquals(BigInt.of(-2), BigInt.of(-2.2))
        assertEquals(BigInt.of(-200000), BigInt.of(-200000.9999999))
    }

    @Test
    fun convertFromDoubleFloor() {
        assertEquals(BigInt.of(1), BigInt.fromDoubleFloor(1.0))
        assertEquals(BigInt.of(1), BigInt.fromDoubleFloor(1.1))
        assertEquals(BigInt.of(0), BigInt.fromDoubleFloor(0.0))
        assertEquals(BigInt.of(-2), BigInt.fromDoubleFloor(-1.1))
        assertEquals(BigInt.of(-2), BigInt.fromDoubleFloor(-1.6))
        assertEquals(BigInt.of(-100000000), BigInt.fromDoubleFloor(-99999999.999))
// commented for now because conversion in js is broken
//        assertEquals(BigInt.of("6200000000000000000000000000000"), BigInt.fromDoubleFloor(6.2e30))
    }

    @Test
    fun convertFromDoubleCeil() {
        assertEquals(BigInt.of(1), BigInt.fromDoubleCeil(1.0))
        assertEquals(BigInt.of(2), BigInt.fromDoubleCeil(1.1))
        assertEquals(BigInt.of(0), BigInt.fromDoubleCeil(-0.3))
        assertEquals(BigInt.of(0), BigInt.fromDoubleCeil(0.0))
        assertEquals(BigInt.of(1), BigInt.fromDoubleCeil(0.000001))
        assertEquals(BigInt.of(1), BigInt.fromDoubleCeil(1e-95))
        assertEquals(BigInt.of(10000001), BigInt.fromDoubleCeil(10000000.1))
//        assertEquals(BigInt.of("1000000000000000000000000000000"), BigInt.fromDoubleCeil(1.0e30))
    }
}
