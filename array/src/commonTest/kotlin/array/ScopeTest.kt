package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ScopeTest : APLTest() {
    @Test
    fun leftBoundFunctionsWithDifferentEnvironment0() {
        val src =
            """
            |stringToGraphemes ⇐ (1,≢)⍛⍴ unicode:toGraphemes
            |{comp ⍵} (stringToGraphemes⍕)¨ 1 2 3            
            """.trimMargin()
        parseAPLExpression(src).let { result ->
            assertDimension(dimensionsOfSize(3), result)
            result.valueAt(0).let { v ->
                assertDimension(dimensionsOfSize(1, 1), v)
                assertString("1", v.valueAt(0))
            }
            result.valueAt(1).let { v ->
                assertDimension(dimensionsOfSize(1, 1), v)
                assertString("2", v.valueAt(0))
            }
            result.valueAt(2).let { v ->
                assertDimension(dimensionsOfSize(1, 1), v)
                assertString("3", v.valueAt(0))
            }
        }
    }

    @Test
    fun leftBoundFunctionsWithDifferentEnvironment1() {
        val src =
            """
            |a ⇐ 'array≡typeof
            |stringToGraphemes ⇐ (1,≢)⍛⍴ unicode:toGraphemes
            |∇ foo (v) {
            |  stringToGraphemes ⍕v
            |}
            |foo 1            
            """.trimMargin()
        parseAPLExpression(src).let { result ->
            assertDimension(dimensionsOfSize(1, 1), result)
            assertString("1", result.valueAt(0))
        }
    }

    @Test
    fun leftBoundFunctionsWithDifferentEnvironment2() {
        val src =
            """
            |{
            |  stringToGraphemes ⇐ (1,≢)⍛⍴ unicode:toGraphemes
            |  {comp ⍵} (stringToGraphemes⍕)¨ 1 2 3
            |} 0
            """.trimMargin()
        parseAPLExpression(src).let { result ->
            assertDimension(dimensionsOfSize(3), result)
            result.valueAt(0).let { v ->
                assertDimension(dimensionsOfSize(1, 1), v)
                assertString("1", v.valueAt(0))
            }
            result.valueAt(1).let { v ->
                assertDimension(dimensionsOfSize(1, 1), v)
                assertString("2", v.valueAt(0))
            }
            result.valueAt(2).let { v ->
                assertDimension(dimensionsOfSize(1, 1), v)
                assertString("3", v.valueAt(0))
            }
        }
    }

    @Test
    fun axisAssignedFunctionWithDifferentEnv0() {
        val src =
            """
            |abc ⇐ +[1]
            |{comp ⍵} abc¨ 2 3 ⍴ 1+⍳6
            """.trimMargin()
        parseAPLExpression(src).let { result ->
            assertDimension(dimensionsOfSize(2, 3), result)
            assertArrayContent(arrayOf(1, 2, 3, 4, 5, 6), result)
        }
    }

    @Test
    fun axisAssignedFunctionWithDifferentEnv1() {
        val src =
            """
            |a ⇐ 'array≡typeof
            |abc ⇐ +[1]
            |∇ foo (v) {
            |  abc¨ v
            |}
            |{comp ⍵} foo 4 5 6
            """.trimMargin()
        parseAPLExpression(src).let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(4, 5, 6), result)
        }
    }

    @Test
    fun axisAssignedFunctionWithDifferentEnv2() {
        val src =
            """
            |a ⇐ 'array≡typeof
            |y ← 1
            |abc ⇐ { +[y] ⍵ }
            |∇ foo (v) {
            |  abc¨ v
            |}
            |{comp ⍵} foo 4 5 6
            """.trimMargin()
        parseAPLExpression(src).let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(4, 5, 6), result)
        }
    }

    @Test
    fun axisAssignedFunctionWithDifferentEnv3() {
        val src =
            """
            |{
            |  a ⇐ 'array≡typeof
            |  y ← 1
            |  abc ⇐ { +[y] ⍵ }
            |  ∇ foo (v) {
            |    abc¨ v
            |  }
            |  {comp ⍵} foo 4 5 6
            |} 0
            """.trimMargin()
        assertFailsWith<InvalidOperatorArgument> {
            parseAPLExpression(src)
        }
    }

    @Test
    fun axisAssignedFunctionWithDifferentEnv4() {
        val src =
            """
            |{
            |  a ⇐ 'array≡typeof
            |  y ← 1
            |  abc ← λ{ +[y] ⍵ }
            |  foo ⇐ {
            |    io:print "foo"
            |    ⍞abc¨ ⍵
            |  }
            |  {comp ⍵} foo 4 5 6
            |} 0
            """.trimMargin()
        parseAPLExpressionWithOutput(src).let { (result, out) ->
            assert1DArray(arrayOf(4, 5, 6), result)
            assertEquals("foo", out)
        }
    }

    @Test
    fun axisAssignedFunctionWithDifferentEnv5() {
        val src =
            """
            |abc ⇐ 10 20 30+[1]
            |{comp ⍵} abc 2 3 ⍴ 1+⍳6
            """.trimMargin()
        parseAPLExpression(src).let { result ->
            assertDimension(dimensionsOfSize(2, 3), result)
            assertArrayContent(arrayOf(11, 22, 33, 14, 25, 36), result)
        }
    }

    @Test
    fun axisAssignedFunctionWithDifferentEnv6() {
        val src =
            """
            |a ⇐ 'array≡typeof
            |abc ⇐ 10 20+[1]
            |∇ foo (v) {
            |  abc v
            |}
            |{comp ⍵} foo 3 2 ⍴ 1+⍳6
            """.trimMargin()
        parseAPLExpression(src).let { result ->
            assertDimension(dimensionsOfSize(3, 2), result)
            assertArrayContent(arrayOf(11, 22, 13, 24, 15, 26), result)
        }
    }

    @Test
    fun axisAssignedFunctionWithDifferentEnv7() {
        val src =
            """
            |{
            |  abc ⇐ 10 20 30+[1]
            |  {comp ⍵} abc 2 3 ⍴ 1+⍳6
            |} 0
            """.trimMargin()
        parseAPLExpression(src).let { result ->
            assertDimension(dimensionsOfSize(2, 3), result)
            assertArrayContent(arrayOf(11, 22, 33, 14, 25, 36), result)
        }
    }

    @Test
    fun axisAssignedFunctionWithDiscardResult() {
        val src =
            """
            |1 + 2
            |10 100 1000 +[0] 3 3 ⍴ ⍳9
            |3
            """.trimMargin()
        assertSimpleNumber(3, parseAPLExpression(src))
    }

    @Test
    fun axisAssignedFunctionWithDiscardResultAndLeftAssigned() {
        val src =
            """
            |1 + 2
            |(10 100 1000 +[0]) 3 3 ⍴ ⍳9
            |3
            """.trimMargin()
        assertSimpleNumber(3, parseAPLExpression(src))
    }

    @Test
    fun leftBoundFunctionInDfn() {
        val src =
            """
            |foo ⇐ { a ← λ{ 1 2 3 +[io:print 1] ⍵ } ◊ io:print 2 ◊ a }
            |io:print 3
            |⍞(foo 0) 2 3 ⍴ 1+⍳6
            """.trimMargin()
        val (result, out) = parseAPLExpressionWithOutput(src)
        assertDimension(dimensionsOfSize(2, 3), result)
        assertArrayContent(arrayOf(2, 4, 6, 5, 7, 9), result)
        assertEquals("321", out)
    }

    @Test
    fun escapingForEachScope() {
        parseAPLExpression("a0←10 ⋄ { a←⍵ ⋄ { { a + ⍵ }¨ ⍵+1 } 101 201 301 401 }¨ 10 20 30 40").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assert1DArray(arrayOf(112, 212, 312, 412), result.valueAt(0))
            assert1DArray(arrayOf(122, 222, 322, 422), result.valueAt(1))
            assert1DArray(arrayOf(132, 232, 332, 432), result.valueAt(2))
            assert1DArray(arrayOf(142, 242, 342, 442), result.valueAt(3))
        }
    }
}
