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
        val result = parseAPLExpression("+⌺⍨1 2 3")
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
        class FooCombinedFunction(val fn: APLFunction, val arg: Instruction, pos: Position) : APLFunction(pos) {
            override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                val argValue = arg.evalWithContext(context)
                val c = fn.eval2Arg(context, argValue, a, null)
                return (c.ensureNumber(pos).asLong() * b.ensureNumber(pos).asLong()).makeAPLNumber()
            }
        }

        class FooOperator : APLOperatorValueRightArg {
            override fun combineFunction(fn: APLFunction, instr: Instruction, opPos: Position): APLFunction {
                return FooCombinedFunction(fn, instr, opPos)
            }
        }

        val engine = Engine()
        engine.registerOperator(engine.currentNamespace.internAndExport("foo"), FooOperator())
        engine.parseAndEval(StringSourceLocation("1 (+foo 2) 3"), newContext = false).let { result ->
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

    @Test
    fun testAxis0() {
        val src =
            """
            |a ⇐ def
            |a[2] 1
            """.trimMargin()
        val result = evalWithDebugFunctions(src)
        assertSimpleNumber(210, result)
    }

    @Test
    fun testAxis1() {
        val src =
            """
            |a ⇐ def[2]
            |a 1
            """.trimMargin()
        val result = evalWithDebugFunctions(src)
        assertSimpleNumber(210, result)
    }

    @Test
    fun testAxis2() {
        val src =
            """
            |a ⇐ def[2] abc
            |a 1
            """.trimMargin()
        val result = evalWithDebugFunctions(src)
        assertSimpleNumber(1210, result)
    }

    @Test
    fun testAxis3() {
        val src =
            """
            |a ⇐ def[2] abc[3]
            |a 1
            """.trimMargin()
        val result = evalWithDebugFunctions(src)
        assertSimpleNumber(301210, result)
    }

    @Test
    fun testAxis4() {
        val src =
            """
            |a ⇐ def[io:print 2] abc[io:print 3]
            |io:print 6
            |a 1
            """.trimMargin()
        val (result, out) = evalWithDebugFunctionsOutput(src)
        assertSimpleNumber(301210, result)
        assertEquals("326", out)
    }

    private fun evalWithDebugFunctions(src: String): APLValue {
        return evalWithDebugFunctionsOutput(src).first
    }

    private fun evalWithDebugFunctionsOutput(src: String): Pair<APLValue, String> {
        val engine = Engine()
        val namespace = engine.coreNamespace
        engine.registerOperator(namespace.internAndExport("abc"), AbcOperator())
        engine.registerFunction(namespace.internAndExport("def"), TestFunction())
        val output = StringBuilderOutput()
        engine.standardOutput = output
        val result = engine.parseAndEval(StringSourceLocation(src))
        return Pair(result, output.buf.toString())
    }

    class AbcOperator : APLOperatorOneArg {
        override fun combineFunction(fn: APLFunction, operatorAxis: Instruction?, pos: Position): APLFunctionDescriptor {
            return AbcFunctionDescriptor(fn, operatorAxis)
        }
    }

    class AbcFunctionDescriptor(val fn: APLFunction, val opAxis: Instruction?) : APLFunctionDescriptor {
        inner class AbcFunctionDescriptorImpl(pos: Position) : NoAxisAPLFunction(pos) {
            override fun eval1Arg(context: RuntimeContext, a: APLValue): APLValue {
                val result = fn.eval1Arg(context, a, null)
                val axisLong = if(opAxis == null) 0 else opAxis.evalWithContext(context).ensureNumber(pos).asLong(pos)
                return (result.ensureNumber(pos).asLong(pos) * 1000 + axisLong * 1000000).makeAPLNumber()
            }

            override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
                val result = fn.eval2Arg(context, a, b, null)
                val axisLong = if(opAxis == null) 0 else opAxis.evalWithContext(context).ensureNumber(pos).asLong(pos)
                return (result.ensureNumber(pos).asLong(pos) * 1000 + axisLong * 1000000).makeAPLNumber()
            }
        }

        override fun make(pos: Position) = AbcFunctionDescriptorImpl(pos)
    }

    class TestFunction : APLFunctionDescriptor {
        class TestFunctionImpl(pos: Position) : APLFunction(pos) {
            override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
                val aLong = a.ensureNumber(pos).asLong(pos)
                val axisLong = if(axis == null) 0 else axis.ensureNumber(pos).asLong(pos)
                return (aLong * 10 + axisLong * 100).makeAPLNumber()
            }

            override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                val aLong = a.ensureNumber(pos).asLong(pos)
                val bLong = b.ensureNumber(pos).asLong(pos)
                val axisLong = if(axis == null) 0 else axis.ensureNumber(pos).asLong(pos)
                return (aLong + bLong * 10 + axisLong * 100).makeAPLNumber()
            }
        }

        override fun make(pos: Position) = TestFunctionImpl(pos)
    }
}
