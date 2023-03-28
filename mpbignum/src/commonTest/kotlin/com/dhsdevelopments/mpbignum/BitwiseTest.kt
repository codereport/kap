package com.dhsdevelopments.mpbignum

import kotlin.test.Test
import kotlin.test.assertEquals

class BitwiseTest {

    //////////////////
    // and
    //////////////////

    @Test
    fun andBigIntWithBigInt() {
        val a = BigInt.of(0b0011)
        val b = BigInt.of(0b1010)
        val c = a and b
        assertEquals("2", c.toString())
    }

    @Test
    fun andLongWithBigInt() {
        val a = 0b0011.toLong()
        val b = BigInt.of(0b1010)
        val c = a and b
        assertEquals("2", c.toString())
    }

    @Test
    fun andBigIntWithLong() {
        val a = BigInt.of(0b0011)
        val b = 0b1010.toLong()
        val c = a and b
        assertEquals("2", c.toString())
    }

    @Test
    fun andIntWithBigInt() {
        val a = 0b0011
        val b = BigInt.of(0b1010)
        val c = a and b
        assertEquals("2", c.toString())
    }

    @Test
    fun andBigIntWithInt() {
        val a = BigInt.of(0b0011)
        val b = 0b1010
        val c = a and b
        assertEquals("2", c.toString())
    }

    //////////////////
    // or
    //////////////////

    @Test
    fun orBigIntWithBigInt() {
        val a = BigInt.of(0b0011)
        val b = BigInt.of(0b1010)
        val c = a or b
        assertEquals("11", c.toString())
    }

    @Test
    fun orLongWithBigInt() {
        val a = 0b0011.toLong()
        val b = BigInt.of(0b1010)
        val c = a or b
        assertEquals("11", c.toString())
    }

    @Test
    fun orBigIntWithLong() {
        val a = BigInt.of(0b0011)
        val b = 0b1010.toLong()
        val c = a or b
        assertEquals("11", c.toString())
    }

    @Test
    fun orIntWithBigInt() {
        val a = 0b0011
        val b = BigInt.of(0b1010)
        val c = a or b
        assertEquals("11", c.toString())
    }

    @Test
    fun orBigIntWithInt() {
        val a = BigInt.of(0b0011)
        val b = 0b1010
        val c = a or b
        assertEquals("11", c.toString())
    }

    //////////////////
    // xor
    //////////////////

    @Test
    fun xorBigIntWithBigInt() {
        val a = BigInt.of(0b0011)
        val b = BigInt.of(0b1010)
        val c = a xor b
        assertEquals("9", c.toString())
    }

    @Test
    fun xorLongWithBigInt() {
        val a = 0b0011.toLong()
        val b = BigInt.of(0b1010)
        val c = a xor b
        assertEquals("9", c.toString())
    }

    @Test
    fun xorBigIntWithLong() {
        val a = BigInt.of(0b0011)
        val b = 0b1010.toLong()
        val c = a xor b
        assertEquals("9", c.toString())
    }

    @Test
    fun xorIntWithBigInt() {
        val a = 0b0011
        val b = BigInt.of(0b1010)
        val c = a xor b
        assertEquals("9", c.toString())
    }

    @Test
    fun xorBigIntWithInt() {
        val a = BigInt.of(0b0011)
        val b = 0b1010
        val c = a xor b
        assertEquals("9", c.toString())
    }
}
