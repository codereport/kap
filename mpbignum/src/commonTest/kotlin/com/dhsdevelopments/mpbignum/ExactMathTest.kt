package com.dhsdevelopments.mpbignum

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

class ExactMathTest {
    @Test
    fun addNoException() {
        val a = 2L
        val b = 4L
        assertEquals(6L, addExact(a, b))
    }

    @Test
    fun addNoExceptionWrapped() {
        val a = 2L
        val b = 4L
        assertEquals(6L, addExactWrapped(a, b))
    }

    @Test
    fun addExceptionPositive() {
        val a = Long.MAX_VALUE
        val b = 1L
        assertFailsWith<ArithmeticException> {
            addExact(a, b)
        }
    }

    @Test
    fun addExceptionPositiveWrapped() {
        val a = Long.MAX_VALUE
        val b = 1L
        try {
            addExactWrapped(a, b)
            fail("call to addExactWrapped should throw an exception")
        } catch (e: LongExpressionOverflow) {
            assertEquals("9223372036854775808", e.result.toString())
        }
    }

    @Test
    fun addExceptionNegative() {
        val a = Long.MIN_VALUE
        val b = -1L
        assertFailsWith<ArithmeticException> {
            addExact(a, b)
        }
    }

    @Test
    fun addExceptionNegativeWrapped() {
        val a = Long.MIN_VALUE
        val b = -1L
        try {
            addExactWrapped(a, b)
            fail("call to subExactWrapped should throw an exception")
        } catch (e: LongExpressionOverflow) {
            assertEquals("-9223372036854775809", e.result.toString())
        }
    }

    @Test
    fun subNoException() {
        val a = 8L
        val b = 7L
        assertEquals(1L, subExact(a, b))
    }

    @Test
    fun subNoExceptionWrapped() {
        val a = 8L
        val b = 7L
        assertEquals(1L, subExactWrapped(a, b))
    }

    @Test
    fun subExceptionNegative() {
        val a = Long.MIN_VALUE
        val b = 1L
        assertFailsWith<ArithmeticException> {
            subExact(a, b)
        }
    }

    @Test
    fun subExceptionNegativeWrapped() {
        val a = Long.MIN_VALUE
        val b = 1L
        try {
            subExactWrapped(a, b)
            fail("call to subExactWrapped should throw an exception")
        } catch (e: LongExpressionOverflow) {
            assertEquals("-9223372036854775809", e.result.toString())
        }
    }

    @Test
    fun subExceptionPositive() {
        val a = Long.MAX_VALUE
        val b = -1L
        assertFailsWith<ArithmeticException> {
            subExact(a, b)
        }
    }

    @Test
    fun subExceptionPositiveWrapped() {
        val a = Long.MAX_VALUE
        val b = -1L
        try {
            subExactWrapped(a, b)
            fail("call to subExactWrapped should throw an exception")
        } catch (e: LongExpressionOverflow) {
            assertEquals("9223372036854775808", e.result.toString())
        }
    }

    @Test
    fun mulNoException() {
        val a = 2L
        val b = 8L
        assertEquals(16L, mulExact(a, b))
    }

    @Test
    fun mulNoExceptionWrapped() {
        val a = 2L
        val b = 8L
        assertEquals(16L, mulExactWrapped(a, b))
    }

    @Test
    fun mulPositive() {
        val a = 2L
        val b = Long.MAX_VALUE / 2 + 1
        assertFailsWith<ArithmeticException> {
            mulExact(a, b)
        }
    }

    @Test
    fun mulPositiveWrapped() {
        val a = 2L
        val b = Long.MAX_VALUE / 2 + 1
        try {
            mulExactWrapped(a, b)
            fail("call to mulExactWrapped should throw an exception")
        } catch (e: LongExpressionOverflow) {
            assertEquals("9223372036854775808", e.result.toString())
        }
    }

    @Test
    fun mulNegative0() {
        val a = -2L
        val b = Long.MIN_VALUE / 2 - 1
        assertFailsWith<ArithmeticException> {
            mulExact(a, b)
        }
    }

    @Test
    fun mulNegativeWrapped0() {
        val a = -2L
        val b = Long.MIN_VALUE / 2 - 1
        try {
            mulExactWrapped(a, b)
            fail("call to mulExactWrapped should throw an exception")
        } catch (e: LongExpressionOverflow) {
            assertEquals("9223372036854775810", e.result.toString())
        }
    }

    @Test
    fun mulNegative1() {
        val a = -2L
        val b = -(Long.MAX_VALUE / 2 + 1)
        assertFailsWith<ArithmeticException> {
            mulExact(a, b)
        }
    }

    @Test
    fun mulNegativeWrapped1() {
        val a = -2L
        val b = -(Long.MAX_VALUE / 2 + 1)
        try {
            mulExactWrapped(a, b)
            fail("call to mulExactWrapped should throw an exception")
        } catch (e: LongExpressionOverflow) {
            assertEquals("9223372036854775808", e.result.toString())
        }
    }

    @Test
    fun mulLimit0() {
        val a = Int.MAX_VALUE.toLong()
        val b = 1L
        assertEquals(Int.MAX_VALUE.toLong(), mulExact(a, b))
    }

    @Test
    fun mulLimitWrapped0() {
        val a = Int.MAX_VALUE.toLong()
        val b = 1L
        assertEquals(Int.MAX_VALUE.toLong(), mulExactWrapped(a, b))
    }

    @Test
    fun mulLimit1() {
        val a = Int.MAX_VALUE.toLong() + 1
        val b = 1L
        assertEquals(Int.MAX_VALUE.toLong() + 1, mulExact(a, b))
    }

    @Test
    fun mulLimit1Wrapped() {
        val a = Int.MAX_VALUE.toLong() + 1
        val b = 1L
        assertEquals(Int.MAX_VALUE.toLong() + 1, mulExactWrapped(a, b))
    }

    @Test
    fun mulLimit2() {
        val a = Long.MAX_VALUE
        val b = 1L
        assertEquals(Long.MAX_VALUE, mulExact(a, b))
    }

    @Test
    fun mulLimit2Wrapped() {
        val a = Long.MAX_VALUE
        val b = 1L
        assertEquals(Long.MAX_VALUE, mulExactWrapped(a, b))
    }

    @Test
    fun mulLimit3() {
        val a = Long.MIN_VALUE
        val b = 1L
        assertEquals(Long.MIN_VALUE, mulExact(a, b))
    }

    @Test
    fun mulLimit3Wrapped() {
        val a = Long.MIN_VALUE
        val b = 1L
        assertEquals(Long.MIN_VALUE, mulExactWrapped(a, b))
    }

    @Test
    fun largeMultiplicationNoOverflow() {
        val a = 0x7fffffffL
        val b = 0x7fffffffL
        assertEquals(0x3fffffff00000001L, mulExactWrapped(a, b))
    }

    @Test
    fun largeMultiplicationOverflow0() {
        val a = 0xffffffffL
        val b = 0xfffffffeL
        try {
            mulExactWrapped(a, b)
            fail("call to mulExactWrapped should throw an exception")
        } catch (e: LongExpressionOverflow) {
            assertEquals("fffffffd00000002", e.result.toString(16))
        }
    }

    @Test
    fun largeMultiplicationOverflow1() {
        val a = 0x100000000L
        val b = 0x100000000L
        try {
            mulExactWrapped(a, b)
            fail("call to mulExactWrapped should throw an exception")
        } catch (e: LongExpressionOverflow) {
            assertEquals("10000000000000000", e.result.toString(16))
        }
    }
}
