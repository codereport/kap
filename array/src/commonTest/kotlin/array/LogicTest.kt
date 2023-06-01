package array

import kotlin.test.Test
import kotlin.test.assertFailsWith

class LogicTest : APLTest() {
    @Test
    fun andTest() {
        assertSimpleNumber(0, parseAPLExpression("0∧0"))
        assertSimpleNumber(0, parseAPLExpression("0∧1"))
        assertSimpleNumber(0, parseAPLExpression("1∧0"))
        assertSimpleNumber(1, parseAPLExpression("1∧1"))
    }

    @Test
    fun andFailsWithInvalidValue() {
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("0∧2") }
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("2∧0") }
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("¯1∧0") }
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("0∧¯1") }
        assertFailsWith<IncompatibleTypeException> { parseAPLExpression("0∧@a") }
        assertFailsWith<IncompatibleTypeException> { parseAPLExpression("@b∧1") }
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("0∧1j1") }
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("0j1∧1") }
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("0∧0.1") }
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("0.7∧0") }
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("0∧(int:asBigint 2)") }
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("(int:asBigint 2)∧0") }
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("0∧(1÷3)") }
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("(1÷3)∧0") }
    }

    @Test
    fun andTestWithArray() {
        parseAPLExpression("1 1 0 0 ∧ 0 1 1 0").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(0, 1, 0, 0), result)
        }
    }

    @Test
    fun andTestWithArrayBigint() {
        val src =
            """
            |(int:asBigint 1) (int:asBigint 1) (int:asBigint 0) (int:asBigint 0) ∧ `
            |           (int:asBigint 0) (int:asBigint 1) (int:asBigint 1) (int:asBigint 0)
            |""".trimMargin()
        parseAPLExpression(src).let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(InnerBigIntOrLong(0), InnerBigIntOrLong(1), InnerBigIntOrLong(0), InnerBigIntOrLong(0)), result)
        }
    }

    @Test
    fun andDouble() {
        parseAPLExpression("1.0 1.0 0.0 0.0 ∧ 1.0 0.0 1.0 0.0").let { result ->
            assert1DArray(arrayOf(InnerDouble(1.0), InnerDouble(0.0), InnerDouble(0.0), InnerDouble(0.0)), result)
        }
    }

    @Test
    fun orTest() {
        assertSimpleNumber(0, parseAPLExpression("0∨0"))
        assertSimpleNumber(1, parseAPLExpression("0∨1"))
        assertSimpleNumber(1, parseAPLExpression("1∨0"))
        assertSimpleNumber(1, parseAPLExpression("1∨1"))
    }

    @Test
    fun orFailsWithInvalidValue() {
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("0∨2") }
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("2∨0") }
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("¯1∨0") }
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("0∨¯1") }
        assertFailsWith<IncompatibleTypeException> { parseAPLExpression("0∨@a") }
        assertFailsWith<IncompatibleTypeException> { parseAPLExpression("@b∨1") }
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("0∨1j1") }
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("0j1∨1") }
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("0∨0.1") }
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("0.7∨0") }
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("0∨(int:asBigint 2)") }
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("(int:asBigint 2)∨0") }
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("0∨(1÷3)") }
        assertFailsWith<APLIllegalArgumentException> { parseAPLExpression("(1÷3)∨0") }
    }

    @Test
    fun orDouble() {
        parseAPLExpression("1.0 1.0 0.0 0.0 ∨ 1.0 0.0 1.0 0.0").let { result ->
            assert1DArray(arrayOf(InnerDouble(1.0), InnerDouble(1.0), InnerDouble(1.0), InnerDouble(0.0)), result)
        }
    }

    @Test
    fun orTestBigints() {
        assertBigIntOrLong(0, parseAPLExpression("(int:asBigint 0)∨(int:asBigint 0)"))
        assertBigIntOrLong(1, parseAPLExpression("(int:asBigint 0)∨(int:asBigint 1)"))
        assertBigIntOrLong(1, parseAPLExpression("(int:asBigint 1)∨(int:asBigint 0)"))
        assertBigIntOrLong(1, parseAPLExpression("(int:asBigint 1)∨(int:asBigint 1)"))
    }

    @Test
    fun orTestWithArray() {
        parseAPLExpression("1 1 0 0 ∨ 0 1 1 0").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1, 1, 1, 0), result)
        }
    }

    @Test
    fun testNotWorking() {
        parseAPLExpression("~0 1").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(1, 0), result)
        }
    }

    @Test
    fun testNotFailing() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("~10")
        }
    }

    //////////////////////////////
    // Tests for without
    //////////////////////////////

    @Test
    fun simpleWithout() {
        parseAPLExpression("(⍳19) ~ 1 4 5 10").let { result ->
            assertDimension(dimensionsOfSize(15), result)
            assertArrayContent(arrayOf(0, 2, 3, 6, 7, 8, 9, 11, 12, 13, 14, 15, 16, 17, 18), result)
        }
    }

    @Test
    fun removeNoElements() {
        parseAPLExpression("(⍳3) ~ 10 11").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(0, 1, 2), result)
        }
    }

    @Test
    fun removeOneElement() {
        parseAPLExpression("(⍳3) ~ 2").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(0, 1), result)
        }
    }

    @Test
    fun removeMultiDimension() {
        parseAPLExpression("(⍳12) ~ 2 2 ⍴ 0 3 10 11").let { result ->
            assertDimension(dimensionsOfSize(8), result)
            assertArrayContent(arrayOf(1, 2, 4, 5, 6, 7, 8, 9), result)
        }
    }

    @Test
    fun removeFromMultiDimensionShouldFail() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(2 4 ⍴ ⍳4) ~ 1 2")
        }
    }

    @Test
    fun removeComplexElement() {
        parseAPLExpression("((2 2 ⍴ 0 1 2 3) (2 2 ⍴ 3 4 5 6)) ~ ⊂(2 2 ⍴ 0 1 2 3)").let { result ->
            assertDimension(dimensionsOfSize(1), result)
            val v = result.valueAt(0)
            assertDimension(dimensionsOfSize(2, 2), v)
            assertArrayContent(arrayOf(3, 4, 5, 6), v)
        }
    }

    @Test
    fun removeFromScalarNoMatch() {
        parseAPLExpression("2 ~ 1").let { result ->
            assertDimension(dimensionsOfSize(1), result)
            assertArrayContent(arrayOf(2), result)
        }
    }

    @Test
    fun removeFromScalarMatch() {
        assertAPLNull(parseAPLExpression("2 ~ 2"))
    }

    @Test
    fun nandTest() {
        assertSimpleNumber(1, parseAPLExpression("0⍲0"))
        assertSimpleNumber(1, parseAPLExpression("0⍲1"))
        assertSimpleNumber(1, parseAPLExpression("1⍲0"))
        assertSimpleNumber(0, parseAPLExpression("1⍲1"))
    }

    @Test
    fun errorWithNandIllegalArgument() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("3⍲0")
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("0⍲3")
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("¯1⍲1")
        }
    }

    @Test
    fun nandArrayRightArgument() {
        parseAPLExpression("1 ⍲ 0 1 1 0").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1, 0, 0, 1), result)
        }
        parseAPLExpression("0 ⍲ 0 1 1 0").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1, 1, 1, 1), result)
        }
    }

    @Test
    fun nandArrayLeftArgument() {
        parseAPLExpression("1 1 0 0 ⍲ 1").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(0, 0, 1, 1), result)
        }
        parseAPLExpression("1 1 0 0 ⍲ 0").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1, 1, 1, 1), result)
        }
    }


    @Test
    fun norTest() {
        assertSimpleNumber(1, parseAPLExpression("0⍱0"))
        assertSimpleNumber(0, parseAPLExpression("0⍱1"))
        assertSimpleNumber(0, parseAPLExpression("1⍱0"))
        assertSimpleNumber(0, parseAPLExpression("1⍱1"))
    }

    @Test
    fun norArrayRightArgument() {
        parseAPLExpression("1 ⍱ 0 1 1 0").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(0, 0, 0, 0), result)
        }
        parseAPLExpression("0 ⍱ 0 1 1 0").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(1, 0, 0, 1), result)
        }
    }

    @Test
    fun norArrayLeftArgument() {
        parseAPLExpression("1 1 0 0 ⍱ 1").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(0, 0, 0, 0), result)
        }
        parseAPLExpression("1 1 0 0 ⍱ 0").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(0, 0, 1, 1), result)
        }
    }

    @Test
    fun errorWithNorIllegalArgument() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("3⍱0")
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("0⍱3")
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("¯1⍱0")
        }
    }

    @Test
    fun nandBigint() {
        parseAPLExpression("(int:asBigint 1) ⍲ (int:asBigint 0)").let { result ->
            assertSimpleNumber(1, result)
        }
    }

    @Test
    fun norBigint() {
        parseAPLExpression("(int:asBigint 1) ⍱ (int:asBigint 1)").let { result ->
            assertSimpleNumber(0, result)
        }
    }
}
