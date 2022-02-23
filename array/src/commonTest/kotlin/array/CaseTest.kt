package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CaseTest : APLTest() {
    @Test
    fun characterCase() {
        parseAPLExpression("0 1 0 % \"abc\" \"FOO\"").let { result ->
            assertString("aOc", result)
        }
    }

    @Test
    fun stringCase() {
        parseAPLExpression("0 1 0 % (\"a1\" \"b1\" \"c1\") (\"a2\" \"b2\" \"c2\")").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertString("a1", result.valueAt(0))
            assertString("b2", result.valueAt(1))
            assertString("c1", result.valueAt(2))
        }
    }

    @Test
    fun selectionArrayWrongDimensions() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("0 1 % (1 2 3) (4 5 6)")
        }
    }

    @Test
    fun selectionIndexOutOfRange() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("0 1 2 % (1 2 3) (4 5 6)")
        }
    }

    @Test
    fun contentArrayInvalidDimension0() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("0 1 2 % (1 2 3 4) (4 5 6)")
        }
    }

    @Test
    fun contentArrayInvalidDimension1() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("0 1 2 % (1 2 3) (3 4 5 6)")
        }
    }

    @Test
    fun contentArrayInvalidDimension2() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(2 2 ⍴ 0 0 1 0) % (1 2 3 4) (4 5 6 7)")
        }
    }

    @Test
    fun contentArrayInvalidDimension3() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(2 3 ⍴ 0 0 1 0 1 1 1 0) % (1 2 3 4 5 6) (10 11 12 13 14 15 16)")
        }
    }

    @Test
    fun contentArrayInvalidDimension4() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(2 2 ⍴ 0 0 1 0) % (2 2 ⍴ 1 2 3 4) (4 5 6 7)")
        }
    }

    @Test
    fun contentArrayInvalidDimension5() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(2 2 ⍴ 0 0 1 0) % (1 2 3 4) (2 2 ⍴ 4 5 6 7)")
        }
    }

    @Test
    fun twoDimensionalCase() {
        parseAPLExpression("(2 2 ⍴ 0 0 1 0) % (2 2 ⍴ ⍳4) (2 2 ⍴ 100+⍳4)").let { result ->
            assertDimension(dimensionsOfSize(2, 2), result)
            assertArrayContent(arrayOf(0, 1, 102, 3), result)
        }
    }

    @Test
    fun caseWithLazy() {
        parseAPLExpressionWithOutput("0 0 1 1 1 1 % (io:print¨0 1 2 3 4 5) (io:print¨100 101 102 103 104 105)").let { (result, out) ->
            assertDimension(dimensionsOfSize(6), result)
            assertArrayContent(arrayOf(0, 1, 102, 103, 104, 105), result)
            assertEquals("01102103104105", out)
        }
    }

    @Test
    fun multiSelection() {
        parseAPLExpression("0 10 2 2 % (100×⍳11) + 11 ⍴ (⊂0 1 2 3)").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(0, 1001, 202, 203), result)
        }
    }

    @Test
    fun caseWithScalarArg0() {
        parseAPLExpression("0 1 1 % 9 (5 6 7)").let { result ->
            assert1DArray(arrayOf(9, 6, 7), result)
        }
    }

    @Test
    fun caseWithScalarArg1() {
        parseAPLExpression("0 1 1 % (5 6 7) 9").let { result ->
            assert1DArray(arrayOf(5, 9, 9), result)
        }
    }

    @Test
    fun caseWithScalarArg2() {
        parseAPLExpression("(2 2 ⍴ 0 0 1 1) % 9 (2 2 ⍴ 3 4 5 6)").let { result ->
            assertDimension(dimensionsOfSize(2, 2), result)
            assertArrayContent(arrayOf(9, 9, 5, 6), result)
        }
    }

    @Test
    fun caseWithSingleArgument() {
        parseAPLExpression("0 0 % (,⊂ 1 2)").let { result ->
            assert1DArray(arrayOf(1, 2), result)
        }
    }
}
