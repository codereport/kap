package com.dhsdevelopments.mpbignum

import kotlin.test.Test
import kotlin.test.assertEquals

class ModTest {
    @Test
    fun modPositive0() {
        val a = BigInt.of("7")
        val b = BigInt.of("2")
        val c = a % b
        assertEquals(BigInt.of("1"), c)
    }

    @Test
    fun modPositive1() {
        val a = BigInt.of("123456")
        val b = BigInt.of("380")
        val c = a % b
        assertEquals(BigInt.of("336"), c)
    }

    @Test
    fun rightSideNegative0() {
        val a = BigInt.of("7")
        val b = BigInt.of("-2")
        val c = a % b
        assertEquals(BigInt.of("1"), c)
    }

    @Test
    fun rightSideNegative1() {
        val a = BigInt.of("123456")
        val b = BigInt.of("-380")
        val c = a % b
        assertEquals(BigInt.of("336"), c)
    }

    @Test
    fun leftSideNegative0() {
        val a = BigInt.of("-7")
        val b = BigInt.of("2")
        val c = a % b
        assertEquals(BigInt.of("-1"), c)
    }

    @Test
    fun leftSideNegative1() {
        val a = BigInt.of("-123456")
        val b = BigInt.of("380")
        val c = a % b
        assertEquals(BigInt.of("-336"), c)
    }

    @Test
    fun bothSidesNegative0() {
        val a = BigInt.of("-7")
        val b = BigInt.of("-2")
        val c = a % b
        assertEquals(BigInt.of("-1"), c)
    }

    @Test
    fun bothSidesNegative1() {
        val a = BigInt.of("-123456")
        val b = BigInt.of("-380")
        val c = a % b
        assertEquals(BigInt.of("-336"), c)
    }

    @Test
    fun leftSideZero() {
        val a = BigIntConstants.ZERO
        val b = a % 2
        assertEquals(BigInt.of(0), b)
    }

    @Test
    fun leftSideZeroRightNegative() {
        val a = BigIntConstants.ZERO
        val b = a % -2
        assertEquals(BigInt.of(0), b)
    }
}
