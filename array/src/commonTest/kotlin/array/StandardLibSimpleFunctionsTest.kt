package array

import kotlin.test.Test

class StandardLibSimpleFunctionsTest : APLTest() {
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
            |⍝use("output3.kap")
            |⍝o3:format 2 2 ⍴ 1 100 10000 1.2
            |⍝⊇
            |2 3 ⊇ 1 2 3 4 5 6
            """.trimMargin()
        parseAPLExpression(src)
    }

    @Test
    fun formatterTest() {
        val src =
            """
            |use("standard-lib.kap")
            |use("output3.kap")
            |o3:format 2 2 ⍴ 1 100 10000 1.2
            """.trimMargin()
        parseAPLExpression(src)
    }
}
