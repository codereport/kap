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
    fun dropUnderTake0() {
        parseAPLExpression("((1↓)under(¯2↑)) 3 4 ⍴ ⍳12").let { result ->
            assertDimension(dimensionsOfSize(2, 4), result)
            assertArrayContent(arrayOf(0, 1, 2, 3, 8, 9, 10, 11), result)
        }
    }

    @Test
    fun dropUnderTake1() {
        parseAPLExpression("((1↓)⌾(1↑)) 3 4 ⍴ ⍳12").let { result ->
            assertDimension(dimensionsOfSize(2, 4), result)
            assertArrayContent(arrayOf(4, 5, 6, 7, 8, 9, 10, 11), result)
        }
    }

    /**
     * Attempt to drop a column under a take of rows.
     * This should fail, since the resulting array has incorrect dimensions.
     */
    @Test
    fun dropUnderTakeIllegalDropAxis() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("((0 1↓)⌾(2↑)) 5 4 ⍴ ⍳20")
        }
    }

    @Test
    fun mulUnderInverseSub() {
        parseAPLExpression("(10×)⌾(1-⍨) ⍳6").let { result ->
            assert1DArray(arrayOf(-9, 1, 11, 21, 31, 41), result)
        }
    }
}
