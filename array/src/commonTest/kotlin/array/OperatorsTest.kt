package array

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OperatorsTest : APLTest() {
    @Test
    fun commuteTwoArgs() {
        val result = parseAPLExpression("4÷⍨160")
        assertSimpleNumber(40, result)
    }

    @Test
    fun commuteOneArg() {
        val result = parseAPLExpression("+⍨3")
        assertSimpleNumber(6, result)
    }

    @Test
    fun commuteWithArrays() {
        val result = parseAPLExpression("2÷⍨8×⍳10")
        assertDimension(dimensionsOfSize(10), result)
        assertArrayContent(arrayOf(0, 4, 8, 12, 16, 20, 24, 28, 32, 36), result)
    }

    @Test
    fun multiOperators() {
        val result = parseAPLExpression("+⌻⍨1 2 3")
        assertDimension(dimensionsOfSize(3, 3), result)
        assertArrayContent(arrayOf(2, 3, 4, 3, 4, 5, 4, 5, 6), result)
    }

    @Test
    fun reduceWithoutAxisOnFunction() {
        parseAPLExpression(",/ (2 3 ⍴ 10+⍳6) (2 3 ⍴ 20+⍳6) (2 3 ⍴ 30+⍳6)").let { result ->
            assertTrue(result.isScalar())
            assertTrue(result !is APLSingleValue)
            val inner = result.valueAt(0)
            assertDimension(dimensionsOfSize(2, 9), inner)
            assertArrayContent(arrayOf(10, 11, 12, 20, 21, 22, 30, 31, 32, 13, 14, 15, 23, 24, 25, 33, 34, 35), inner)
        }
    }

    @Test
    fun catenateReduceDirect() {
        parseAPLExpression("a ⇐ {,[1]/⍵} ⋄ ⊃ a (2 4 3 ⍴ ⍳100) (2 3 ⍴ 100+⍳100)").let { result ->
            assertDimension(dimensionsOfSize(2, 5, 3), result)
            assertArrayContent(
                arrayOf(
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 100, 101, 102,
                    12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 103, 104, 105),
                result)
        }
    }

    @Test
    fun catenateReduceWithArgument() {
        parseAPLExpression("a ⇐ {,[⍺]/⍵} ⋄ ⊃ 1 a (2 4 3 ⍴ ⍳100) (2 3 ⍴ 100+⍳100)").let { result ->
            assertDimension(dimensionsOfSize(2, 5, 3), result)
            assertArrayContent(
                arrayOf(
                    0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 100, 101, 102,
                    12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 103, 104, 105),
                result)
        }
    }

    @Test
    fun reduceWithFunctionAxis() {
        parseAPLExpression(",[0]/ (2 3 ⍴ 10+⍳6) (2 3 ⍴ 20+⍳6) (2 3 ⍴ 30+⍳6)").let { result ->
            assertTrue(result.isScalar())
            assertTrue(result !is APLSingleValue)
            val inner = result.valueAt(0)
            assertDimension(dimensionsOfSize(6, 3), inner)
            assertArrayContent(arrayOf(10, 11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 25, 30, 31, 32, 33, 34, 35), inner)
        }
    }

    @Test
    fun reduceWithAxis() {
        parseAPLExpression(
            ",/[0] 3 3 ⍴ (2 3 ⍴ 10+⍳6) (2 3 ⍴ 20+⍳6) (2 3 ⍴ 30+⍳6) " +
                    "(2 3 ⍴ 40+⍳6) (2 3 ⍴ 50+⍳6) (2 3 ⍴ 60+⍳6) (2 3 ⍴ 70+⍳6) (2 3 ⍴ 80+⍳6) (2 3 ⍴ 90+⍳6)").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            fun verifySingle(n: Int, expected: Array<Int>) {
                val inner = result.valueAt(n)
                assertDimension(dimensionsOfSize(2, 9), inner)
                assertArrayContent(expected, inner)
            }
            verifySingle(0, arrayOf(10, 11, 12, 40, 41, 42, 70, 71, 72, 13, 14, 15, 43, 44, 45, 73, 74, 75))
            verifySingle(1, arrayOf(20, 21, 22, 50, 51, 52, 80, 81, 82, 23, 24, 25, 53, 54, 55, 83, 84, 85))
            verifySingle(2, arrayOf(30, 31, 32, 60, 61, 62, 90, 91, 92, 33, 34, 35, 63, 64, 65, 93, 94, 95))
        }
    }

    @Test
    fun customOperatorWithNumberArgument() {
        class FooCombinedFunction(val fn: APLFunction, val arg: Instruction, pos: FunctionInstantiation) : APLFunction(pos) {
            override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                val argValue = arg.evalWithContext(context)
                val c = fn.eval2Arg(context, argValue, a, null)
                return (c.ensureNumber(pos).asLong() * b.ensureNumber(pos).asLong()).makeAPLNumber()
            }
        }

        class FooOperator : APLOperatorValueRightArg {
            override fun combineFunction(fn: APLFunction, instr: Instruction, opPos: FunctionInstantiation): APLFunction {
                return FooCombinedFunction(fn, instr, opPos)
            }
        }

        val engine = Engine()
        engine.registerOperator(engine.currentNamespace.internAndExport("foo"), FooOperator())
        engine.parseAndEval(StringSourceLocation("1 (+foo 2) 3")).let { result ->
            assertSimpleNumber(9, result)
        }
    }

    @Test
    fun twoArgOperatorPrecedence() {
        parseAPLExpression("1 2+.×¨4 5").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(4, 10), result)
        }
    }

    @Test
    fun twoArgOperatorPrecedenceWithParen() {
        parseAPLExpression("1 2+.(×¨)4 5").let { result ->
            assertSimpleNumber(14, result)
        }
    }

    @Test
    fun twoArgWithApplyWithoutParen() {
        val result = parseAPLExpression(
            """
            |f0 ← λ{⍺+⍵}
            |20 200 -.⍞f0 1 2
            """.trimMargin())
        assertSimpleNumber(-181, result)
    }

    @Test
    fun twoArgWithApplyWithParen() {
        val result = parseAPLExpression(
            """
            |f0 ← λ{⍺+⍵}
            |20 200 (-.⍞f0) 1 2
            """.trimMargin())
        assertSimpleNumber(-181, result)
    }

    @Test
    fun twoArgWithApplyWithDoubleParen() {
        val result = parseAPLExpression(
            """
            |f0 ← λ{⍺+⍵}
            |20 200 (-.(⍞f0)) 1 2
            """.trimMargin())
        assertSimpleNumber(-181, result)
    }

    /**
     * This test ensures that axis arguments are evaluated at the point of definition instead of
     * when the function is called.
     */
    @Ignore
    @Test
    fun operatorAxisEvaluationTime0() {
        val engine = Engine()
        val out = StringBuilderOutput()
        engine.standardOutput = out
        engine.parseAndEval(StringSourceLocation("a ⇐ +/[io:print 0]")).let { result ->
            assertAPLNull(result)
            assertEquals("0", out.buf.toString())
        }
        engine.parseAndEval(StringSourceLocation("(a 8 7 6) (a 10 11 12)")).let { result ->
            assert1DArray(arrayOf(21, 33), result)
            assertEquals("0", out.buf.toString())
        }
    }
}
