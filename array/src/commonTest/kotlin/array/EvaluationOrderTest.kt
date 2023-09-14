package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class EvaluationOrderTest : APLTest() {
    /**
     * Check that evaluation order is right-to left:
     *
     * ```
     * A foo[B] C
     * ```
     *
     * The evaluation order should be C, B, A
     */
    @Test
    fun functionCallEvaluationOrder() {
        class FooFunction : APLFunctionDescriptor {
            inner class FooFunctionImpl(pos: FunctionInstantiation) : APLFunction(pos) {
                override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                    return (a.ensureNumber(pos).asInt(pos) +
                            b.ensureNumber(pos).asInt(pos) +
                            axis!!.ensureNumber(pos).asInt() * 2).makeAPLNumber()
                }
            }

            override fun make(instantiation: FunctionInstantiation) = FooFunctionImpl(instantiation)
        }

        val engine = Engine()
        engine.registerFunction(engine.internSymbol("foo", engine.currentNamespace), FooFunction())

        val output = StringBuilderOutput()
        engine.standardOutput = output
        val result = engine.parseAndEval(StringSourceLocation("(io:print 1) foo[io:print 2] io:print 3"))

        assertEquals("321", output.buf.toString())
        assertAPLValue(8, result)
    }

    /**
     * Ensure that the variable `a` is assigned before its value is read.
     */
    @Test
    fun expressionEvaluationOrder() {
        parseAPLExpression("a + 1 + a←2").let { result ->
            assertSimpleNumber(5, result)
        }
    }

    /**
     * Ensure that calls are evaluated when the result of a call is not used.
     * This is needed if a call is for side-effects only.
     */
    @Test
    fun collapseResultWhenNotUsed0() {
        parseAPLExpressionWithOutput(
            """
            |∇ printx (v) {
            |  io:print v
            |  v
            |}
            |
            |printx¨11 22
            |33
        """.trimMargin()).let { (result, out) ->
            assertEquals("1122", out)
            assertSimpleNumber(33, result)
        }
    }

    @Test
    fun collapseResultWhenNotUsed1() {
        parseAPLExpressionWithOutput(
            """
            |∇ (x) printx (y) {
            |  io:print x
            |  io:print y
            |  x,y
            |}
            |
            |1000 2000 printx¨1 2
            |33
        """.trimMargin()).let { (result, out) ->
            assertEquals("1000120002", out)
            assertSimpleNumber(33, result)
        }
    }

    @Test
    fun discardedResults0() {
        parseAPLExpressionWithOutput("io:print io:print¨ 1 2 3").let { (result, out) ->
            assertEquals("123123", out)
            assert1DArray(arrayOf(1, 2, 3), result)
        }
    }

    @Test
    fun discardedResults1() {
        parseAPLExpressionWithOutput("io:print 1000 2000 3000 {io:print ⍺ \":\" ⍵ ⋄ ⍺ + ⍵}¨ 1 2 3").let { (result, out) ->
            assertEquals("1000:12000:23000:3100120023003", out)
            assert1DArray(arrayOf(1001, 2002, 3003), result)
        }
    }

    @Test
    fun logicalOpAndTuples0() {
        parseAPLExpression("1 and 2 ; 10 or 11").let { result ->
            assertIs<APLList>(result)
            assertEquals(2, result.listSize())
            assertSimpleNumber(2, result.listElement(0))
            assertSimpleNumber(10, result.listElement(1))
        }
    }

    @Test
    fun logicalOpAndTuples1() {
        parseAPLExpression("1 and 2 ; 10 and 11").let { result ->
            assertIs<APLList>(result)
            assertEquals(2, result.listSize())
            assertSimpleNumber(2, result.listElement(0))
            assertSimpleNumber(11, result.listElement(1))
        }
    }

    @Test
    fun logicalOpAndTuples2() {
        parseAPLExpression("1 and 2 and 3 ; 100 or 200 or 300").let { result ->
            assertIs<APLList>(result)
            assertEquals(2, result.listSize())
            assertSimpleNumber(3, result.listElement(0))
            assertSimpleNumber(100, result.listElement(1))
        }
    }

    @Test
    fun logicalOpAndTuples4() {
        parseAPLExpression("1 and 2 ; 10 or 11 ; 100 or 101").let { result ->
            assertIs<APLList>(result)
            assertEquals(3, result.listSize())
            assertSimpleNumber(2, result.listElement(0))
            assertSimpleNumber(10, result.listElement(1))
            assertSimpleNumber(100, result.listElement(2))
        }
    }
}
