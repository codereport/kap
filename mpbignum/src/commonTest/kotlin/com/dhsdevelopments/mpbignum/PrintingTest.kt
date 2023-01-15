package com.dhsdevelopments.mpbignum

import kotlin.test.Test
import kotlin.test.assertEquals

class PrintingTest {
    @Test
    fun makeBignumFromInt() {
        val a = 10.toBigint()
        assertEquals("10", a.toString())
    }

    @Test
    fun makeBignumFromString() {
        val a = bigIntFromString("100000000000000000000")
        assertEquals("100000000000000000000", a.toString())
    }

    @Test
    fun addSmallValues() {
        val a = 10.toBigint()
        val b = 100.toBigint()
        val c = a + b
        assertEquals("110", c.toString())
    }

    @Test
    fun addIntToOversized() {
        val a = 10.toBigint()
        val b = Int.MAX_VALUE.toBigint()
        val c = a + b
        assertEquals("2147483657", c.toString())
    }
}
