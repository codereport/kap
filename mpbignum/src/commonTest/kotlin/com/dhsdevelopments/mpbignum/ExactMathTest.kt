package com.dhsdevelopments.mpbignum

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExactMathTest {
    @Test
    fun addNoException() {
        val a = 2L
        val b = 4L
        assertEquals(6L, addExact(a, b))
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
    fun addExceptionNegative() {
        val a = Long.MIN_VALUE
        val b = -1L
        assertFailsWith<ArithmeticException> {
            addExact(a, b)
        }
    }

    @Test
    fun subNoException() {
        val a = 8L
        val b = 7L
        assertEquals(1L, subExact(a, b))
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
    fun subExceptionPositive() {
        val a = Long.MAX_VALUE
        val b = -1L
        assertFailsWith<ArithmeticException> {
            subExact(a, b)
        }
    }

    @Test
    fun mulNoException() {
        val a = 2L
        val b = 8L
        assertEquals(16L, mulExact(a, b))
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
    fun mulNegative0() {
        val a = -2L
        val b = Long.MIN_VALUE / 2 - 1
        assertFailsWith<ArithmeticException> {
            mulExact(a, b)
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
}
