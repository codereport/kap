package array

import array.complex.Complex
import com.dhsdevelopments.mpbignum.Rational
import com.dhsdevelopments.mpbignum.make
import kotlin.test.Test
import kotlin.test.assertFailsWith

class InverseFnTest : APLTest() {
    @Test
    fun negateInverse() {
        assertSimpleNumber(-10, parseAPLExpression("(-inverse) 10"))
    }

    @Test
    fun subInverse0() {
        assertSimpleNumber(1, parseAPLExpression("7 -inverse 6"))
    }

    @Test
    fun subInverse1() {
        assertSimpleNumber(15, parseAPLExpression("8 -⍨inverse 7"))
    }

    @Test
    fun subInverse2() {
        assertSimpleNumber(15, parseAPLExpression("(8 -⍨inverse) 7"))
    }

    @Test
    fun subInverse3() {
        parseAPLExpression("10 200 -[0]˝ 2 3 ⍴ ⍳6").let { result ->
            assertDimension(dimensionsOfSize(2, 3), result)
            assertArrayContent(arrayOf(10, 9, 8, 197, 196, 195), result)
        }
    }

    @Test
    fun subInverse4() {
        parseAPLExpression("(1-⍨˝) 6").let { result ->
            assertSimpleNumber(7, result)
        }
    }

    @Test
    fun reciprocalInverse() {
        assertRational(Rational.make(1, 8), parseAPLExpression("÷inverse 8"))
    }

    @Test
    fun divInverse0() {
        assertSimpleNumber(32, parseAPLExpression("128 ÷inverse 4"))
    }

    @Test
    fun divInverse1() {
        assertSimpleNumber(8192, parseAPLExpression("4 ÷⍨inverse 2048"))
    }

    @Test
    fun mulInverse0() {
        assertSimpleNumber(5, parseAPLExpression("2 ×inverse 10"))
    }

    @Test
    fun mulInverse1() {
        assertRational(Rational.make(1, 5), parseAPLExpression("10 ×⍨inverse 2"))
    }

    @Test
    fun mulInverseMonadicFails() {
        assertFailsWith<InverseNotAvailable> {
            parseAPLExpression("×inverse 5")
        }
    }

    @Test
    fun addInverse1Arg0() {
        assertSimpleNumber(10, parseAPLExpression("+inverse 10"))
    }

    @Test
    fun addInverse1Arg1() {
        assertSimpleComplex(Complex(4.0, -9.0), parseAPLExpression("+inverse 4J9"))
    }

    @Test
    fun addInverseWithSwap() {
        assertSimpleNumber(7, parseAPLExpression("1 +⍨inverse 8"))
    }

    @Test
    fun logInverse1ArgWithArray() {
        parseAPLExpression("⍟inverse 2 3 ⍴ 2+⍳6").let { result ->
            assertDimension(dimensionsOfSize(2, 3), result)
            assertArrayContent(
                arrayOf(
                    NearDouble(7.389056099, 4),
                    NearDouble(20.08553692, 4),
                    NearDouble(54.59815003, 4),
                    NearDouble(148.4131591, 4),
                    NearDouble(403.4287935, 4),
                    NearDouble(1096.633158, 4)), result)
        }
    }

    @Test
    fun addInverse2ArgWithArray() {
        parseAPLExpression("2 +inverse 2 3 ⍴ 100+⍳6").let { result ->
            assertDimension(dimensionsOfSize(2, 3), result)
            assertArrayContent(arrayOf(98, 99, 100, 101, 102, 103), result)
        }
    }

    @Test
    fun leftAssignedInverse0() {
        assertSimpleNumber(7, parseAPLExpression("((1+)inverse) 8"))
    }

    @Test
    fun leftAssignedInverse1() {
        assertSimpleNumber(7, parseAPLExpression("1 +inverse 8"))
    }

    @Test
    fun leftAssignedInverseTwoArgFails() {
        assertFailsWith<LeftAssigned2ArgException> {
            parseAPLExpression("7 ((1+)inverse) 8")
        }
    }

    @Test
    fun inverseWithMonadicChain0() {
        assertSimpleNumber(50, parseAPLExpression("(2+200÷)inverse 6"))
    }

    @Test
    fun inverseWithMonadicChain1() {
        assertSimpleNumber(50, parseAPLExpression("((2+)(200÷))inverse 6"))
    }

    @Test
    fun inverseWithMonadicChain2() {
        assertRational(Rational.make(1, 8), parseAPLExpression("((2+)÷)inverse 10"))
    }

    @Test
    fun inverseWithDyadicChain0() {
        assertSimpleDouble(25.0, parseAPLExpression("15 (÷+)inverse 0.025"))
    }

    @Test
    fun inverseExponential0() {
        assertAPLValue(NearDouble(9.0, 4), parseAPLExpression("2 *inverse 512"))
    }

    @Test
    fun inverseExponential1() {
        assertAPLValue(NearDouble(1.609437912, 8), parseAPLExpression("*inverse 5"))
    }

