package array

import kotlin.test.Test

class RankAdjustTest : APLTest() {
    @Test
    fun rankUpScalar() {
        parseAPLExpression("< 1").let { result ->
            assertDimension(dimensionsOfSize(1), result)
            assertArrayContent(arrayOf(1), result)
        }
    }

    @Test
    fun rankUp1d() {
        parseAPLExpression("< ⍳9").let { result ->
            assertDimension(dimensionsOfSize(1, 9), result)
            assertArrayContent(arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8), result)
        }
    }

    @Test
    fun rankUp2d() {
        parseAPLExpression("< 3 3 ⍴ ⍳9").let { result ->
            assertDimension(dimensionsOfSize(1, 3, 3), result)
            assertArrayContent(arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8), result)
        }
    }

    @Test
    fun rankDownScalar() {
        assertSimpleNumber(1, parseAPLExpression("> 1"))
    }

    @Test
    fun rankDown1d() {
        parseAPLExpression("> ⍳9").let { result ->
            assertDimension(dimensionsOfSize(9), result)
            assertArrayContent(arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8), result)
        }
    }

    @Test
    fun rankDown2d0() {
        parseAPLExpression("> ⍳9").let { result ->
            assertDimension(dimensionsOfSize(9), result)
            assertArrayContent(arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8), result)
        }
    }

    @Test
    fun rankDown2d1() {
        parseAPLExpression("> 1 3 ⍴ ⍳3").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(0, 1, 2), result)
        }
    }

    @Test
    fun rankDown3d0() {
        parseAPLExpression("> 3 3 3 ⍴ ⍳27").let { result ->
            assertDimension(dimensionsOfSize(9, 3), result)
            assertArrayContent(
                arrayOf(
                    0, 1, 2, 3, 4, 5, 6, 7, 8,
                    9, 10, 11, 12, 13, 14, 15, 16, 17,
                    18, 19, 20, 21, 22, 23, 24, 25, 26),
                result)
        }
    }

    @Test
    fun rankDown3d1() {
        parseAPLExpression("> 1 3 3 ⍴ ⍳27").let { result ->
            assertDimension(dimensionsOfSize(3, 3), result)
            assertArrayContent(arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8), result)
        }
    }

    @Test
    fun rankDown3d2() {
        parseAPLExpression("> 0 3 3 ⍴ ⍳27").let { result ->
            assertDimension(dimensionsOfSize(0, 3), result)
        }
    }
}
