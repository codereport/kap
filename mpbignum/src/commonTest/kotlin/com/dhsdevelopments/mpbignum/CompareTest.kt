package com.dhsdevelopments.mpbignum

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CompareTest {
    @Test
    fun lessThanThanBigIntToBigInt() {
        val a = BigInt.of(2)
        val b = BigInt.of(3)
        assertTrue(a < b)
        assertFalse(b < a)
        val c = BigInt.of(2)
        assertFalse(a < c)
    }

    @Test
    fun compIntToBigInt() {
        val a = BigInt.of(2)
        val b = 3
        assertTrue(a < b)
        assertFalse(a > b)
    }

    @Test
    fun compLongToBigInt() {
        val a = BigInt.of(2)
        val b = 3L
        assertTrue(a < b)
        assertFalse(a > b)
    }

    @Test
    fun compBigIntToInt() {
        val a = 2
        val b = BigInt.of(10)
        assertTrue(a < b)
        assertFalse(a > b)
    }

    @Test
    fun compBigIntToLong() {
        val a = 2L
        val b = BigInt.of(10)
        assertTrue(a < b)
        assertFalse(a > b)
    }
}
