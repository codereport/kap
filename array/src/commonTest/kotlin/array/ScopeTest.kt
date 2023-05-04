package array

import kotlin.test.Test

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
            result.valueAt(0).let { v ->
                assertString("1", v)
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
        parseAPLExpression(src).let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(4, 5, 6), result)
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
            |    io:print abc
            |    ⍞abc¨ ⍵
            |  }
            |  {comp ⍵} foo 4 5 6
            |} 0
            """.trimMargin()
        parseAPLExpression(src).let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(4, 5, 6), result)
        }
    }
}
