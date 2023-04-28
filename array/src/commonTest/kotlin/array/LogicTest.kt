package array

import array.complex.Complex
import com.dhsdevelopments.mpbignum.Rational
import com.dhsdevelopments.mpbignum.make
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
    fun andTestWithArray() {
        parseAPLExpression("1 1 0 0 ∧ 0 1 1 0").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(0, 1, 0, 0), result)
        }
    }

    @Test
    fun leastCommonMultipleIntegers() {
        assertSimpleNumber(0, parseAPLExpression("0∧0"))
        assertSimpleNumber(6, parseAPLExpression("2∧3"))
        assertSimpleNumber(6, parseAPLExpression("2∧3"))
        assertSimpleNumber(12, parseAPLExpression("4∧6"))
        assertSimpleNumber(-6, parseAPLExpression("2∧¯3"))
        assertSimpleNumber(-6, parseAPLExpression("¯2∧3"))
        assertSimpleNumber(6, parseAPLExpression("¯2∧¯3"))
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
    fun leastCommonMultipleBigints() {
        assertBigIntOrLong(0, parseAPLExpression("(int:asBigint 0)∧(int:asBigint 0)"))
        assertBigIntOrLong(6, parseAPLExpression("(int:asBigint 2)∧(int:asBigint 3)"))
        assertBigIntOrLong(6, parseAPLExpression("(int:asBigint 2)∧(int:asBigint 3)"))
        assertBigIntOrLong(12, parseAPLExpression("(int:asBigint 4)∧(int:asBigint 6)"))
        assertBigIntOrLong(-6, parseAPLExpression("(int:asBigint 2)∧(int:asBigint ¯3)"))
        assertBigIntOrLong(-6, parseAPLExpression("(int:asBigint ¯2)∧(int:asBigint 3)"))
        assertBigIntOrLong(6, parseAPLExpression("(int:asBigint ¯2)∧(int:asBigint ¯3)"))
    }

    @Test
    fun leastCommonMultipleDouble() {
        assertNearDouble(NearDouble(22.8), parseAPLExpression("1.2∧3.8"))
        assertNearDouble(NearDouble(2.0), parseAPLExpression("1∧2.0÷3"))
    }

    @Test
    fun andDouble() {
        parseAPLExpression("1.0 1.0 0.0 0.0 ∧ 1.0 0.0 1.0 0.0").let { result ->
            assert1DArray(arrayOf(InnerDouble(1.0), InnerDouble(0.0), InnerDouble(0.0), InnerDouble(0.0)), result)
        }
    }

    @Test
    fun leastCommonMultipleComplex() {
        assertSimpleComplex(Complex(123.0, 192.0), parseAPLExpression("6J21∧9J30"))
        assertSimpleComplex(Complex(38.0, 43.0), parseAPLExpression("5J8∧1J6"))
        assertSimpleComplex(Complex(495.0, -312.0), parseAPLExpression("9J30∧5J18"))
        assertSimpleComplex(Complex(-5.0, -14.0), parseAPLExpression("2J3∧4J1"))
        assertSimpleComplex(Complex(25.0, -19.0), parseAPLExpression("5J3∧5J2"))
        assertSimpleComplex(Complex(-31.0, 5.0), parseAPLExpression("¯5J3∧5J2"))
        assertSimpleComplex(Complex(-141.0, 75.0), parseAPLExpression("9J30∧1J5"))
        assertSimpleComplex(Complex(-6.0, 18.0), parseAPLExpression("3J6∧2J2"))
        assertSimpleComplex(Complex(4.0, 18.0), parseAPLExpression("3J5∧4J18"))
    }

    @Test
    fun leastCommonMultipleRational() {
        assertRational(Rational.make(535, 2), parseAPLExpression("(10÷8)∧(107÷10)"))
    }

    @Test
    fun orTest() {
        assertSimpleNumber(0, parseAPLExpression("0∨0"))
        assertSimpleNumber(1, parseAPLExpression("0∨1"))
        assertSimpleNumber(1, parseAPLExpression("1∨0"))
        assertSimpleNumber(1, parseAPLExpression("1∨1"))
    }

    @Test
    fun orDouble() {
        parseAPLExpression("1.0 1.0 0.0 0.0 ∨ 1.0 0.0 1.0 0.0").let { result ->
            assert1DArray(arrayOf(InnerDouble(1.0), InnerDouble(1.0), InnerDouble(1.0), InnerDouble(0.0)), result)
        }
    }

    @Test
    fun greatestCommonDenominatorIntegers() {
        assertSimpleNumber(1, parseAPLExpression("3∨8"))
        assertSimpleNumber(4, parseAPLExpression("4∨8"))
        assertSimpleNumber(4, parseAPLExpression("4∨16"))
        assertSimpleNumber(2, parseAPLExpression("6∨16"))
    }

    @Test
    fun orTestBigints() {
        assertBigIntOrLong(0, parseAPLExpression("(int:asBigint 0)∨(int:asBigint 0)"))
        assertBigIntOrLong(1, parseAPLExpression("(int:asBigint 0)∨(int:asBigint 1)"))
        assertBigIntOrLong(1, parseAPLExpression("(int:asBigint 1)∨(int:asBigint 0)"))
        assertBigIntOrLong(1, parseAPLExpression("(int:asBigint 1)∨(int:asBigint 1)"))
    }

    @Test
    fun greatestCommonDenominatorBigints() {
        assertBigIntOrLong(1, parseAPLExpression("(int:asBigint 3)∨(int:asBigint 8)"))
        assertBigIntOrLong(4, parseAPLExpression("(int:asBigint 4)∨(int:asBigint 8)"))
        assertBigIntOrLong(4, parseAPLExpression("(int:asBigint 4)∨(int:asBigint 16)"))
        assertBigIntOrLong(2, parseAPLExpression("(int:asBigint 6)∨(int:asBigint 16)"))
    }

    @Test
    fun greatestCommonDenominatorDouble() {
        assertNearDouble(NearDouble(0.2), parseAPLExpression("10.4∨4.2"))
        assertNearDouble(NearDouble(0.2), parseAPLExpression("6.2∨3.2"))
    }

    @Test
    fun greatestCommonDenominatorComplex() {
        assertSimpleComplex(Complex(6.0, 4.0), parseAPLExpression("6J4∨¯10J54"))
        assertSimpleComplex(Complex(9.0, 2000.0), parseAPLExpression("9J2000∨¯11973J6054"))
        // (⊂(⍕p) , "∨" , ⍕z) , ((p←y×3J3) ∨ z←(y←3J8)×16J2)
        assertSimpleComplex(Complex(11.0, 5.0), parseAPLExpression("¯15J33∨32J134"))
    }

    @Test
    fun greatestCommonDenominatorRational() {
        assertRational(Rational.make(1, 30), parseAPLExpression("(31÷10)∨(10÷6)"))
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
