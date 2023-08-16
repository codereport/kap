package array

import kotlin.test.Test
import kotlin.test.assertFailsWith

class StructuralUnderTest : APLTest() {
    @Test
    fun scalarFunctionUnderTake0() {
        parseAPLExpression("((1000+)under(1↑)) 3 4 ⍴ ⍳12").let { result ->
            assertDimension(dimensionsOfSize(3, 4), result)
            assertArrayContent(arrayOf(1000, 1001, 1002, 1003, 4, 5, 6, 7, 8, 9, 10, 11), result)
        }
    }

    @Test
    fun scalarFunctionUnderTake1() {
        parseAPLExpression("(1000+)under(¯1↑) 3 4 ⍴ ⍳12").let { result ->
            assertDimension(dimensionsOfSize(3, 4), result)
            assertArrayContent(arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 1008, 1009, 1010, 1011), result)
        }
    }

    @Test
    fun scalarFunctionUnderTakeWithAxis() {
        parseAPLExpression("1 (1+)⍢↑[1] 3 3 ⍴ ⍳9").let { result ->
            assertDimension(dimensionsOfSize(3, 3), result)
            assertArrayContent(arrayOf(1, 1, 2, 4, 4, 5, 7, 7, 8), result)
        }
    }

    @Test
    fun scalarFunctionUnderMonadicTake0() {
        parseAPLExpression("(1000+)⍢↑ 1 2 3 4 5 6 7 8").let { result ->
            assert1DArray(arrayOf(1001, 2, 3, 4, 5, 6, 7, 8), result)
        }
    }

    @Test
    fun scalarFunctionUnderMonadicTake1() {
        parseAPLExpression("(1000+)⍢↑ 3 4 ⍴ 1 2 3 4 5 6 7 8 9 10 11 12").let { result ->
            assertDimension(dimensionsOfSize(3, 4), result)
            assertArrayContent(arrayOf(1001, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), result)
        }
    }

    @Test
    fun dropUnderTake0() {
        parseAPLExpression("((1↓)under(¯2↑)) 3 4 ⍴ ⍳12").let { result ->
            assertDimension(dimensionsOfSize(2, 4), result)
            assertArrayContent(arrayOf(0, 1, 2, 3, 8, 9, 10, 11), result)
        }
    }

    @Test
    fun dropUnderTake1() {
        parseAPLExpression("((1↓)⍢(1↑)) 3 4 ⍴ ⍳12").let { result ->
            assertDimension(dimensionsOfSize(2, 4), result)
            assertArrayContent(arrayOf(4, 5, 6, 7, 8, 9, 10, 11), result)
        }
    }

    @Test
    fun addUnderTakeWithAxis() {
        parseAPLExpression("1 (10+)⍢↓[1] 3 3 ⍴ ⍳9").let { result ->
            assertDimension(dimensionsOfSize(3, 3), result)
            assertArrayContent(arrayOf(0, 11, 12, 3, 14, 15, 6, 17, 18), result)
        }
    }

    @Test
    fun addUnderTakeWithInvalidAxis() {
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("1 (10+)⍢↓[2] 5 5 ⍴ ⍳9")
        }
    }

    @Test
    fun addUnderDyadicDrop0() {
        parseAPLExpression("(100+)⍢(1↓) 10 11 12 13 14").let { result ->
            assert1DArray(arrayOf(10, 111, 112, 113, 114), result)
        }
    }

    @Test
    fun addUnderDyadicDrop1() {
        parseAPLExpression("(100+)⍢(1↓) 3 4 ⍴ ⍳12").let { result ->
            assertDimension(dimensionsOfSize(3, 4), result)
            assertArrayContent(arrayOf(0, 1, 2, 3, 104, 105, 106, 107, 108, 109, 110, 111), result)
        }
    }

    @Test
    fun addUnderDyadicDrop2() {
        parseAPLExpression("(100+)⍢(¯1↓) 2 4 ⍴ ⍳8").let { result ->
            assertDimension(dimensionsOfSize(2, 4), result)
            assertArrayContent(arrayOf(100, 101, 102, 103, 4, 5, 6, 7), result)
        }
    }

    @Test
    fun addUnderMonadicDrop() {
        parseAPLExpression("(100+)⍢↓ 10 11 12 13 14").let { result ->
            assert1DArray(arrayOf(10, 111, 112, 113, 114), result)
        }
    }

    @Test
    fun dimensionsChangedAfterUnderMonadicDrop() {
        assertFailsWith<APLIllegalArgumentException> {
            parseAPLExpression("{2 2 ⍴ ⍳4}⍢↓ 10 11 12 13 14")
        }
    }

    @Test
    fun takeUnderDrop() {
        parseAPLExpression("((50 60 70 80⍪)under(1↓)) 4 4 ⍴ ⍳16").let { result ->
            assertDimension(dimensionsOfSize(5, 4), result)
            assertArrayContent(arrayOf(0, 1, 2, 3, 50, 60, 70, 80, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15), result)
        }
    }

    /**
     * Attempt to drop a column under a take of rows.
     * This should fail, since the resulting array has incorrect dimensions.
     */
    @Test
    fun dropUnderTakeIllegalDropAxis() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("((0 1↓)⍢(2↑)) 5 4 ⍴ ⍳20")
        }
    }

    @Test
    fun mulUnderInverseSub() {
        parseAPLExpression("(10×)⍢(1-⍨) ⍳6").let { result ->
            assert1DArray(arrayOf(-9, 1, 11, 21, 31, 41), result)
        }
    }
}
