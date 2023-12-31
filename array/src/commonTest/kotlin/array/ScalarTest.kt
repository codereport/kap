package array

import array.complex.Complex
import com.dhsdevelopments.mpbignum.Rational
import com.dhsdevelopments.mpbignum.make
import kotlin.math.pow
import kotlin.math.sign
import kotlin.test.*

class ScalarTest : APLTest() {
    @Test
    fun testReshape() {
        val result = parseAPLExpression("3 4 ⍴ ⍳100")
        assertDimension(dimensionsOfSize(3, 4), result)
        assertArrayContent(arrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), result)
    }

    @Test
    fun testAdd() {
        runScalarTest("+") { a, b -> a + b }
    }

    @Test
    fun testSub() {
        runScalarTest("-") { a, b -> a - b }
    }

    @Test
    fun testMul() {
        runScalarTest("×") { a, b -> a * b }
    }

    @Test
    fun testDiv() {
        runScalarTest("÷") { a, b -> a / b }
    }

    @Test
    fun testPow() {
        runScalarTest("⋆") { a, b -> a.pow(b) }
    }

    @Test
    fun testAdd1Arg() {
        runScalarTest1Arg("+") { a -> a }
    }

    @Test
    fun testSub1Arg() {
        runScalarTest1Arg("-") { a -> -a }
    }

    @Test
    fun testMulArg() {
        runScalarTest1Arg("×") { a -> a.sign }
    }

    @Test
    fun testDivArg() {
        runScalarTest1Arg("÷") { a -> if (a == 0.0) 0.0 else 1 / a }
    }

    @Test
    fun testCompareEquals() {
        runScalarTest("=") { a, b -> if (a == b) 1.0 else 0.0 }
    }

    @Test
    fun testCompareNotEquals() {
        runScalarTest("≠") { a, b -> if (a != b) 1.0 else 0.0 }
    }

    @Test
    fun additionWithAxis0() {
        val result = parseAPLExpression("10 20 30 40 +[0] 4 3 2 ⍴ 100+⍳24")
        assertDimension(dimensionsOfSize(4, 3, 2), result)
        assertArrayContent(
            arrayOf(
                110, 111, 112, 113, 114, 115, 126, 127, 128, 129, 130, 131, 142, 143, 144, 145, 146, 147, 158, 159, 160, 161, 162, 163
            ),
            result)
    }

    @Test
    fun additionWithAxis1() {
        val result = parseAPLExpression("10 20 30 +[1] 4 3 2 ⍴ 100+⍳24")
        assertDimension(dimensionsOfSize(4, 3, 2), result)
        assertArrayContent(
            arrayOf(
                110, 111, 122, 123, 134, 135, 116, 117, 128, 129, 140, 141, 122, 123, 134, 135, 146, 147, 128, 129, 140, 141, 152, 153
            ),
            result)
    }

    @Test
    fun additionWithAxis2() {
        val result = parseAPLExpression("10 20 +[2] 4 3 2 ⍴ 100+⍳24")
        assertDimension(dimensionsOfSize(4, 3, 2), result)
        assertArrayContent(
            arrayOf(
                110, 121, 112, 123, 114, 125, 116, 127, 118, 129, 120, 131, 122, 133,
                124, 135, 126, 137, 128, 139, 130, 141, 132, 143),
            result)
    }

    @Test
    fun multiFunctionPlus() {
        parseAPLExpression("0 1 2 + 10 11 12 + 20 21 22").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(30, 33, 36), result)
        }
    }

    @Test
    fun multiFunctionPlusWithSpecialisedArray() {
        parseAPLExpression("(int:ensureLong 0 1 2) + (int:ensureLong 10 11 12) + (int:ensureLong 20 21 22)").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(30, 33, 36), result)
        }
    }

    @Test
    fun multiFunctionPlusAndMinus() {
        parseAPLExpression("20 21 22 - 0 1 2 + 10 11 12").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(10, 9, 8), result)
        }
    }

    @Test
    fun multiFunctionPlusAndMinusSpecialised() {
        parseAPLExpression("(int:ensureLong 20 21 22) - (int:ensureLong 0 1 2) + (int:ensureLong 10 11 12)").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(10, 9, 8), result)
        }
    }

    @Test
    fun additionWithDifferentTypes() {
        assertAPLValue(4, parseAPLExpression("2+2"))
        assertAPLValue(NearDouble(3.2), parseAPLExpression("2+1.2"))
        assertAPLValue(Rational.make(7, 3), parseAPLExpression("2+(1÷3)"))
        assertAPLValue(InnerBigIntOrLong("10000000000000000000000000000000002"), parseAPLExpression("2+10000000000000000000000000000000000"))
        assertAPLValue(NearDouble(3.2), parseAPLExpression("1.2+2"))
        assertAPLValue(NearDouble(2.4), parseAPLExpression("1.2+1.2"))
        assertAPLValue(NearDouble(1.53333333), parseAPLExpression("1.2+(1÷3)"))
        assertAPLValue(NearDouble(1.0e34, -30), parseAPLExpression("1.2+10000000000000000000000000000000000"))
        assertAPLValue(Rational.make(7, 3), parseAPLExpression("(1÷3)+2"))
        assertAPLValue(NearDouble(1.53333333), parseAPLExpression("(1÷3)+1.2"))
        assertAPLValue(Rational.make(2, 3), parseAPLExpression("(1÷3)+(1÷3)"))
        assertAPLValue(Rational.make("30000000000000000000000000000000001", "3"), parseAPLExpression("(1÷3)+10000000000000000000000000000000000"))
        assertAPLValue(InnerBigIntOrLong("10000000000000000000000000000000002"), parseAPLExpression("10000000000000000000000000000000000+2"))
        assertAPLValue(NearDouble(1.0e34, -30), parseAPLExpression("10000000000000000000000000000000000+1.2"))
        assertAPLValue(Rational.make("30000000000000000000000000000000001", "3"), parseAPLExpression("10000000000000000000000000000000000+(1÷3)"))
    }

    @Test
    fun failWithWrongRank() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("(2 3 ⍴ ⍳6) +[0] 2 3 4 ⍴ ⍳24")
        }
    }

    @Test
    fun testPowBigInt0() {
        parseAPLExpression("4*62").let { result ->
            assertBigIntOrLong("21267647932558653966460912964485513216", result)
        }
        parseAPLExpression("4*int:asBigint 62").let { result ->
            assertBigIntOrLong("21267647932558653966460912964485513216", result)
        }
    }

    @Test
    fun testPowBigInt1() {
        parseAPLExpression("¯4*61").let { result ->
            assertBigIntOrLong("-5316911983139663491615228241121378304", result)
        }
        parseAPLExpression("¯4*int:asBigint 61").let { result ->
            assertBigIntOrLong("-5316911983139663491615228241121378304", result)
        }
    }

    @Test
    fun testPowRational0() {
        parseAPLExpression("(1÷4)*3").let { result ->
            assertRational(Rational.make(1, 64), result)
        }
    }

    @Test
    fun testPowRational1() {
        parseAPLExpression("(¯3÷4)*7").let { result ->
            assertRational(Rational.make(-2187, 16384), result)
        }
    }

    @Test
    fun testPowRational2() {
        parseAPLExpression("(3÷4)*¯4").let { result ->
            assertRational(Rational.make(256, 81), result)
        }
    }

    @Test
    fun testPowRational3() {
        parseAPLExpression("8*¯4").let { result ->
            assertRational(Rational.make(1, 4096), result)
        }
    }

    @Test
    fun testPowRational4() {
        parseAPLExpression("(int:asBigint 8)*¯4").let { result ->
            assertRational(Rational.make(1, 4096), result)
        }
    }

    @Test
    fun testPowRational5() {
        parseAPLExpression("(¯1÷5)⋆(1÷4)").let { result ->
            assertAPLValue(NearComplex(Complex(0.4728708045015879, 0.47287080450158786)), result)
        }
    }

    @Test
    fun testMax() {
        // ints
        runMaxTest(2, "⌈", "1", "2")
        runMaxTest(0, "⌈", "0", "0")
        runMaxTest(1, "⌈", "1", "0")
        runMaxTest(2, "⌈", "¯10", "2")
        runMaxTest(-10, "⌈", "¯10", "¯20")
        // doubles
        runMaxTest(InnerDouble(2.0), "⌈", "1.0", "2.0")
        runMaxTest(InnerDouble(0.0), "⌈", "0.0", "0.0")
        runMaxTest(InnerDouble(1.0), "⌈", "1.0", "0.0")
        runMaxTest(InnerDouble(2.0), "⌈", "¯10.0", "2.0")
        runMaxTest(InnerDouble(-10.0), "⌈", "¯10.0", "¯20.0")
        // combination
        runMaxTest(InnerDouble(2.0), "⌈", "2", "1.0")
        runMaxTest(InnerDouble(2.0), "⌈", "2.0", "¯9")
        runMaxTest(InnerDouble(0.0), "⌈", "0.0", "¯2")
        runMaxTest(InnerDouble(10.0), "⌈", "10", "1.0")
        // complex
        runMaxTest(Complex(2.0, 3.0), "⌈", "2J3", "1J4")
        runMaxTest(Complex(4.0, 6.0), "⌈", "4J2", "4J6")
        // bigint
        runMaxTest(InnerBigIntOrLong("1000000000000000000000000000001"), "⌈", "1000000000000000000000000000000", "1000000000000000000000000000001")
        runMaxTest(InnerBigIntOrLong("1000000000000000000000000000000"), "⌈", "1000000000000000000000000000000", "¯1000000000000000000000000000001")
        runMaxTest(InnerBigIntOrLong("-1000000000000000000000000000000"), "⌈", "¯1000000000000000000000000000000", "¯1000000000000000000000000000001")
        // rational
        runMaxTest(Rational.make(3, 4), "⌈", "(3÷4)", "(1÷2)")
        runMaxTest(InnerBigIntOrLong(1), "⌈", "(3÷4)", "1")
        runMaxTest(Rational.make(3, 4), "⌈", "(3÷4)", "¯5")
        runMaxTest(Rational.make(1, 2), "⌈", "(¯3÷4)", "(1÷2)")
        runMaxTest(InnerBigIntOrLong(8), "⌈", "(10÷3)", "8")
        // rational to bigint
        runMaxTest(InnerBigIntOrLong("1000000000000000000000000000000000000"), "⌈", "(10÷3)", "1000000000000000000000000000000000000")
        // characters
        parseAPLExpression("@a⌈@b").let { result ->
            val v = result.unwrapDeferredValue()
            assertTrue(v is APLChar)
            assertEquals('b'.code, v.value)
        }
        parseAPLExpression("@C⌈@D").let { result ->
            val v = result.unwrapDeferredValue()
            assertTrue(v is APLChar)
            assertEquals('D'.code, v.value)
        }
    }

    @Test
    fun testMin() {
        runMaxTest(1, "⌊", "1", "2")
        runMaxTest(0, "⌊", "0", "0")
        runMaxTest(0, "⌊", "1", "0")
        runMaxTest(-10, "⌊", "¯10", "2")
        runMaxTest(-20, "⌊", "¯10", "¯20")

        runMaxTest(InnerDouble(1.0), "⌊", "1.0", "2.0")
        runMaxTest(InnerDouble(0.0), "⌊", "0.0", "0.0")
        runMaxTest(InnerDouble(0.0), "⌊", "1.0", "0.0")
        runMaxTest(InnerDouble(-10.0), "⌊", "¯10.0", "2.0")
        runMaxTest(InnerDouble(-20.0), "⌊", "¯10.0", "¯20.0")

        runMaxTest(InnerDouble(1.0), "⌊", "2", "1.0")
        runMaxTest(InnerDouble(-9.0), "⌊", "2.0", "¯9")
        runMaxTest(InnerDouble(-2.0), "⌊", "0.0", "¯2")
        runMaxTest(InnerDouble(1.0), "⌊", "10", "1.0")

        runMaxTest(Complex(1.0, 4.0), "⌊", "2J3", "1J4")
        runMaxTest(Complex(4.0, 2.0), "⌊", "4J2", "4J6")

        runMaxTest(InnerBigIntOrLong("1000000000000000000000000000000"), "⌊", "1000000000000000000000000000000", "1000000000000000000000000000001")
        runMaxTest(InnerBigIntOrLong("-1000000000000000000000000000001"), "⌊", "1000000000000000000000000000000", "¯1000000000000000000000000000001")
        runMaxTest(InnerBigIntOrLong("-1000000000000000000000000000001"), "⌊", "¯1000000000000000000000000000000", "¯1000000000000000000000000000001")

        runMaxTest(Rational.make(3, 4), "⌈", "(3÷4)", "(1÷2)")
        runMaxTest(InnerBigIntOrLong(1), "⌈", "(3÷4)", "1")
        runMaxTest(Rational.make(3, 4), "⌈", "(3÷4)", "¯5")
        runMaxTest(Rational.make(1, 2), "⌈", "(¯3÷4)", "(1÷2)")
        runMaxTest(InnerBigIntOrLong(8), "⌈", "(10÷3)", "8")

        runMaxTest(
            InnerBigIntOrLong("1000000000000000000000000000000000000"),
            "⌊",
            "(5000000000000000000000000000000000000÷3)",
            "1000000000000000000000000000000000000")

        parseAPLExpression("@a⌊@b").let { result ->
            val v = result.unwrapDeferredValue()
            assertTrue(v is APLChar)
            assertEquals('a'.code, v.value)
        }
        parseAPLExpression("@C⌊@D").let { result ->
            val v = result.unwrapDeferredValue()
            assertTrue(v is APLChar)
            assertEquals('C'.code, v.value)
        }
    }

    @Test
    fun minComparingIncompatibleTypes() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("@a⌈1").collapse()
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("1⌈@a").collapse()
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("@a⌊1").collapse()
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("1⌊@a").collapse()
        }
    }

    @Test
    fun testCeiling() {
        assertBigIntOrLong(2, parseAPLExpression("⌈1.4"))
        assertBigIntOrLong(3, parseAPLExpression("⌈2.9"))
        assertBigIntOrLong(-3, parseAPLExpression("⌈¯3.1"))
        assertBigIntOrLong(9, parseAPLExpression("⌈9"))
        assertBigIntOrLong("9", parseAPLExpression("⌈50÷6"))
        assertBigIntOrLong("-16", parseAPLExpression("⌈¯50÷3"))
        assertSimpleNumber(3, parseAPLExpression("⌈3"))
        assertSimpleNumber(-3, parseAPLExpression("⌈¯3"))
        assertBigIntOrLong("3", parseAPLExpression("⌈ int:asBigint 3"))
        assertBigIntOrLong("-3", parseAPLExpression("⌈ int:asBigint ¯3"))
        assertBigIntOrLong("4503599627370494", parseAPLExpression("⌈4503599627370494.0"))
    }

    @Test
    fun ceilOnComplexShouldFail() {
        assertFailsWith<IncompatibleTypeException> {
            parseAPLExpression("⌈1j2")
        }
    }

    @Test
    fun testFloor() {
        assertBigIntOrLong(5, parseAPLExpression("⌊5.9"))
        assertBigIntOrLong(3, parseAPLExpression("⌊3.1"))
        assertBigIntOrLong(-6, parseAPLExpression("⌊¯5.1"))
        assertBigIntOrLong(-9, parseAPLExpression("⌊¯8.9"))
        assertBigIntOrLong("8", parseAPLExpression("⌊50÷6"))
        assertBigIntOrLong("-17", parseAPLExpression("⌊¯50÷3"))
        assertSimpleNumber(3, parseAPLExpression("⌊3"))
        assertSimpleNumber(-3, parseAPLExpression("⌊¯3"))
        assertBigIntOrLong("3", parseAPLExpression("⌊ int:asBigint 3"))
        assertBigIntOrLong("-3", parseAPLExpression("⌊ int:asBigint ¯3"))
        assertBigIntOrLong("4503599627370494", parseAPLExpression("⌊4503599627370494.0"))
    }

    @Test
    fun floorResultFromIntShouldBeInt() {
        parseAPLExpression("⌊1 2 3 4 5 6", collapse = false).let { result ->
            assertSame(ArrayMemberType.LONG, result.specialisedType)
        }
    }

    @Test
    fun floorResultFromDoubleShouldBeInt() {
        parseAPLExpression("⌊1.2 2.2 3.2 4.2 5.2 6.2", collapse = false).let { result ->
            assertSame(ArrayMemberType.LONG, result.specialisedType)
        }
    }

    @Test
    fun floorResultFromGenericShouldBeInt() {
        parseAPLExpression("⌊ 1 2 3 4 5 1.2 2.2 3.2 4.2 5.2 6.2", collapse = false).let { result ->
            assertSame(ArrayMemberType.LONG, result.specialisedType)
        }
    }

    @Test
    fun floorOnComplexShouldFail() {
        assertFailsWith<IncompatibleTypeException> {
            parseAPLExpression("⌊1j2")
        }
    }

    @Test
    fun testComplexCeiling() {
        assertSimpleDouble(2.0, parseAPLExpression("ceilc 1.4"))
        assertSimpleDouble(3.0, parseAPLExpression("ceilc 2.9"))
        assertSimpleDouble(-3.0, parseAPLExpression("ceilc ¯3.1"))
        assertSimpleNumber(9, parseAPLExpression("ceilc 9"))
        assertBigIntOrLong("9", parseAPLExpression("⌈50÷6"))
        assertSimpleComplex(Complex(4.0, 6.0), parseAPLExpression("ceilc 4.1J5.2"))
        assertSimpleComplex(Complex(91.0, 2.0), parseAPLExpression("ceilc 90.8J1.9"))
        assertSimpleComplex(Complex(-1.0, -5.0), parseAPLExpression("ceilc ¯1.8J¯4.82"))
        assertSimpleComplex(Complex(-10.0, -40.0), parseAPLExpression("ceilc ¯10.1J¯40.1"))
    }

    @Test
    fun testComplexFloor() {
        assertSimpleDouble(5.0, parseAPLExpression("floorc 5.9"))
        assertSimpleDouble(-6.0, parseAPLExpression("floorc ¯5.1"))
        assertBigIntOrLong("8", parseAPLExpression("floorc 50÷6"))
        assertBigIntOrLong("-17", parseAPLExpression("floorc ¯50÷3"))
        assertSimpleNumber(3, parseAPLExpression("floorc 3"))
        assertSimpleComplex(Complex(1.0, 4.0), parseAPLExpression("floorc 1.1J3.9"))
        assertSimpleComplex(Complex(2.0, 3.0), parseAPLExpression("floorc 1.9J3.9"))
        assertSimpleComplex(Complex(-2.0, -7.0), parseAPLExpression("floorc ¯1.3J¯7.0"))
        assertSimpleComplex(Complex(-4.0, -6.0), parseAPLExpression("floorc -4.1J5.2"))
        assertSimpleComplex(Complex(1.0, 9.0), parseAPLExpression("floorc 1.01J9.9"))
    }

    @Test
    fun ceilingWithIllegalType() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⌈@a").collapse()
        }
    }

    @Test
    fun failWithWrongDimension() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("1 2 3 4 +[0] 5 6 7 ⍴ ⍳24")
        }
    }

    @Test
    fun floorWithIllegalType() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⌊@x").collapse()
        }
    }

    @Test
    fun failWithWrongAxis() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("1 2 3 4 +[3] 5 6 7 ⍴ ⍳24")
        }
    }

    @Test
    fun floorConvertsComplexToDouble() {
        val result = parseAPLExpression("floorc 3.4J0.01")
        val v = result.unwrapDeferredValue()
        assertTrue(v is APLDouble, "expected APLDouble, actual type: ${v::class.simpleName}")
        assertSimpleDouble(3.0, v)
    }

    @Test
    fun scalarFunctionWithEnclosedArg() {
        parseAPLExpression("(⊂1 2 3) + 10 20").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            result.valueAt(0).let { v ->
                assertDimension(dimensionsOfSize(3), v)
                assertArrayContent(arrayOf(11, 12, 13), v)
            }
            result.valueAt(1).let { v ->
                assertDimension(dimensionsOfSize(3), v)
                assertArrayContent(arrayOf(21, 22, 23), v)
            }
        }
    }

    @Test
    fun twoLevelScalarLeftArg() {
        parseAPLExpression("1 + 1 + 2 3 ⍴ ⍳6").let { result ->
            assertDimension(dimensionsOfSize(2, 3), result)
            assertArrayContent(arrayOf(2, 3, 4, 5, 6, 7), result)
        }
    }

    @Test
    fun twoLevelScalarLeftArgGeneric() {
        parseAPLExpression("1 + 1 + int:ensureGeneric 2 3 ⍴ ⍳6").let { result ->
            assertDimension(dimensionsOfSize(2, 3), result)
            assertArrayContent(arrayOf(2, 3, 4, 5, 6, 7), result)
        }
    }

    @Test
    fun twoLevelEnclosedArrayLeftArg() {
        parseAPLExpression("1 + (⊂1 2) + 2 3 ⍴ ⍳6").let { result ->
            assertDimension(dimensionsOfSize(2, 3), result)
            fun assertElement(expected: Array<Int>, index: Int) {
                result.valueAt(index).let { v ->
                    assertDimension(dimensionsOfSize(expected.size), v)
                    assertArrayContent(expected, v)
                }
            }
            assertElement(arrayOf(2, 3), 0)
            assertElement(arrayOf(3, 4), 1)
            assertElement(arrayOf(4, 5), 2)
            assertElement(arrayOf(5, 6), 3)
            assertElement(arrayOf(6, 7), 4)
            assertElement(arrayOf(7, 8), 5)
        }
    }

    @Test
    fun twoLevelEnclosedArrayLeftArgGeneric() {
        parseAPLExpression("1 + (⊂1 2) + int:ensureGeneric 2 3 ⍴ ⍳6").let { result ->
            assertDimension(dimensionsOfSize(2, 3), result)
            fun assertElement(expected: Array<Int>, index: Int) {
                result.valueAt(index).let { v ->
                    assertDimension(dimensionsOfSize(expected.size), v)
                    assertArrayContent(expected, v)
                }
            }
            assertElement(arrayOf(2, 3), 0)
            assertElement(arrayOf(3, 4), 1)
            assertElement(arrayOf(4, 5), 2)
            assertElement(arrayOf(5, 6), 3)
            assertElement(arrayOf(6, 7), 4)
            assertElement(arrayOf(7, 8), 5)
        }
    }

    @Test
    fun twoLevelEnclosed0() {
        parseAPLExpression("1 + 1 + ⊂10 11").let { result ->
            assertTrue(result.isScalar())
            val v = result.valueAt(0)
            assertDimension(dimensionsOfSize(2), v)
            assertArrayContent(arrayOf(12, 13), v)
        }
    }

    @Test
    fun twoLevelEnclosed1() {
        parseAPLExpression("1 + 1 + ⊂ int:ensureGeneric 10 11").let { result ->
            assertTrue(result.isScalar())
            val v = result.valueAt(0)
            assertDimension(dimensionsOfSize(2), v)
            assertArrayContent(arrayOf(12, 13), v)
        }
    }

    @Test
    fun leftEnclosedRightScalar() {
        parseAPLExpression("(⊂5 6) + 1").let { result ->
            assertTrue(result.isScalar())
            val v = result.valueAt(0)
            assertDimension(dimensionsOfSize(2), v)
            assertArrayContent(arrayOf(6, 7), v)
        }
    }

    @Test
    fun leftEnclosedRightScalarGeneric() {
        parseAPLExpression("(⊂ int:ensureGeneric 5 6) + 1").let { result ->
            assertTrue(result.isScalar())
            val v = result.valueAt(0)
            assertDimension(dimensionsOfSize(2), v)
            assertArrayContent(arrayOf(6, 7), v)
        }
    }

    @Test
    fun bothEnclosed() {
        parseAPLExpression("(⊂5 6) + (⊂10 11)").let { result ->
            assertTrue(result.isScalar())
            val v = result.valueAt(0)
            assertDimension(dimensionsOfSize(2), v)
            assertArrayContent(arrayOf(15, 17), v)
        }
    }

    @Test
    fun bothEnclosedGeneric() {
        parseAPLExpression("(⊂ int:ensureGeneric 5 6) + (⊂ int:ensureGeneric 10 11)").let { result ->
            assertTrue(result.isScalar())
            val v = result.valueAt(0)
            assertDimension(dimensionsOfSize(2), v)
            assertArrayContent(arrayOf(15, 17), v)
        }
    }

    @Test
    fun divNegativeToBigint() {
        parseAPLExpression("¯9223372036854775808÷¯1").let { result ->
            assertBigIntOrLong("9223372036854775808", result)
        }
    }

    private fun runMaxTest(expected: Any, op: String, a: String, b: String) {
        assertAPLValue(expected, parseAPLExpression("${a}${op}${b}"))
        assertAPLValue(expected, parseAPLExpression("${b}${op}${a}"))
    }

    private fun runScalarTest1Arg(functionName: String, doubleFn: (Double) -> Double) {
        val result = parseAPLExpression("${functionName} ¯4.0+⍳10")
        assertDimension(dimensionsOfSize(10), result)
        for (i in 0 until result.dimensions[0]) {
            assertEquals(
                doubleFn((i - 4).toDouble()),
                result.valueAt(i).ensureNumber().asDouble(),
                "function: ${functionName}, arg: ${i - 4}")
        }
    }

    private fun runScalarTest(functionName: String, doubleFn: (Double, Double) -> Double) {
        runScalarTestSD(functionName, doubleFn)
        runScalarTestDS(functionName, doubleFn)
    }

    private fun runScalarTestSD(functionName: String, doubleFn: (Double, Double) -> Double) {
        val result = parseAPLExpression("100 $functionName 100.0+3 4 ⍴ ⍳100")
        assertDimension(dimensionsOfSize(3, 4), result)
        for (i in 0 until result.size) {
            assertEquals(
                doubleFn(100.0, (100 + i).toDouble()),
                result.valueAt(i).ensureNumber().asDouble(),
                "function: ${functionName}}, index: ${i}")
        }
    }

    private fun runScalarTestDS(functionName: String, doubleFn: (Double, Double) -> Double) {
        val result = parseAPLExpression("(100.0+3 4 ⍴ ⍳100) $functionName 10")
        assertDimension(dimensionsOfSize(3, 4), result)
        for (i in 0 until result.size) {
            assertEquals(
                doubleFn((100 + i).toDouble(), 10.0),
                result.valueAt(i).ensureNumber().asDouble(),
                "function: ${functionName}}. index: ${i}")
        }
    }
}
