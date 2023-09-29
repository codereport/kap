package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PickTest : APLTest() {
    @Test
    fun simplePick() {
        parseAPLExpression("2 ⊇ 100+⍳10", true).let { result ->
            assertSimpleNumber(102, result)
        }
    }

    @Test
    fun simplePickArrayResult() {
        parseAPLExpression("2 3 ⊇ 100+⍳10", true).let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(102, 103), result)
        }
    }

    @Test
    fun pickWithExplicitInclude() {
        val src =
            """
            |use("standard-lib.kap")
            |2 3 ⊇ 1 2 3 4 5 6
            """.trimMargin()
        parseAPLExpression(src)
    }

    @Test
    fun pickWith2DArray() {
        parseAPLExpression("(2 2) (3 0) (3 1) ⊇ 5 6 ⍴ ⍳100", true).let { result ->
            assert1DArray(arrayOf(14, 18, 19), result)
        }
    }

    @Test
    fun pickWith3DArray() {
        parseAPLExpression("(1 1 1) (1 3 0) (0 3 1) ⊇ 2 5 6 ⍴ ⍳100", true).let { result ->
            assert1DArray(arrayOf(37, 48, 19), result)
        }
    }

    @Test
    fun pickWith2DResult() {
        parseAPLExpression("(2 2 ⍴ 1 0 4 4) ⊇ 100+⍳100", true).let { result ->
            assertDimension(dimensionsOfSize(2, 2), result)
            assertArrayContent(arrayOf(101, 100, 104, 104), result)
        }
    }

    @Test
    fun pickWithEnclosedInputAnd2DResult() {
        parseAPLExpression("(2 2 ⍴ 1 0 4 4) ⊇ (⊂10 100)+⍳100", true).let { result ->
            assertDimension(dimensionsOfSize(2, 2), result)
            assert1DArray(arrayOf(11, 101), result.valueAt(0))
            assert1DArray(arrayOf(10, 100), result.valueAt(1))
            assert1DArray(arrayOf(14, 104), result.valueAt(2))
            assert1DArray(arrayOf(14, 104), result.valueAt(3))
        }
    }

    @Test
    fun pickWithInvalidSelectionDimensions0() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(2 2 ⍴ 1 0 4 4) ⊇ 2 2 ⍴ ⍳4", true)
        }
    }

    @Test
    fun pickWithInvalidSelectionDimensions0Generic() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(2 2 ⍴ 1 0 4 4) ⊇ int:ensureGeneric 2 2 ⍴ ⍳4", true)
        }
    }

    @Test
    fun pickWithInvalidSelectionDimensions1() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(,⊂1 2) ⊇ ⍳4", true)
        }
    }

    @Test
    fun pickWithInvalidSelectionDimensions1Generic() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(,⊂1 2) ⊇ int:ensureGeneric ⍳4", true)
        }
    }

    @Test
    fun pickEmptyResult() {
        parseAPLExpression("⍬ ⊇ 1 2 3 4 5", true).let { result ->
            assertAPLNull(result)
        }
    }

    @Test
    fun pickInvalidDimensionOfIndex0() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(,⊂2 2 ⍴ 1 2 3 4) ⊇ ⍳10")
        }
    }

    @Test
    fun pickInvalidDimensionOfIndex1() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(,⊂2 2 ⍴ 1 2 3 4) ⊇ 100 100 ⍴ 1 2 3")
        }
    }

    @Test
    fun pickWithUnder0() {
        parseAPLExpression("(1+)⍢(2⊇) 10 20 30 40 50 60").let { result ->
            assert1DArray(arrayOf(10, 20, 31, 40, 50, 60), result)
        }
    }

    @Test
    fun pickWithUnder0Generic() {
        parseAPLExpression("(1+)⍢(2⊇) int:ensureGeneric 10 20 30 40 50 60").let { result ->
            assert1DArray(arrayOf(10, 20, 31, 40, 50, 60), result)
        }
    }

    @Test
    fun pickWithUnder1() {
        parseAPLExpression("(1+)⍢(2 5⊇) 10 20 30 40 50 60").let { result ->
            assert1DArray(arrayOf(10, 20, 31, 40, 50, 61), result)
        }
    }

    @Test
    fun pickWithUnder2() {
        parseAPLExpression("{9 9}⍢(2 5⊇) 10 20 30 40 50 60").let { result ->
            assert1DArray(arrayOf(10, 20, 9, 40, 50, 9), result)
        }
    }

    @Test
    fun pickWithUnder3() {
        parseAPLExpression("(0 1) (2 1) (2 2) (1 2) (0 3) -⍢⊇ 3 4 ⍴ 10×⍳100").let { result ->
            assertDimension(dimensionsOfSize(3, 4), result)
            assertArrayContent(arrayOf(0, -10, 20, -30, 40, 50, -60, 70, 80, -90, -100, 110), result)
        }
    }

    @Test
    fun pickWithUnderInvalidDimension0() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(0 1 1) (2 1) (2 2) (1 2) (0 3) -⍢⊇ 3 4 ⍴ 10×⍳100")
        }
    }

    @Test
    fun pickWithUnderInvalidDimension1() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(0 1) (2 1) (2 2) (1 2) (0 3) {1 2 3 4}⍢⊇ 3 4 ⍴ 10×⍳100")
        }
    }

    @Test
    fun pickwithUnderAndSideEffect() {
        parseAPLExpressionWithOutput("(0 1) (2 1) (2 2) (1 2) (0 3) {0 1 2 3 4}⍢⊇ io:print¨ 3 4 ⍴ 10×⍳100").let { (result, out) ->
            assertDimension(dimensionsOfSize(3, 4), result)
            assertArrayContent(arrayOf(0, 0, 20, 4, 40, 50, 3, 70, 80, 1, 2, 110), result)
            assertEquals("02040507080110", out)
        }
    }

    @Test
    fun pickWithUnderAndScalarExt() {
        parseAPLExpression("{9}⍢(2 5⊇) 10 20 30 40 50 60").let { result ->
            assert1DArray(arrayOf(10, 20, 9, 40, 50, 9), result)
        }
    }
}
