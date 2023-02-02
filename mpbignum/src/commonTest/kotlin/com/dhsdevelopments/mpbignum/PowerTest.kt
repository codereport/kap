package com.dhsdevelopments.mpbignum

import kotlin.test.Test
import kotlin.test.assertEquals

class PowerTest {
    @Test
    fun bigIntSmallPower() {
        val a = BigInt.of(10)
        val b = 5L
        val c = a.pow(b)
        assertEquals("100000", c.toString())
    }
}