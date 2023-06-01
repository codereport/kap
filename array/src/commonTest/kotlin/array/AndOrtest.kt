package array

import kotlin.test.Test
import kotlin.test.assertEquals

class AndOrtest : APLTest() {
    @Test
    fun simpleAnd() {
        assertSimpleNumber(1, parseAPLExpression("1 and 1"))
        assertSimpleNumber(0, parseAPLExpression("0 and 0"))
        assertSimpleNumber(0, parseAPLExpression("1 and 0"))
        assertSimpleNumber(0, parseAPLExpression("0 and 1"))
    }

    @Test
    fun simpleOr() {
        assertSimpleNumber(1, parseAPLExpression("1 or 1"))
        assertSimpleNumber(0, parseAPLExpression("0 or 0"))
        assertSimpleNumber(1, parseAPLExpression("1 or 0"))
        assertSimpleNumber(1, parseAPLExpression("0 or 1"))
    }

    @Test
    fun precedenceTest0() {
        assertSimpleNumber(1, parseAPLExpression("1+0 and 1+0"))
    }

    @Test
    fun precedenceTest1() {
        assertSimpleNumber(1, parseAPLExpression("1+0 and 1+0 or 0"))
    }

    @Test
    fun evaluationOrder() {
        assertSimpleNumber(1, parseAPLExpression("0 and 0 or 1"))
        assertSimpleNumber(0, parseAPLExpression("1 or 0 and 0"))
    }

    @Test
    fun shortCircuitTest0() {
        parseAPLExpressionWithOutput("io:print 1 or io:print 0").let { (result, out) ->
            assertSimpleNumber(1, result)
            assertEquals("1", out)
        }
    }

    @Test
    fun shortCircuitTest1() {
        parseAPLExpressionWithOutput("io:print 0 or io:print 0").let { (result, out) ->
            assertSimpleNumber(0, result)
            assertEquals("00", out)
        }
    }
}
