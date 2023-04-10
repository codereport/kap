package com.dhsdevelopments.mpbignum

import kotlin.test.Test
import kotlin.test.assertEquals

class GcdTest {
    @Test
    fun simpleGcd() {
        val a = BigInt.of(300)
        val b = BigInt.of(9)
        val c = a.gcd(b)
        assertEquals(BigInt.of(3), c)
    }
}
