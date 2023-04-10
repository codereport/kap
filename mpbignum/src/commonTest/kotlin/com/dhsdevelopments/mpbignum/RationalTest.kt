package com.dhsdevelopments.mpbignum

import kotlin.test.Test
import kotlin.test.assertEquals

class RationalTest {
    @Test
    fun addSimple() {
        val a = Rational.make(BigInt.of(10), BigInt.of(11))
        val b = Rational.make(BigInt.of(20), BigInt.of(12))
        val c = a + b
        assertEquals(Rational.make(BigInt.of(85), BigInt.of(33)), c)
    }

    @Test
    fun addZero() {
        val a = Rational.make(BigInt.of(10), BigInt.of(11))
        val b = Rational.make(BigInt.of(0), BigInt.of(1))
        val c = a + b
        assertEquals(Rational.make(BigInt.of(10), BigInt.of(11)), c)
    }
}
