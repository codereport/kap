package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FlowControlTest : APLTest() {
    @Test
    fun assertIf() {
        assertSimpleNumber(10, parseAPLExpression("if (1) { 10 }", true))
        parseAPLExpression("if (0) { 10 }", true).let { result ->
            assertDimension(dimensionsOfSize(0), result)
        }
    }

    @Test
    fun testIfElse() {
        assertSimpleNumber(10, parseAPLExpression("if (1) { 10 } else { 20 }", true))
        assertSimpleNumber(20, parseAPLExpression("if (0) { 10 } else { 20 }", true))
    }

    @Test
    fun testIfElseInExpressionLeftSide() {
        assertSimpleNumber(1010, parseAPLExpression("1000 + if (1) { 10 } else { 20 }", true))
        assertSimpleNumber(1020, parseAPLExpression("1000 + if (0) { 10 } else { 20 }", true))
    }

    @Test
    fun testIfElseInExpressionRightSide() {
        assertSimpleNumber(1004, parseAPLExpression("if (1) { 4 } else { 5 } + 1000", true))
        assertSimpleNumber(1005, parseAPLExpression("if (0) { 4 } else { 5 } + 1000", true))
    }

    @Test
    fun testIfElseInExpressionBothSides() {
        assertSimpleNumber(11004, parseAPLExpression("10000 + if (1) { 4 } else { 5 } + 1000", true))
        assertSimpleNumber(11005, parseAPLExpression("10000 + if (0) { 4 } else { 5 } + 1000", true))
    }

    @Test
    fun testSideEffectsInIf() {
        parseAPLExpressionWithOutput("io:print 10 ◊ if (1) { io:print 2 } ◊ io:print 3 ◊ 100", true).let { (result, s) ->
            assertSimpleNumber(100, result)
            assertEquals("1023", s)
        }
        parseAPLExpressionWithOutput("io:print 10 ◊ if (0) { io:print 2 } ◊ io:print 3 ◊ 100", true).let { (result, s) ->
            assertSimpleNumber(100, result)
            assertEquals("103", s)
        }
    }

    @Test
    fun testSideEffectsInIfElse() {
        parseAPLExpressionWithOutput(
            "io:print 10 ◊ if (1) { io:print 2 } else { io:print 4 } ◊ io:print 3 ◊ 100",
            true).let { (result, s) ->
            assertSimpleNumber(100, result)
            assertEquals("1023", s)
        }
        parseAPLExpressionWithOutput(
            "io:print 10 ◊ if (0) { io:print 2 } else { io:print 4 } ◊ io:print 3 ◊ 100",
            true).let { (result, s) ->
            assertSimpleNumber(100, result)
            assertEquals("1043", s)
        }
    }

    @Test
    fun testMultilineIf() {
        val result = parseAPLExpression(
            """
            |if (1) {
            |    10
            |}
            """.trimMargin(), true)
        assertSimpleNumber(10, result)
    }

    @Test
    fun testMultilineIfWithElse() {
        val result0 = parseAPLExpression(
            """
            |if (1) {
            |    10
            |} else {
            |    20
            |}
            """.trimMargin(), true)
        assertSimpleNumber(10, result0)

        val result1 = parseAPLExpression(
            """
            |if (0) {
            |    10
            |} else {
            |    20
            |}
            """.trimMargin(), true)
        assertSimpleNumber(20, result1)
    }

    @Test
    fun testIfWithArrayArgument() {
        parseAPLExpression("if (,1) { 10 } else { 20 }", true).let { result ->
            assertSimpleNumber(10, result)
        }
    }

    @Test
    fun testIfWithArrayArgumentTrue() {
        parseAPLExpression("if (,0) { 10 } else { 20 }", true).let { result ->
            assertSimpleNumber(10, result)
        }
    }

    @Test
    fun testIfWithArrayArgumentMultiValue() {
        parseAPLExpression("if (1 1 1 1) { 10 } else { 20 }", true).let { result ->
            assertSimpleNumber(10, result)
        }
    }

    @Test
    fun testIfWithArrayArgumentMultiValueTrue() {
        parseAPLExpression("if (0 0 0 0 0 0 0) { 10 } else { 20 }", true).let { result ->
            assertSimpleNumber(10, result)
        }
    }

    @Test
    fun recursionTest() {
        val (result, out) = parseAPLExpressionWithOutput(
            """
            |∇ foo (x) { if (x>0) { io:print x ◊ foo x-1 } else { 123 } }
            |foo 10
            """.trimMargin(), true)
        assertSimpleNumber(123, result)
        assertEquals("10987654321", out)
    }

    @Test
    fun lambdaRecursionTest() {
        val (result, out) = parseAPLExpressionWithOutput(
            """
            |foo ← λ{ x←⍵ ◊ if(x>0) { io:print x ◊ ⍞foo x-1 } else { 123 } }
            |⍞foo 10
            """.trimMargin(), true)
        assertSimpleNumber(123, result)
        assertEquals("10987654321", out)
    }

    @Test
    fun scopeTest0() {
        val result = parseAPLExpression(
            """
            |∇ foo (x) { λ{⍵+x} }
            |x ← foo 2
            |⍞x 100
            """.trimMargin())
        assertSimpleNumber(102, result)
    }

    @Test
    fun scopeTest1() {
        assertFailsWith<VariableNotAssigned> {
            parseAPLExpression(
                """
                |foo ← λ{ x + ⍵ }
                |bar ← λ{ x ← 10 ◊ (⍞foo 1) - ⍵ }
                |⍞bar 20 
                """.trimMargin())
        }
    }

    /**
     * Test the scope of local variables.
     */
    @Test
    fun scopeTestLocalVars() {
        val result = parseAPLExpression(
            """
            |foo ← λ{ x ← 1 + ⍵ }
            |bar ← λ{ x ← 100 ◊ y ← (⍞foo 2) ◊ y + x + ⍵ }
            |⍞bar 150
            """.trimMargin())
        assertSimpleNumber(253, result)
    }

    /**
     * Ensures that variables declared as `local(name)` will ensure allocation of a new binding.
     */
    @Test
    fun scopeTestExplicitLocal() {
        val result = parseAPLExpression(
            """
            |foo ← λ{ x ← 1 + ⍵ ◊ y ← { declare(:local x) x ← 2 ◊ x+50+⍵ } 190 ◊ y+x }
            |⍞foo 60
            """.trimMargin())
        assertSimpleNumber(303, result)
    }

    @Test
    fun whileLoopTest() {
        val src = "i←0 ◊ while (i<10) {io:print i ◊ prev←i ◊ i←i+1 ◊ prev+5}"
        parseAPLExpressionWithOutput(src, withStandardLib = true).let { (result, out) ->
            assertSimpleNumber(14, result)
            assertEquals("0123456789", out)
        }
    }

    @Test
    fun whileWithZeroIterations() {
        parseAPLExpressionWithOutput("io:print \"a\" ◊ while (0) { io:print \"b\" }", withStandardLib = true).let { (result, out) ->
            assertAPLNull(result)
            assertEquals("a", out)
        }
    }

    /**
     * Test the scope of the body in defsyntax forms
     */
    @Test
    fun defsyntaxScope() {
        parseAPLExpressionWithOutput("{ if(⍵<10) { io:print ⍵+100 ◊ 3 } } 4", true).let { (result, out) ->
            assertEquals("104", out)
            assertSimpleNumber(3, result)
        }
    }

    @Test
    fun unwindTest() {
        val engine = Engine()
        val output = StringBuilderOutput()
        engine.standardOutput = output
        assertFailsWith<APLEvalException> {
            engine.parseAndEval(
                StringSourceLocation("int:unwindProtect λ{io:print \"bar\" ◊ b ◊ io:print \"foo\"} λ{io:print \"qwe\"}"))
        }
        assertEquals("barqwe", output.buf.toString())
    }

    @Test
    fun unwindWithNoErrorTest() {
        val (result, out) = parseAPLExpressionWithOutput("int:unwindProtect λ{io:print \"bar\" ◊ io:print \"foo\" ◊ 9} λ{io:print \"qwe\"}")
        assertSimpleNumber(9, result)
        assertEquals("barfooqwe", out)
    }

    /**
     * Test that the atLeave function is called when scope is exited normally.
     */
    @Test
    fun atLeaveTest0() {
        val (result, out) = parseAPLExpressionWithOutput("{ { io:print 1+⍵ } atLeave 100 } 0")
        assertSimpleNumber(100, result)
        assertEquals("101", out)
    }

    /**
     * Test that the atLeave function is still called at non-local exit.
     */
    @Test
    fun atLeaveTest1() {
        val (result, out) = parseAPLExpressionWithOutput("{ { io:print 1+⍵ } atLeave 100 ◊ 'foo throw 1 } catch 1 2 ⍴ 'foo λ{50+⍺}")
        assertSimpleNumber(51, result)
        assertEquals("101", out)
    }
}