    @Test
    fun inverseLog0() {
        assertAPLValue(InnerBigIntOrLong(1024), parseAPLExpression("2 ⍟inverse 10"))
    }

    @Test
    fun inverseLog1() {
        assertRational(Rational.make(1, 8), parseAPLExpression("2 ⍟inverse ¯3"))
    }

    @Test
    fun inverseCompose() {
        assertAPLValue(NearDouble(-0.001, 8), parseAPLExpression("8 ÷∘-˝ 8000"))
    }

    @Test
    fun inverseCompose1() {
        // TODO: This result gives incorrect result in Dyalog due to a Dyalog bug. This should be the correct value.
        assertAPLValue(NearDouble(10.125, 8), parseAPLExpression("8 -∘÷⍨˝ 10"))
    }

    @Test
    fun inverseLeftReverseCompose0() {
        assertSimpleDouble(40.0, parseAPLExpression("5 -⍛÷˝ ¯0.125"))
    }

    @Test
    fun inverseLeftReverseCompose1() {
        assertSimpleDouble(2.0, parseAPLExpression("8 -⍛÷⍨˝ ¯0.25"))
    }

    @Test
    fun inverseWithMonadicComposeShouldFail() {
        assertFailsWith<InverseNotAvailable> {
            parseAPLExpression("(+∘-)inverse 1")
        }
    }

    @Test
    fun inverseLeftReverseComposeFailsWithMonadic() {
        assertFailsWith<InverseNotAvailable> {
            parseAPLExpression("-⍛÷˝ ¯20")
        }
    }

    @Test
    fun inverseOnTakeAndDrop() {
        parseAPLExpression("(100+)⍢(3↑5↓) ⍳10").let { result ->
            assert1DArray(arrayOf(0, 1, 2, 3, 4, 105, 106, 107, 8, 9), result)
        }
    }

    @Test
    fun inverseOnTakeAndDropInsertZeroSize() {
        parseAPLExpression("{,100}⍢(0↑5↓) ⍳10").let { result ->
            assert1DArray(arrayOf(0, 1, 2, 3, 4, 100, 5, 6, 7, 8, 9), result)
        }
    }

    @Test
    fun inverseTakeZero() {
        parseAPLExpression("{,100}⍢(0↑) ⍳10").let { result ->
            assert1DArray(arrayOf(100, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9), result)
        }
    }

    @Test
    fun inverseTakeMoreThanSizeOfList() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("{,100}⍢(6↑) ⍳3")
        }
    }

    @Test
    fun inverseWhereTest0() {
        parseAPLExpression("⍸˝ 4 5").let { result ->
            assert1DArray(arrayOf(0, 0, 0, 0, 1, 1), result)
        }
    }

    @Test
    fun inverseWhereTest1() {
        parseAPLExpression("⍸˝ 2 2 4").let { result ->
            assert1DArray(arrayOf(0, 0, 2, 0, 1), result)
        }
    }

    @Test
    fun inverseWhereTest2() {
        assertFailsWith<APLIncompatibleDomainsException> {
            parseAPLExpression("⍸˝ 2 1")
        }
    }

    @Test
    fun inverseWhereTest3() {
        assertFailsWith<APLIncompatibleDomainsException> {
            parseAPLExpression("⍸˝ ¯1 2")
        }
    }

    @Test
    fun inverseWhereTest4() {
        assertFailsWith<APLIncompatibleDomainsException> {
            parseAPLExpression("⍸˝ 5")
        }
    }

    @Test
    fun inverseWhereTest5() {
        parseAPLExpression("⍸˝ (1 2) (2 3)").let { result ->
            assertDimension(dimensionsOfSize(3, 4), result)
            assertArrayContent(arrayOf(0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1), result)
        }
    }

    @Test
    fun inverseWhereTest6() {
        assertFailsWith<APLIncompatibleDomainsException> {
            parseAPLExpression("⍸˝ (1 2) (0 3)")
        }
    }

    @Test
    fun inverseWhereTest7() {
        assertFailsWith<APLIncompatibleDomainsException> {
            parseAPLExpression("⍸˝ (1 2) (1 0)")
        }
    }

    @Test
    fun inverseWhereTest8() {
        parseAPLExpression("⍸˝ (1 2) (1 2) (5 2)").let { result ->
            assertDimension(dimensionsOfSize(6, 3), result)
            assertArrayContent(arrayOf(0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1), result)
        }
    }

    @Test
    fun inverseWhereTest9() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("⍸˝ (1 2) 1")
        }
    }

    @Test
    fun inverseWhereTest10() {
        assertFailsWith<APLIncompatibleDomainsException> {
            parseAPLExpression("⍸˝ 2 2⍴1 2 3 4")
        }
    }

    @Test
    fun inverseWhereTest11() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("⍸˝ (2 2⍴1 0 0 0)(2 2⍴1 0 0 0)")
        }
    }

    @Test
    fun inverseWhereTest12() {
        assertAPLNull(parseAPLExpression("⍸˝ ⍬"))
    }
}
