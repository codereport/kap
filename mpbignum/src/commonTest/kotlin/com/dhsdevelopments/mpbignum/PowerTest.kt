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

    @Test
    fun bigIntLargePower() {
        val a = BigInt.of(2)
        val b = 1000
        val c = a.pow(b)
        val expected = """
            10715086071862673209484250490600018105614048117055336074437503883703510511249361224
            93198378815695858127594672917553146825187145285692314043598457757469857480393456777
            48242309854210746050623711418779541821530464749835819412673987675591655439460770629
            14571196477686542167660429831652624386837205668069376
        """.trimIndent().filter { it != '\n' }
        assertEquals(expected, c.toString())
    }
}
