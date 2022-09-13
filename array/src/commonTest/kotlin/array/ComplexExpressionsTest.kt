package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ComplexExpressionsTest : APLTest() {
    @Test
    fun parenExpressionWithScalarValue() {
        val result = parseAPLExpression("(1+2)")
        assertSimpleNumber(3, result)
    }

    @Test
    fun nestedArrayNoExpression() {
        val result = parseAPLExpression("(1 2) (3 4)")
        assertDimension(dimensionsOfSize(2), result)
        assertArrayContent(arrayOf(1, 2), result.valueAt(0))
        assertArrayContent(arrayOf(3, 4), result.valueAt(1))
    }

    @Test
    fun nestedArrayScalarValue() {
        val result = parseAPLExpression("(1) (2 3)")
        assertDimension(dimensionsOfSize(2), result)
        assertSimpleNumber(1, result.valueAt(0))
        assertArrayContent(arrayOf(2, 3), result.valueAt(1))
    }

    @Test
    fun nestedArrayWithScalarValueFromFn() {
        val result = parseAPLExpression("∇ foo (x) {1+x} ◊ (foo 1) (foo 6)")
        assertDimension(dimensionsOfSize(2), result)
        assertSimpleNumber(2, result.valueAt(0))
        assertSimpleNumber(7, result.valueAt(1))
    }

    @Test
    fun nestedArrayWithScalarValueFromExpr() {
        val result = parseAPLExpression("(1+2) (3+4) (1+5)")
        assertDimension(dimensionsOfSize(3), result)
        assertSimpleNumber(3, result.valueAt(0))
        assertSimpleNumber(7, result.valueAt(1))
        assertSimpleNumber(6, result.valueAt(2))
    }

    @Test
    fun doubleNestedArrays() {
        val result = parseAPLExpression("(⍳3) (10+⍳10)")
        assertDimension(dimensionsOfSize(2), result)
        result.valueAt(0).let { value ->
            assertDimension(dimensionsOfSize(3), value)
            assertArrayContent(arrayOf(0, 1, 2), value)
        }
        result.valueAt(1).let { value ->
            assertDimension(dimensionsOfSize(10), value)
            assertArrayContent(arrayOf(10, 11, 12, 13, 14, 15, 16, 17, 18, 19), value)
        }
    }

    @Test
    fun closeParenMissing() {
        assertFailsWith<ParseException> {
            parseAPLExpression("(1+2+3")
        }
    }

    @Test
    fun openParenMissing() {
        assertFailsWith<ParseException> {
            parseAPLExpression("1+2+3)")
        }
    }

    @Test
    fun closeBracketMissing() {
        assertFailsWith<ParseException> {
            parseAPLExpression("1 2 4 5 6 7 +/[")
        }
    }

    @Test
    fun openBracketMissing() {
        assertFailsWith<ParseException> {
            parseAPLExpression("1 2 3 4 5 6 7 +/2] 1")
        }
    }

    @Test
    fun closeBraceMissing() {
        assertFailsWith<ParseException> {
            parseAPLExpression("{1+2+3")
        }
    }

    @Test
    fun openBraceMissing() {
        assertFailsWith<ParseException> {
            parseAPLExpression("1+2+3}")
        }
    }

    @Test
    fun incorrectlyNestedParens1() {
        assertFailsWith<ParseException> {
            parseAPLExpression("(1+2 {3+4)}")
        }
    }

    @Test
    fun incorrectlyNestedParens2() {
        assertFailsWith<ParseException> {
            parseAPLExpression("{1+2 (3+4} 5 6 7)")
        }
    }

    @Test
    fun nestedFunctions() {
        val result = parseAPLExpression("{⍵+{1+⍵} 4} 5")
        assertSimpleNumber(10, result)
    }

    @Test
    fun nestedTwoArgFunctions() {
        val result = parseAPLExpression("200 {⍺+⍵+10 {1+⍺+⍵} 4} 5 ")
        assertSimpleNumber(220, result)
    }

    @Test
    fun multilineExpression() {
        parseAPLExpressionWithOutput(
            """
            |io:print 3
            |2
        """.trimMargin()).let { (result, output) ->
            assertSimpleNumber(2, result)
            assertEquals("3", output)
        }
    }

    @Test
    fun multilineExpressionWithBlankLines() {
        parseAPLExpressionWithOutput(
            """
            |io:print 3
            |
            |2
            |
        """.trimMargin()).let { (result, output) ->
            assertSimpleNumber(2, result)
            assertEquals("3", output)
        }
    }

    @Test
    fun functionInParens() {
        parseAPLExpression("8 16 32 (÷) 2").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(4, 8, 16), result)
        }
    }

    @Test
    fun functionAndOperatorInParen() {
        parseAPLExpression("2 (↑¨) (1 2 3 4) (4 5 6 7)").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(1, 2), result.valueAt(0))
            assertArrayContent(arrayOf(4, 5), result.valueAt(1))
        }
    }

    @Test
    fun functionInParenWithOperator() {
        parseAPLExpression("(⊂)¨ (0 1 2) (3 4 5)").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            result.valueAt(0).let { v ->
                assertTrue(v.isScalar())
                assertArrayContent(arrayOf(0, 1, 2), v.valueAt(0))
            }
            result.valueAt(1).let { v ->
                assertTrue(v.isScalar())
                assertArrayContent(arrayOf(3, 4, 5), v.valueAt(0))
            }
        }
    }

    @Test
    fun operatorInParenShouldFail() {
        // TODO: This should probably be a parse error. Being able to have a variable with the same name as an operator can be confusing.
        assertFailsWith<VariableNotAssigned> {
            parseAPLExpression("1 2 3 +(¨) 4 5 6")
        }
    }

    @Test
    fun functionInParensLeftArg() {
        parseAPLExpression("(2+) 3 4 5").let { result ->
            assert1DArray(arrayOf(5, 6, 7), result)
        }
    }

    @Test
    fun noContentInParenExpression() {
        assertFailsWith<ParseException> {
            parseAPLExpression("1 () 3")
        }
    }

    @Test
    fun twoFunctionCalls() {
        parseAPLExpression("1 + 2 + 3").let { result ->
            assertSimpleNumber(6, result)
        }
    }

    @Test
    fun forEachWithEnclose() {
        parseAPLExpression("(-⍳5) ⌽¨ ⊂10 20 30 40 50").let { result ->
            assertDimension(dimensionsOfSize(5), result)
            assertArrayContent(arrayOf(10, 20, 30, 40, 50), result.valueAt(0))
            assertArrayContent(arrayOf(50, 10, 20, 30, 40), result.valueAt(1))
            assertArrayContent(arrayOf(40, 50, 10, 20, 30), result.valueAt(2))
            assertArrayContent(arrayOf(30, 40, 50, 10, 20), result.valueAt(3))
            assertArrayContent(arrayOf(20, 30, 40, 50, 10), result.valueAt(4))
        }
    }

    @Test
    fun findWithSelect() {
        parseAPLExpression("\"abcabc\" (⊣(⫽⍨)∊) @c").let { result ->
            assertString("cc", result)
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

    @Test
    fun testAxis5() {
        val src =
            """
            |a ⇐ def[io:print 2]
            |b ⇐ a abc[io:print 3]
            |io:print 6
            |b 1
            """.trimMargin()
        val (result, out) = evalWithDebugFunctionsOutput(src)
        assertSimpleNumber(301210, result)
        assertEquals("236", out)
    }

    @Test
    fun testAxis6() {
        val src =
            """
            |a ⇐ def
            |b ⇐ a[io:print 2] abc[io:print 3]
            |io:print 6
            |b 1
            """.trimMargin()
        val (result, out) = evalWithDebugFunctionsOutput(src)
        assertSimpleNumber(301210, result)
        assertEquals("326", out)
    }

    @Test
    fun testAxis7() {
        val src =
            """
            |a ⇐ def[2] abc[3]
            |a[4] 1
            """.trimMargin()
        assertFailsWith<AxisNotSupported> {
            evalWithDebugFunctions(src)
        }
    }

    @Test
    fun testAxis8() {
        val src =
            """
            |a ⇐ def[2]
            |a[4] 1
            """.trimMargin()
        assertFailsWith<AxisNotSupported> {
            evalWithDebugFunctions(src)
        }
    }

    @Test
    fun testAxis9() {
        val src =
            """
            |a ⇐ def abc
            |a[4] 1
            """.trimMargin()
        assertFailsWith<AxisNotSupported> {
            evalWithDebugFunctions(src)
        }
    }

    @Test
    fun testAxis10() {
        val src =
            """
            |a ⇐ def[io:print 1] def[io:print 2]
            |io:print 6
            |a 4
            """.trimMargin()
        val (result, out) = evalWithDebugFunctionsOutput(src)
        assertSimpleNumber(380, result)
        assertEquals("216", out)
    }

    @Test
    fun testAxis11() {
        val src =
            """
            |a ⇐ def[io:print 1] - def[io:print 2]
            |io:print 6
            |a 4
            """.trimMargin()
        val (result, out) = evalWithDebugFunctionsOutput(src)
        assertSimpleNumber(-100, result)
        assertEquals("216", out)
    }

    @Test
    fun testLeftBindAssigned0() {
        val src =
            """
            |a ⇐ (io:print 1)+
            |io:print 2
            |a + 5
            """.trimMargin()
        val (result, out) = evalWithDebugFunctionsOutput(src)
        assertSimpleNumber(6, result)
        assertEquals("12", out)
    }

    @Test
    fun testLeftBindAssigned1() {
        val src =
            """
            |a ⇐ ((io:print 10) (io:print 100))+[io:print 0]
            |io:print 2
            |a 2 3 ⍴ ⍳6
            """.trimMargin()
        val (result, out) = evalWithDebugFunctionsOutput(src)
        assertDimension(dimensionsOfSize(2, 3), result)
        assertArrayContent(arrayOf(10, 11, 12, 103, 104, 105), result)
        assertEquals("0100102", out)
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
        val result = engine.parseAndEval(StringSourceLocation(src)).collapse()
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
                val axisLong = if (opAxis == null) 0 else opAxis.evalWithContext(context).ensureNumber(pos).asLong(pos)
                return (result.ensureNumber(pos).asLong(pos) * 1000 + axisLong * 1000000).makeAPLNumber()
            }

            override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue): APLValue {
                val result = fn.eval2Arg(context, a, b, null)
                val axisLong = if (opAxis == null) 0 else opAxis.evalWithContext(context).ensureNumber(pos).asLong(pos)
                return (result.ensureNumber(pos).asLong(pos) * 1000 + axisLong * 1000000).makeAPLNumber()
            }
        }

        override fun make(pos: Position) = AbcFunctionDescriptorImpl(pos)
    }

    class TestFunction : APLFunctionDescriptor {
        class TestFunctionImpl(pos: Position) : APLFunction(pos) {
            override fun eval1Arg(context: RuntimeContext, a: APLValue, axis: APLValue?): APLValue {
                val aLong = a.ensureNumber(pos).asLong(pos)
                val axisLong = if (axis == null) 0 else axis.ensureNumber(pos).asLong(pos)
                return (aLong * 10 + axisLong * 100).makeAPLNumber()
            }

            override fun eval2Arg(context: RuntimeContext, a: APLValue, b: APLValue, axis: APLValue?): APLValue {
                val aLong = a.ensureNumber(pos).asLong(pos)
                val bLong = b.ensureNumber(pos).asLong(pos)
                val axisLong = if (axis == null) 0 else axis.ensureNumber(pos).asLong(pos)
                return (aLong + bLong * 10 + axisLong * 100).makeAPLNumber()
            }
        }

        override fun make(pos: Position) = TestFunctionImpl(pos)
    }
}
