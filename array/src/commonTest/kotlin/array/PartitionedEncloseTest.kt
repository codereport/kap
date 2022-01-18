package array

import kotlin.test.Test
import kotlin.test.assertFailsWith

class PartitionedEncloseTest : APLTest() {
    @Test
    fun simpleEnclose() {
        parseAPLExpression("1 0 1 0 0 ⊆ ⍳5").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(Inner1D(arrayOf(0, 1)), Inner1D(arrayOf(2, 3, 4))), result)
        }
    }

    @Test
    fun trailingEmpty() {
        parseAPLExpression("0 0 0 0 1 ⊆ \"qwert\"").let { result ->
            assert1DArray(arrayOf("qwer", "t"), result)
        }
    }

    @Test
    fun leftArgTooLarge() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("1 1 1 1 1 1 2 ⊆ \"qwert\"")
        }
    }

    @Test
    fun leftArgTooSmall() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("1 2 ⊆ \"qwe\"")
        }
    }

    @Test
    fun multiBlank() {
        parseAPLExpression("1 2 0 2 ⊆ \"qwer\"").let { result ->
            assert1DArray(arrayOf("q", InnerAPLNull(), "we", InnerAPLNull(), "r"), result)
        }
    }

    @Test
    fun twoDimensionalDefaultAxis() {
        parseAPLExpression("1 0 1 ⊆ 3 3 ⍴ 1000+⍳9").let { result ->
            assertDimension(dimensionsOfSize(3, 2), result)
            assertArrayContent(
                arrayOf(
                    Inner1D(arrayOf(1000, 1001)),
                    Inner1D(arrayOf(1002)),
                    Inner1D(arrayOf(1003, 1004)),
                    Inner1D(arrayOf(1005)),
                    Inner1D(arrayOf(1006, 1007)),
                    Inner1D(arrayOf(1008))),
                result)
        }
    }

    @Test
    fun twoDimensionalFirstAxis() {
        parseAPLExpression("1 0 1 ⊆[0] 3 3 ⍴ 1000+⍳9").let { result ->
            assertDimension(dimensionsOfSize(2, 3), result)
            assertArrayContent(
                arrayOf(
                    Inner1D(arrayOf(1000, 1003)),
                    Inner1D(arrayOf(1001, 1004)),
                    Inner1D(arrayOf(1002, 1005)),
                    Inner1D(arrayOf(1006)),
                    Inner1D(arrayOf(1007)),
                    Inner1D(arrayOf(1008))),
                result)
        }
    }

    @Test
    fun illegalAxis0() {
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("1 0 1 ⊆[2] 3 3 ⍴ 1000+⍳9")
        }
    }

    @Test
    fun illegalAxis1() {
        assertFailsWith<IllegalAxisException> {
            parseAPLExpression("1 0 1 0 0 ⊆[1] ⍳5")
        }
    }

    @Test
    fun emptyLeftArg() {
        parseAPLExpression("⍬ ⊂ ⍬").let { result ->
            assertDimension(dimensionsOfSize(0), result)
        }
    }

    @Test
    fun extraEmptyResult0() {
        parseAPLExpression("1 4 1 0 0 1 ⊆ \"abcdef\"").let { result ->
            assertDimension(dimensionsOfSize(7), result)
            assertArrayContent(
                arrayOf(
                    "a",
                    InnerAPLNull(),
                    InnerAPLNull(),
                    InnerAPLNull(),
                    "b",
                    "cde",
                    "f"), result)
        }
    }

    @Test
    fun extraEmptyResult1() {
        parseAPLExpression("1 4 0 1 0 1 ⊆ \"abcdef\"").let { result ->
            assertDimension(dimensionsOfSize(7), result)
            assertArrayContent(
                arrayOf(
                    "a",
                    InnerAPLNull(),
                    InnerAPLNull(),
                    InnerAPLNull(),
                    "bc",
                    "de",
                    "f"), result)
        }
    }

    @Test
    fun extraEmptyResult2() {
        parseAPLExpression("1 4 0 1 0 2 ⊆ \"abcdef\"").let { result ->
            assertDimension(dimensionsOfSize(8), result)
            assertArrayContent(
                arrayOf(
                    "a",
                    InnerAPLNull(),
                    InnerAPLNull(),
                    InnerAPLNull(),
                    "bc",
                    "de",
                    InnerAPLNull(),
                    "f"), result)
        }
    }

    @Test
    fun extraEmptyResult3() {
        parseAPLExpression("2 4 0 1 0 2 ⊆ \"abcdef\"").let { result ->
            assertDimension(dimensionsOfSize(9), result)
            assertArrayContent(
                arrayOf(
                    InnerAPLNull(),
                    "a",
                    InnerAPLNull(),
                    InnerAPLNull(),
                    InnerAPLNull(),
                    "bc",
                    "de",
                    InnerAPLNull(),
                    "f"), result)
        }
    }
}
