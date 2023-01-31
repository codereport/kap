package com.dhsdevelopments.mpbignum

import kotlin.test.Test
import kotlin.test.assertEquals

class SubTest {
    @Test
    fun subtractBigIntWithOverflow() {
        val a = BigInt.of(10)
        val b = BigInt.of(Int.MIN_VALUE)
        val c = b - a
        assertEquals("-2147483658", c.toString())
    }
}
