package com.dhsdevelopments.mpbignum

import kotlin.test.Test
import kotlin.test.assertEquals

class AbsTest {
    @Test
    fun absPositive() {
        val a = BigInt.of(2)
        val b = a.absoluteValue
        assertEquals(BigInt.of(2), b)
    }

    @Test
    fun absNegative() {
        val a = BigInt.of(-2)
        val b = a.absoluteValue
        assertEquals(BigInt.of(2), b)
    }
}
