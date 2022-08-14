package array

import kotlin.test.*

class DimensionTest {
    @Test
    fun emptyDimensionsTest() {
        val d = emptyDimensions()
        assertEquals(0, d.size)
        assertEquals(1, d.contentSize())

        val m = d.multipliers()
        assertEquals(0, m.size)
    }

    @Test
    fun singleDimensionTest() {
        val d = dimensionsOfSize(10)
        assertEquals(1, d.size)
        assertEquals(10, d.contentSize())
        assertEquals(10, d[0])
        // This test is commented out because accessing an array outside of the valid bounds
        // will not throw an error when using JS
//        assertFails {
//            d[1]
//        }

        val m = d.multipliers()
        assertEquals(1, m.size)
        assertEquals(1, m[0])
    }

    @Test
    fun twoDimensions() {
        val d = dimensionsOfSize(2, 3)
        assertEquals(2, d.size)
        assertEquals(6, d.contentSize())
        assertEquals(2, d[0])
        assertEquals(3, d[1])

        val m = d.multipliers()
        assertEquals(2, m.size)
        assertEquals(3, m[0])
        assertEquals(1, m[1])
    }

    @Test
    fun equalityTest() {
        val d0 = dimensionsOfSize(2, 3, 4)
        val d1 = dimensionsOfSize(2, 3, 4)
        assertTrue(d0.compareEquals(d1))

        val d2 = dimensionsOfSize(1, 3, 4)
        assertFalse(d0.compareEquals(d2))
    }

    @Test
    fun insertTest() {
        val d0 = dimensionsOfSize(2, 3, 4)
        val d1 = d0.insert(0, 2)
        val d2 = dimensionsOfSize(2, 2, 3, 4)
        assertTrue(d1.compareEquals(d2))
    }

    @Test
    fun replaceTest() {
        val d0 = dimensionsOfSize(2, 3, 4)
        val d1 = d0.replace(2, 6)
        val d2 = dimensionsOfSize(2, 3, 6)
        assertTrue(d1.compareEquals(d2))
    }

    @Test
    fun oversizedArrayTest0() {
        assertFailsWith<ArraySizeException> {
            dimensionsOfSize(2, 0x7FFFFFFF)
        }
    }

    @Test
    fun oversizedArrayTest1() {
        assertFailsWith<ArraySizeException> {
            dimensionsOfSize(
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
                8, 8, 8, 8, 8, 8, 8, 8, 8, 8)
        }
    }

    @Test
    fun oversizedArrayTest2() {
        val d = dimensionsOfSize(0x7FFFFFFF)
        assertFailsWith<ArraySizeException> {
            d.insert(0, 2)
        }
    }

    @Test
    fun oversizedArrayTest3() {
        val d = dimensionsOfSize(4, 0x10000000)
        assertFailsWith<ArraySizeException> {
            d.replace(0, 8)
        }
    }

    @Test
    fun negativeDimensionsTest() {
        assertFailsWith<InvalidDimensionsException> {
            dimensionsOfSize(2, 3, -1, 2)
        }
    }
}
