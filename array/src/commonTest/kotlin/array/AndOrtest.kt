package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
        assertSimpleNumber(1, parseAPLExpression("2+0 and 1+0 or 0"))
    }

    @Test
    fun evaluationOrder() {
        assertSimpleNumber(1, parseAPLExpression("0 and 0 or 1"))
        assertSimpleNumber(0, parseAPLExpression("1 or 0 and 0"))
    }

    @Test
    fun shortCircuitTestOr0() {
        parseAPLExpressionWithOutput("io:print 1 or io:print 0").let { (result, out) ->
            assertSimpleNumber(1, result)
            assertEquals("1", out)
        }
    }

    @Test
    fun shortCircuitTestOr1() {
        parseAPLExpressionWithOutput("io:print 0 or io:print 0").let { (result, out) ->
            assertSimpleNumber(0, result)
            assertEquals("00", out)
        }
    }

    @Test
    fun shortCircuitTestAnd0() {
        parseAPLExpressionWithOutput("io:print 1 and io:print 0").let { (result, out) ->
            assertSimpleNumber(0, result)
            assertEquals("10", out)
        }
    }

    @Test
    fun shortCircuitTestAnd1() {
        parseAPLExpressionWithOutput("io:print 0 and io:print 0").let { (result, out) ->
            assertSimpleNumber(0, result)
            assertEquals("0", out)
        }
    }

    @Test
    fun invalidSyntax0() {
        assertFailsWith<ParseException> {
            parseAPLExpression("and 1")
        }
    }

    @Test
    fun invalidSyntax1() {
        assertFailsWith<ParseException> {
            parseAPLExpression("1 and")
        }
    }

    @Test
    fun invalidSyntax2() {
        assertFailsWith<ParseException> {
            parseAPLExpression("and")
        }
    }

    @Test
    fun invalidSyntax3() {
        assertFailsWith<ParseException> {
            parseAPLExpression("or 1")
        }
    }

    @Test
    fun invalidSyntax4() {
        assertFailsWith<ParseException> {
            parseAPLExpression("1 or")
        }
    }

    @Test
    fun invalidSyntax5() {
        assertFailsWith<ParseException> {
            parseAPLExpression("or")
        }
    }
}
