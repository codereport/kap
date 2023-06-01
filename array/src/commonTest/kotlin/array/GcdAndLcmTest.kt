package array

import array.complex.Complex
import com.dhsdevelopments.mpbignum.Rational
import com.dhsdevelopments.mpbignum.make
import kotlin.test.Test

class GcdAndLcmTest : APLTest() {
    @Test
    fun leastCommonMultipleIntegers() {
        assertSimpleNumber(0, parseAPLExpression("0 math:lcm 0"))
        assertSimpleNumber(6, parseAPLExpression("2 math:lcm 3"))
        assertSimpleNumber(6, parseAPLExpression("2 math:lcm 3"))
        assertSimpleNumber(12, parseAPLExpression("4 math:lcm 6"))
        assertSimpleNumber(-6, parseAPLExpression("2 math:lcm ¯3"))
        assertSimpleNumber(-6, parseAPLExpression("¯2 math:lcm 3"))
        assertSimpleNumber(6, parseAPLExpression("¯2 math:lcm ¯3"))
    }

    @Test
    fun leastCommonMultipleBigints() {
        assertBigIntOrLong(0, parseAPLExpression("(int:asBigint 0) math:lcm (int:asBigint 0)"))
        assertBigIntOrLong(6, parseAPLExpression("(int:asBigint 2) math:lcm (int:asBigint 3)"))
        assertBigIntOrLong(6, parseAPLExpression("(int:asBigint 2) math:lcm (int:asBigint 3)"))
        assertBigIntOrLong(12, parseAPLExpression("(int:asBigint 4) math:lcm (int:asBigint 6)"))
        assertBigIntOrLong(-6, parseAPLExpression("(int:asBigint 2) math:lcm (int:asBigint ¯3)"))
        assertBigIntOrLong(-6, parseAPLExpression("(int:asBigint ¯2) math:lcm (int:asBigint 3)"))
        assertBigIntOrLong(6, parseAPLExpression("(int:asBigint ¯2) math:lcm (int:asBigint ¯3)"))
    }

    @Test
    fun leastCommonMultipleDouble() {
        assertNearDouble(NearDouble(22.8), parseAPLExpression("1.2 math:lcm 3.8"))
        assertNearDouble(NearDouble(2.0), parseAPLExpression("1 math:lcm 2.0÷3"))
    }

    @Test
    fun leastCommonMultipleComplex() {
        assertSimpleComplex(Complex(123.0, 192.0), parseAPLExpression("6J21 math:lcm 9J30"))
        assertSimpleComplex(Complex(38.0, 43.0), parseAPLExpression("5J8 math:lcm 1J6"))
        assertSimpleComplex(Complex(495.0, -312.0), parseAPLExpression("9J30 math:lcm 5J18"))
        assertSimpleComplex(Complex(-5.0, -14.0), parseAPLExpression("2J3 math:lcm 4J1"))
        assertSimpleComplex(Complex(25.0, -19.0), parseAPLExpression("5J3 math:lcm 5J2"))
        assertSimpleComplex(Complex(-31.0, 5.0), parseAPLExpression("¯5J3 math:lcm 5J2"))
        assertSimpleComplex(Complex(-141.0, 75.0), parseAPLExpression("9J30 math:lcm 1J5"))
        assertSimpleComplex(Complex(-6.0, 18.0), parseAPLExpression("3J6 math:lcm 2J2"))
        assertSimpleComplex(Complex(4.0, 18.0), parseAPLExpression("3J5 math:lcm 4J18"))
    }

    @Test
    fun leastCommonMultipleRational() {
        assertRational(Rational.make(535, 2), parseAPLExpression("(10÷8) math:lcm (107÷10)"))
    }

    @Test
    fun greatestCommonDenominatorIntegers() {
        assertSimpleNumber(1, parseAPLExpression("3 math:gcd 8"))
        assertSimpleNumber(4, parseAPLExpression("4 math:gcd 8"))
        assertSimpleNumber(4, parseAPLExpression("4 math:gcd 16"))
        assertSimpleNumber(2, parseAPLExpression("6 math:gcd 16"))
    }

    @Test
    fun greatestCommonDenominatorBigints() {
        assertBigIntOrLong(1, parseAPLExpression("(int:asBigint 3) math:gcd (int:asBigint 8)"))
        assertBigIntOrLong(4, parseAPLExpression("(int:asBigint 4) math:gcd (int:asBigint 8)"))
        assertBigIntOrLong(4, parseAPLExpression("(int:asBigint 4) math:gcd (int:asBigint 16)"))
        assertBigIntOrLong(2, parseAPLExpression("(int:asBigint 6) math:gcd (int:asBigint 16)"))
    }

    @Test
    fun greatestCommonDenominatorDouble() {
        assertNearDouble(NearDouble(0.2), parseAPLExpression("10.4 math:gcd 4.2"))
        assertNearDouble(NearDouble(0.2), parseAPLExpression("6.2 math:gcd 3.2"))
    }

    @Test
    fun greatestCommonDenominatorComplex() {
        assertSimpleComplex(Complex(6.0, 4.0), parseAPLExpression("6J4 math:gcd ¯10J54"))
        assertSimpleComplex(Complex(9.0, 2000.0), parseAPLExpression("9J2000 math:gcd ¯11973J6054"))
        // (⊂(⍕p) , " math:gcd " , ⍕z) , ((p←y×3J3)  math:gcd  z←(y←3J8)×16J2)
        assertSimpleComplex(Complex(11.0, 5.0), parseAPLExpression("¯15J33 math:gcd 32J134"))
    }

    @Test
    fun greatestCommonDenominatorRational() {
        assertRational(Rational.make(1, 30), parseAPLExpression("(31÷10) math:gcd (10÷6)"))
    }
}
