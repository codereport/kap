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
        BigInt.of("100000000000000000000").let { a ->
            assertEquals("100000000000000000000", a.toString())
        }
        BigInt.of("1000000000000020").let { a ->
            assertEquals("1000000000000020", a.toString())
        }
    }

    @Test
    fun differentLengths() {
        repeat(200) { i ->
            val s = "1" + "0".repeat(i)
            val a = BigInt.of(s)
            assertEquals(s, a.toString(), "Failed at: ${i}")
        }
    }
}
