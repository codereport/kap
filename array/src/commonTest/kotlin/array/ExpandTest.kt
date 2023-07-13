package array

import kotlin.test.Test

class ExpandTest : APLTest() {
    @Test
    fun simpleExpand1D() {
        parseAPLExpression("1 0 1 1 \\ 3 4 5").let { result ->
            assert1DArray(arrayOf(3, 0, 4, 5), result)
        }
    }

    @Test
    fun expandWithScalar() {
        parseAPLExpression("1 0 1 1 \\ 2").let { result ->
            assert1DArray(arrayOf(2, 0, 2, 2), result)
        }
    }

    @Test
    fun expand2DFirstAxis() {
        parseAPLExpression("1 0 1 1 \\ 3 3 ⍴ 100+⍳9").let { result ->
            assertDimension(dimensionsOfSize(3, 4), result)
            assertArrayContent(
                arrayOf(
                    100, 0, 101, 102,
                    103, 0, 104, 105,
                    106, 0, 107, 108),
                result)
        }
    }

    @Test
    fun expand2DLastAxis() {
        parseAPLExpression("1 0 1 1 ⍀ 3 3 ⍴ 100+⍳9").let { result ->
            assertDimension(dimensionsOfSize(4, 3), result)
            assertArrayContent(
                arrayOf(
                    100, 101, 102,
                    0, 0, 0,
                    103, 104, 105,
                    106, 107, 108),
                result)
        }
    }

    @Test
    fun expand2DLastAxisWithExplicitAxis() {
        parseAPLExpression("1 0 1 1 \\[0] 3 3 ⍴ 100+⍳9").let { result ->
            assertDimension(dimensionsOfSize(4, 3), result)
            assertArrayContent(
                arrayOf(
                    100, 101, 102,
                    0, 0, 0,
                    103, 104, 105,
                    106, 107, 108),
                result)
        }
    }

    @Test
    fun expand2DFirstAxisWithExplicitAxis() {
        parseAPLExpression("1 0 1 1 \\[1] 3 3 ⍴ 100+⍳9").let { result ->
            assertDimension(dimensionsOfSize(3, 4), result)
            assertArrayContent(
                arrayOf(
                    100, 0, 101, 102,
                    103, 0, 104, 105,
                    106, 0, 107, 108),
                result)
        }
    }

    @Test
    fun expand4DArray() {
        parseAPLExpression("1 1 0 ⍀ 2 4 4 4 ⍴ 100+⍳1000").let { result ->
            assertDimension(dimensionsOfSize(3, 4, 4, 4), result)
            assertArrayContent(
                arrayOf(
                    100, 101, 102, 103,
                    104, 105, 106, 107,
                    108, 109, 110, 111,
                    112, 113, 114, 115,

                    116, 117, 118, 119,
                    120, 121, 122, 123,
                    124, 125, 126, 127,
                    128, 129, 130, 131,

                    132, 133, 134, 135,
                    136, 137, 138, 139,
                    140, 141, 142, 143,
                    144, 145, 146, 147,

                    148, 149, 150, 151,
                    152, 153, 154, 155,
                    156, 157, 158, 159,
                    160, 161, 162, 163,

                    164, 165, 166, 167,
                    168, 169, 170, 171,
                    172, 173, 174, 175,
                    176, 177, 178, 179,

                    180, 181, 182, 183,
                    184, 185, 186, 187,
                    188, 189, 190, 191,
                    192, 193, 194, 195,

                    196, 197, 198, 199,
                    200, 201, 202, 203,
                    204, 205, 206, 207,
                    208, 209, 210, 211,

                    212, 213, 214, 215,
                    216, 217, 218, 219,
                    220, 221, 222, 223,
                    224, 225, 226, 227,

                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0,

                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0,

                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0,

                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0),
                result)
        }
    }

    @Test
    fun extendedNumRepeats() {
        parseAPLExpression("1 2 0 \\ 10 11").let { result ->
            assert1DArray(arrayOf(10, 11, 11, 0), result)
        }
    }

    @Test
    fun extendedNegativeRepeat() {
        parseAPLExpression("1 2 ¯2 \\ 10 11").let { result ->
            assert1DArray(arrayOf(10, 11, 11, 0, 0), result)
        }
    }

    @Test
    fun expandSize1Axis() {
        parseAPLExpression("0 2 2 \\ 3 1 ⍴ 100+⍳9").let { result ->
            assertDimension(dimensionsOfSize(3, 5), result)
            assertArrayContent(
                arrayOf(
                    0, 100, 100, 100, 100,
                    0, 101, 101, 101, 101,
                    0, 102, 102, 102, 102,
                ),
                result)
        }
    }
}
