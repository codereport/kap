package array

import kotlin.test.Test
import kotlin.test.assertFailsWith

class NumberTypesTest : APLTest() {
    @Test
    fun rationalParts() {
        parseAPLExpression("math:numerator (1÷3) (10÷3) 2 ¯2 0 (¯1÷3) (int:asBigint 2) (int:asBigint 0)").let { result ->
            assert1DArray(arrayOf(1, 10, 2, -2, 0, -1, 2, 0), result)
        }
        parseAPLExpression("math:denominator (1÷3) (10÷3) 2 ¯2 0 (¯1÷3) (int:asBigint 2) (int:asBigint 0)").let { result ->
            assert1DArray(arrayOf(3, 3, 1, 1, 1, 3, 1, 1), result)
        }
    }

    @Test
    fun rationalPartsFromDoubleShouldFail() {
        assertFailsWith<IncompatibleTypeException> {
            parseAPLExpression("math:numerator 1.2")
        }
        assertFailsWith<IncompatibleTypeException> {
            parseAPLExpression("math:denominator 1.2")
        }
    }

    @Test
    fun rationalPartsFromComplexShouldFail() {
        assertFailsWith<IncompatibleTypeException> {
            parseAPLExpression("math:numerator 1.2j2.1")
        }
        assertFailsWith<IncompatibleTypeException> {
            parseAPLExpression("math:denominator 1.2j2.1")
        }
    }
}