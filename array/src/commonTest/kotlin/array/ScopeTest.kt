package array

import kotlin.test.Test
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
        assertFailsWith<VariableNotAssigned> {
            // This fails with VariableNotAssigned because abc is not in scope, and is therefore parsed as a variable.
            // This means that the following ¨ is not parsed as an operator because it's not prefixed by a function.
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
            |    io:print abc
            |    ⍞abc¨ ⍵
            |  }
            |  {comp ⍵} foo 4 5 6
            |} 0
            """.trimMargin()
        assertFailsWith<VariableNotAssigned> {
            // The unassigned variable in this case is abc which is not in scope, as user defined functions are always evaluated in
            // the root scope, regardless of where it is found in the source.
            parseAPLExpression(src)
        }
    }
}
