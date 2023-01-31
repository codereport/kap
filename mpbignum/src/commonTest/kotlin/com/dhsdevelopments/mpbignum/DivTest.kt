package com.dhsdevelopments.mpbignum

import kotlin.test.Test
import kotlin.test.assertEquals

class DivTest {
    @Test
    fun divideBigInt() {
        val a = BigInt.of("100000000000000000000")
        val b = BigInt.of(20)
        val c = a / b
        assertEquals("5000000000000000000", c.toString())
    }

    @Test
    fun divideBigIntWithTruncate() {
        val a = BigInt.of("100000000000000000000000000")
        val b = BigInt.of(3)
        val c = a / b
        assertEquals("33333333333333333333333333", c.toString())
    }
}
