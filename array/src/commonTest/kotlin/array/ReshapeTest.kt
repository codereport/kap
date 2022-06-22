package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ReshapeTest : APLTest() {
    @Test
    fun simpleReshape() {
        val result = parseAPLExpression("3 4 ⍴ ⍳12")
        assertDimension(dimensionsOfSize(3, 4), result)
        assertArrayContent(arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), result)
    }

    @Test
    fun reshapeDecreaseSize() {
        val result = parseAPLExpression("3 ⍴ 1 2 3 4 5 6 7 8")
        assertDimension(dimensionsOfSize(3), result)
        assertArrayContent(arrayOf(1, 2, 3), result)
    }

    @Test
    fun reshapeIncreaseSize() {
        val result = parseAPLExpression("14 ⍴ 1 2 3 4")
        assertDimension(dimensionsOfSize(14), result)
        assertArrayContent(arrayOf(1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2), result)
    }

    @Test
    fun reshapeScalarToSingleDimension() {
        val result = parseAPLExpression("4 ⍴ 1")
        assertDimension(dimensionsOfSize(4), result)
        assertArrayContent(arrayOf(1, 1, 1, 1), result)
    }

    @Test
    fun reshapeScalarToMultiDimension() {
        val result = parseAPLExpression("2 4 ⍴ 1")
        assertDimension(dimensionsOfSize(2, 4), result)
        assertArrayContent(arrayOf(1, 1, 1, 1, 1, 1, 1, 1), result)
    }

    @Test
    fun reshapeCalculatedDimension0() {
        parseAPLExpression("¯1 2 ⍴ ⍳4").let { result ->
            assertDimension(dimensionsOfSize(2, 2), result)
            assertArrayContent(arrayOf(0, 1, 2, 3), result)
        }
    }

    @Test
    fun reshapeCalculatedDimension1() {
        parseAPLExpression("2 ¯1 ⍴ ⍳4").let { result ->
            assertDimension(dimensionsOfSize(2, 2), result)
            assertArrayContent(arrayOf(0, 1, 2, 3), result)
        }
    }

    @Test
    fun reshapeCalculatedFailsWithMismatchedSource() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("2 ¯1 ⍴ ⍳5")
        }
    }

    @Test
    fun reshapeCalculatedFailsWithMultipleUndefinedDimensions() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("¯1 ¯1 ⍴ ⍳4")
        }
    }

    @Test
    fun reshapeSpecialisedLong() {
        parseAPLExpression("2 3 ⍴ 10 11 12 13").let { result ->
            assertDimension(dimensionsOfSize(2, 3), result)
            assertSame(ArrayMemberType.LONG, result.specialisedType)
            assertEquals(10L, result.valueAtLong(0, null))
            assertEquals(11L, result.valueAtLong(1, null))
            assertEquals(12L, result.valueAtLong(2, null))
            assertEquals(13L, result.valueAtLong(3, null))
            assertEquals(10L, result.valueAtLong(4, null))
            assertEquals(11L, result.valueAtLong(5, null))
        }
    }

    @Test
    fun reshapeSpecialisedDouble() {
        parseAPLExpression("2 3 ⍴ 1.1 1.2 1.3 1.4").let { result ->
            assertDimension(dimensionsOfSize(2, 3), result)
            assertSame(ArrayMemberType.DOUBLE, result.specialisedType)
            assertEquals(1.1, result.valueAtDouble(0, null))
            assertEquals(1.2, result.valueAtDouble(1, null))
            assertEquals(1.3, result.valueAtDouble(2, null))
            assertEquals(1.4, result.valueAtDouble(3, null))
            assertEquals(1.1, result.valueAtDouble(4, null))
            assertEquals(1.2, result.valueAtDouble(5, null))
        }
    }

    @Test
    fun reshapeSpecialisedLongSingleValue() {
        parseAPLExpression("2 3 ⍴ 1").let { result ->
            assertDimension(dimensionsOfSize(2, 3), result)
            repeat(6) { i ->
                assertEquals(1, result.valueAtLong(i, null))
            }
        }
    }

    @Test
    fun reshapeSpecialisedDoubleSingleValue() {
        parseAPLExpression("2 3 ⍴ 1.2").let { result ->
            assertDimension(dimensionsOfSize(2, 3), result)
            repeat(6) { i ->
                assertEquals(1.2, result.valueAtDouble(i, null))
            }
        }
    }

    @Test
    fun reshapeFromSingleElementArray() {
        parseAPLExpression("{ (⍴⍵) ⍴ 1 } 1 2 3 4").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1, 1, 1, 1), result)
        }
    }

    @Test
    fun reshapeScalar0() {
        parseAPLExpression("⍬ ⍴ 1").let { result ->
            assertSimpleNumber(1, result)
        }
    }

    @Test
    fun reshapeScalar1() {
        parseAPLExpression("⍬ ⍴ 1 2 3 4 5 6").let { result ->
            assertSimpleNumber(1, result)
        }
    }

    @Test
    fun reshapeToNull() {
        parseAPLExpression("⍬ ⍴ ⊂1 2 3").let { result ->
            assertEquals(0, result.dimensions.size)
            val v = result.valueAt(0)
            assert1DArray(arrayOf(1, 2, 3), v)
        }
    }

    @Test
    fun reshapeReshapedArrayDefaultSimple() {
        parseAPLExpression("6 ⍴ 3 ⍴ 10 11 12 13 14 15 16 17 18").let { result ->
            assert1DArray(arrayOf(10, 11, 12, 10, 11, 12), result)
        }
    }

    @Test
    fun reshapeReshapedArrayGenericSimple() {
        parseAPLExpression("6 ⍴ 3 ⍴ int:ensureGeneric 10 11 12 13 14 15 16 17 18").let { result ->
            assert1DArray(arrayOf(10, 11, 12, 10, 11, 12), result)
        }
    }

    @Test
    fun reshapeReshapedArrayLongSimple() {
        parseAPLExpression("6 ⍴ 3 ⍴ int:ensureLong 10 11 12 13 14 15 16 17 18").let { result ->
            assert1DArray(arrayOf(10, 11, 12, 10, 11, 12), result)
        }
    }

    @Test
    fun reshapeReshapedArrayDoubleSimple() {
        parseAPLExpression("6 ⍴ 3 ⍴ int:ensureDouble 10.0 11.0 12.0 13.0 14.0 15.0 16.0 17.0 18.0").let { result ->
            assert1DArray(
                arrayOf(
                    InnerDouble(10.0),
                    InnerDouble(11.0),
                    InnerDouble(12.0),
                    InnerDouble(10.0),
                    InnerDouble(11.0),
                    InnerDouble(12.0)),
                result)
        }
    }

    @Test
    fun reshapeReshapedArrayMulti() {
        parseAPLExpression("7 ⍴ 6 ⍴ 5 ⍴ int:ensureGeneric ⍳100").let { result ->
            assert1DArray(arrayOf(0, 1, 2, 3, 4, 0, 0), result)
        }
    }

    @Test
    fun reshapeReshapedArrayLongMulti() {
        parseAPLExpression("7 ⍴ 6 ⍴ 5 ⍴ int:ensureLong ⍳100").let { result ->
            assert1DArray(arrayOf(0, 1, 2, 3, 4, 0, 0), result)
        }
    }

    @Test
    fun reshapeReshapedArrayDoubleMulti() {
        parseAPLExpression("7 ⍴ 6 ⍴ 5 ⍴ int:ensureDouble ⍳100").let { result ->
            assert1DArray(
                arrayOf(
                    InnerDouble(0.0),
                    InnerDouble(1.0),
                    InnerDouble(2.0),
                    InnerDouble(3.0),
                    InnerDouble(4.0),
                    InnerDouble(0.0),
                    InnerDouble(0.0)),
                result)
        }
    }

    @Test
    fun reshapeReshapedArrayDefaultSameSize() {
        parseAPLExpression("3 ⍴ 3 ⍴ 10 11 12 13 14 15 16 17 18").let { result ->
            assert1DArray(arrayOf(10, 11, 12), result)
        }
    }

    @Test
    fun reshapeReshapedArrayGenericSameSize() {
        parseAPLExpression("3 ⍴ 3 ⍴ int:ensureGeneric 10 11 12 13 14 15 16 17 18").let { result ->
            assert1DArray(arrayOf(10, 11, 12), result)
        }
    }

    @Test
    fun reshapeReshapedArrayLongSameSize() {
        parseAPLExpression("3 ⍴ 3 ⍴ int:ensureLong 10 11 12 13 14 15 16 17 18").let { result ->
            assert1DArray(arrayOf(10, 11, 12), result)
        }
    }

    @Test
    fun reshapeReshapedArrayDoubleSameSize() {
        parseAPLExpression("3 ⍴ 3 ⍴ int:ensureDouble 10.0 11.0 12.0 13.0 14.0 15.0 16.0 17.0 18.0").let { result ->
            assert1DArray(arrayOf(InnerDouble(10.0), InnerDouble(11.0), InnerDouble(12.0)), result)
        }
    }

    @Test
    fun reshapeReshapedArrayDefaultSmallerSize() {
        parseAPLExpression("2 ⍴ 6 ⍴ 10 11 12 13 14 15 16 17 18").let { result ->
            assert1DArray(arrayOf(10, 11), result)
        }
    }

    @Test
    fun reshapeReshapedArrayGenericSmallerSize() {
        parseAPLExpression("2 ⍴ 6 ⍴ int:ensureGeneric 10 11 12 13 14 15 16 17 18").let { result ->
            assert1DArray(arrayOf(10, 11), result)
        }
    }

    @Test
    fun reshapeReshapedArrayLongSmallerSize() {
        parseAPLExpression("2 ⍴ 6 ⍴ int:ensureLong 10 11 12 13 14 15 16 17 18").let { result ->
            assert1DArray(arrayOf(10, 11), result)
        }
    }

    @Test
    fun reshapeReshapedArrayDoubleSmallerSize() {
        parseAPLExpression("2 ⍴ 6 ⍴ int:ensureDouble 10.0 11.0 12.0 13.0 14.0 15.0 16.0 17.0 18.0").let { result ->
            assert1DArray(arrayOf(InnerDouble(10.0), InnerDouble(11.0)), result)
        }
    }

    @Test
    fun reshapeReshapedArrayDefaultSmallerSizeTwoLevels() {
        parseAPLExpression("2 ⍴ 3 ⍴ 6 ⍴ 10 11 12 13 14 15 16 17 18").let { result ->
            assert1DArray(arrayOf(10, 11), result)
        }
    }

    @Test
    fun reshapeReshapedArrayGenericSmallerSizeTwoLevels() {
        parseAPLExpression("2 ⍴ 3 ⍴ 6 ⍴ int:ensureGeneric 10 11 12 13 14 15 16 17 18").let { result ->
            assert1DArray(arrayOf(10, 11), result)
        }
    }

    @Test
    fun reshapeReshapedArrayLongSmallerSizeTwoLevels() {
        parseAPLExpression("2 ⍴ 3 ⍴ 6 ⍴ int:ensureLong 10 11 12 13 14 15 16 17 18").let { result ->
            assert1DArray(arrayOf(10, 11), result)
        }
    }

    @Test
    fun reshapeReshapedArrayDoubleSmallerSizeTwoLevels() {
        parseAPLExpression("2 ⍴ 3 ⍴ 6 ⍴ int:ensureDouble 10.0 11.0 12.0 13.0 14.0 15.0 16.0 17.0 18.0").let { result ->
            assert1DArray(arrayOf(InnerDouble(10.0), InnerDouble(11.0)), result)
        }
    }

    @Test
    fun reshapeReshapedMultiDimensionalArrayDefault() {
        parseAPLExpression("5 ⍴ 2 2 ⍴ ⍳100").let { result ->
            assert1DArray(arrayOf(0, 1, 2, 3, 0), result)
        }
    }

    @Test
    fun reshapeReshapedMultiDimensionalArrayGeneric() {
        parseAPLExpression("5 ⍴ 2 2 ⍴ int:ensureGeneric ⍳100").let { result ->
            assert1DArray(arrayOf(0, 1, 2, 3, 0), result)
        }
    }

    @Test
    fun reshapeReshapedMultiDimensionalArrayLong() {
        parseAPLExpression("5 ⍴ 2 2 ⍴ int:ensureLong ⍳100").let { result ->
            assert1DArray(arrayOf(0, 1, 2, 3, 0), result)
        }
    }

    @Test
    fun reshapeReshapedMultiDimensionalArrayDouble() {
        parseAPLExpression("5 ⍴ 2 2 ⍴ int:ensureDouble ⍳100").let { result ->
            assert1DArray(arrayOf(InnerDouble(0.0), InnerDouble(1.0), InnerDouble(2.0), InnerDouble(3.0), InnerDouble(0.0)), result)
        }
    }

    @Test
    fun reshapeReshapedArrayDefaultSameSizeWithIota() {
        parseAPLExpression("3 ⍴ 3 ⍴ ⍳100").let { result ->
            assert1DArray(arrayOf(0, 1, 2), result)
        }
    }

    @Test
    fun reshapeReshapedArrayDefaultSimpleWithIotaDescending() {
        parseAPLExpression("6 ⍴ 2 ⍴ ⍳100").let { result ->
            assert1DArray(arrayOf(0, 1, 0, 1, 0, 1), result)
        }
    }

    @Test
    fun reshapeReshapedArrayDefaultSimpleWithIotaAscending() {
        parseAPLExpression("2 ⍴ 6 ⍴ ⍳100").let { result ->
            assert1DArray(arrayOf(0, 1), result)
        }
    }
}
