package array

import kotlin.test.Test

class OverAPLTest : APLTest() {
    @Test
    fun simpleOverTwoArg() {
        parseAPLExpression("11 (,⍥-) 22", withStandardLib = true).let { result ->
            assert1DArray(arrayOf(-11, -22), result)
        }
    }

    @Test
    fun simpleOverOneArg() {
        parseAPLExpression("({2×⍵}⍥-) 22", withStandardLib = true).let { result ->
            assertSimpleNumber(-44, result)
        }
    }
}
