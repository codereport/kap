package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReturnTest : APLTest() {
    @Test
    fun simpleReturn() {
        val src =
            """
            |∇ foo {
            |    io:print "a"
            |    →⍵+10
            |    io:print "b"
            |    ⍵+20
            |}
            |foo 100
            """.trimMargin()
        parseAPLExpressionWithOutput(src).let { (result, out) ->
            assertSimpleNumber(110, result)
            assertEquals("a", out)
        }
    }

    @Test
    fun multiReturn() {
        val src =
            """
            |∇ foo {
            |    io:print "a"
            |    →⍵+10
            |    io:print "b"
            |    →⍵+20
            |}
            |foo 100
            """.trimMargin()
        parseAPLExpressionWithOutput(src).let { (result, out) ->
            assertSimpleNumber(110, result)
            assertEquals("a", out)
        }
    }

    @Test
    fun innerScopeReturn() {
        val src =
            """
            |∇ foo {
            |    io:print "a"
            |    bar ⇐ { io:print "b" ◊ →⍵+10 ◊ io:print "c" ◊ ⍵+20 }
            |    io:print "d"
            |    x ← ⍵ + bar 1
            |    io:print "e"
            |    x
            |}
            |foo 30
            """.trimMargin()
        parseAPLExpressionWithOutput(src).let { (result, out) ->
            assertSimpleNumber(30 + 1 + 10, result)
            assertEquals("adbe", out)
        }
    }

    @Test
    fun returnInIf() {
        val src =
            """
            |∇ foo {
            |    result ← if (⍵≡0) {
            |        →10
            |    } else {
            |        20
            |    }
            |    result + 1
            |}
            |a ← foo 0
            |b ← foo 1
            |a b
            """.trimMargin()
        parseAPLExpression(src, withStandardLib = true).let { result ->
            assert1DArray(arrayOf(10, 21), result)
        }
    }

    @Test
    fun returnOutsideFunctionScopeIsParseError() {
        assertFailsWith<ParseException> {
            parseAPLExpression("→10")
        }
    }

    @Test
    fun returnFromNestedFunction() {
        val src =
            """
            |∇ foo {
            |    {
            |        bar ⇐ { →⍵+100 ◊ ⍵+200 }
            |        1 + bar ⍵
            |    } ⍵+2
            |}
            |foo 3
            """.trimMargin()
        parseAPLExpression(src).let { result ->
            assertSimpleNumber(106, result)
        }
    }

    @Test
    fun twoArgReturn() {
        val src =
            """
            |∇ foo {
            |    io:print ⍺
            |    io:print "a"
            |    ⍵ → 2
            |    io:print ⍺
            |    io:print "b"
            |    1
            |}
            |(3 foo 0) (4 foo 1)
            """.trimMargin()
        parseAPLExpressionWithOutput(src).let { (result, out) ->
            assert1DArray(arrayOf(1, 2), result)
            assertEquals("4a3a3b", out)
        }
    }

    @Test
    fun capturedFrameWithCollapse() {
        parseAPLExpression("{S ⇐ → ⋄ comp {(S⍣(81=×⍨⍵)) ⍵}¨⍳10} 0").let { result ->
            assertSimpleNumber(9, result)
        }
    }

    @Test
    fun capturedFrameNoCollapse() {
        assertFailsWith<ReturnValue> {
            parseAPLExpression("{S ⇐ → ⋄ {(S⍣(81=×⍨⍵)) ⍵}¨⍳10} 0")
        }
    }

    @Test
    fun capturedFrameNoLazyEvaluation() {
        parseAPLExpression("{ S ⇐ → ◊ 100 + { S ⍵+20 ◊ ⍵+1 } 10 } 0").let { result ->
            assertSimpleNumber(30, result)
        }
    }
}
