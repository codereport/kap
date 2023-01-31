package com.dhsdevelopments.mpbignum

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ParseTest {
    @Test
    fun makeBignumFromInt() {
        val a = BigInt.of(10)
        assertEquals("10", a.toString())
    }

    @Test
    fun invalidNumber() {
        assertFailsWith<NumberFormatException> {
            BigInt.of("a")
        }
        assertFailsWith<NumberFormatException> {
            BigInt.of("")
        }
        assertFailsWith<NumberFormatException> {
            BigInt.of("123456w")
        }
    }

    @Test
    fun makeBignumFromString() {
        val a = BigInt.of("100000000000000000000")
        assertEquals("100000000000000000000", a.toString())
    }
}
