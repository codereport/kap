package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PickTest : APLTest() {
    @Test
    fun simplePick() {
        parseAndTestWithGeneric("2 ⊇ {GENERIC} 100+⍳10", true) { result ->
            assertSimpleNumber(102, result)
        }
    }

    @Test
    fun simplePickWithAddDouble() {
        parseAndTestWithGeneric("10.0 + 2 ⊇ {GENERIC} 100.0+⍳10", true) { result ->
            assertSimpleDouble(112.0, result)
        }
    }

    @Test
    fun simplePickArrayResult() {
        parseAndTestWithGeneric("2 3 ⊇ {GENERIC} 100+⍳10", true) { result ->
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
        parseAndTestWithGeneric("(2 2) (3 0) (3 1) ⊇ {GENERIC} 5 6 ⍴ ⍳100", true) { result ->
            assert1DArray(arrayOf(14, 18, 19), result)
        }
    }

    @Test
    fun pickWith2DArrayAddDouble() {
        parseAndTestWithGeneric("10.0 + (2 2) (3 0) (3 1) ⊇ {GENERIC} 5 6 ⍴ 100.0+⍳100", true) { result ->
            assert1DArray(arrayOf(InnerDouble(124.0), InnerDouble(128.0), InnerDouble(129.0)), result)
        }
    }

    @Test
    fun pickWith3DArray() {
        parseAndTestWithGeneric("(1 1 1) (1 3 0) (0 3 1) ⊇ {GENERIC} 2 5 6 ⍴ ⍳100", true) { result ->
            assert1DArray(arrayOf(37, 48, 19), result)
        }
    }

    @Test
    fun pickWith2DResult() {
        parseAndTestWithGeneric("(2 2 ⍴ 1 0 4 4) ⊇ {GENERIC} 100+⍳100", true) { result ->
            assertDimension(dimensionsOfSize(2, 2), result)
            assertArrayContent(arrayOf(101, 100, 104, 104), result)
        }
    }

    @Test
    fun pickWithEnclosedInputAnd2DResult() {
        parseAndTestWithGeneric("(2 2 ⍴ 1 0 4 4) ⊇ {GENERIC} (⊂10 100)+⍳100", true) { result ->
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
        parseAndTestWithGeneric("⍬ ⊇ {GENERIC} 1 2 3 4 5", true) { result ->
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
        parseAndTestWithGeneric("(1+)⍢(2⊇) {GENERIC} 10 20 30 40 50 60") { result ->
            assert1DArray(arrayOf(10, 20, 31, 40, 50, 60), result)
        }
    }

    @Test
    fun pickWithUnder1() {
        parseAndTestWithGeneric("(1+)⍢(2 5⊇) {GENERIC} 10 20 30 40 50 60") { result ->
            assert1DArray(arrayOf(10, 20, 31, 40, 50, 61), result)
        }
    }

    @Test
    fun pickWithUnder2() {
        parseAndTestWithGeneric("{9 9}⍢(2 5⊇) {GENERIC} 10 20 30 40 50 60") { result ->
            assert1DArray(arrayOf(10, 20, 9, 40, 50, 9), result)
        }
    }

    @Test
    fun pickWithUnder3() {
        parseAndTestWithGeneric("(0 1) (2 1) (2 2) (1 2) (0 3) -⍢⊇ {GENERIC} 3 4 ⍴ 10×⍳100") { result ->
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
    fun pickWithUnderAndSideEffect() {
        parseAPLExpressionWithOutput("(0 1) (2 1) (2 2) (1 2) (0 3) {0 1 2 3 4}⍢⊇ io:print¨ 3 4 ⍴ 10×⍳100").let { (result, out) ->
            assertDimension(dimensionsOfSize(3, 4), result)
            assertArrayContent(arrayOf(0, 0, 20, 4, 40, 50, 3, 70, 80, 1, 2, 110), result)
            assertEquals("02040507080110", out)
        }
    }

    @Test
    fun pickWithUnderAndSideEffectGeneric() {
        parseAPLExpressionWithOutput("(0 1) (2 1) (2 2) (1 2) (0 3) {0 1 2 3 4}⍢⊇ int:ensureGeneric io:print¨ 3 4 ⍴ 10×⍳100").let { (result, out) ->
            assertDimension(dimensionsOfSize(3, 4), result)
            assertArrayContent(arrayOf(0, 0, 20, 4, 40, 50, 3, 70, 80, 1, 2, 110), result)
            assertEquals("02040507080110", out)
        }
    }

    @Test
    fun pickWithUnderAndScalarExt() {
        parseAndTestWithGeneric("{9}⍢(2 5⊇) {GENERIC} 10 20 30 40 50 60") { result ->
            assert1DArray(arrayOf(10, 20, 9, 40, 50, 9), result)
        }
    }
}
