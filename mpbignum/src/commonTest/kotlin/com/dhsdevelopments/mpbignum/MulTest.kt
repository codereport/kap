package com.dhsdevelopments.mpbignum

import kotlin.test.Test
import kotlin.test.assertEquals

class MulTest {
    @Test
    fun multiplyBigInt() {
        val a = BigInt.of("100000000000000000000")
        val b = BigInt.of(12)
        val c = a * b
        assertEquals("1200000000000000000000", c.toString())
    }
}
