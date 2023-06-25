package array

import array.complex.Complex
import com.dhsdevelopments.mpbignum.Rational
import com.dhsdevelopments.mpbignum.make
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class NumbersTest : APLTest() {
    @Test
    fun testMathsOperations() {
        assertMathsOperation({ a, b -> a + b }, "+")
        assertMathsOperation({ a, b -> a - b }, "-")
        assertMathsOperation({ a, b -> a * b }, "×")
    }

    @Test
    fun monadicAdd() {
        parseAPLExpression("+1 2 ¯6 2J7 2J¯7").let { result ->
            assert1DArray(arrayOf(1, 2, -6, Complex(2.0, -7.0), Complex(2.0, 7.0)), result)
        }
    }

    @Test
    fun monadicAddWithBigint() {
        parseAPLExpression("+¯10000000000000000000000000000000 10000000000000000000000000000000").let { result ->
            assert1DArray(
                arrayOf(
                    InnerBigIntOrLong("-10000000000000000000000000000000"),
                    InnerBigIntOrLong("10000000000000000000000000000000")),
                result)
        }
    }

    @Test
    fun testSignum() {
        parseAPLExpression("× 2 0 ¯3 ¯10000000000000000000000000000000 10000000000000000000000000000000").let { result ->
            assert1DArray(arrayOf(1, 0, -1, -1, 1), result)
        }
    }

    @Test
    fun addToBigIntSpecialisedArray() {
        parseAPLExpression("+/ 10 ⍴ 1000000000000000000").let { result ->
            assertBigIntOrLong("10000000000000000000", result)
        }
    }

    @Test
    fun addToBigIntGenericArray() {
        parseAPLExpression("+/ int:ensureGeneric 10 ⍴ 1000000000000000000").let { result ->
            assertBigIntOrLong("10000000000000000000", result)
        }
    }

    @Test
    fun addBigintToDouble() {
        parseAPLExpression("(int:asBigint 5) + 0.0").let { result ->
            assertNearDouble(NearDouble(5.0), result)
        }
    }

    @Test
    fun addDoubleToBigint() {
        parseAPLExpression("0.0 + int:asBigint 5").let { result ->
            assertNearDouble(NearDouble(5.0), result)
        }
    }


    @Test
    fun subBigint() {
        parseAPLExpression("30000000000000000000 - 10000000000000000000").let { result ->
            assertBigIntOrLong("20000000000000000000", result)
        }
    }

    @Test
    fun subBigintSpeialisedArray() {
        parseAPLExpression("-/ 100000 ⍴ 100000000000000").let { result ->
            assertBigIntOrLong("-9999800000000000000", result)
        }
    }

    @Test
    fun testDivision() {
        assertSimpleNumber(0, parseAPLExpression("1÷0"))
        assertSimpleNumber(0, parseAPLExpression("100÷0"))
        assertSimpleNumber(0, parseAPLExpression("¯100÷0"))
        assertSimpleDouble(0.0, parseAPLExpression("12.2÷0"))
        assertSimpleDouble(0.0, parseAPLExpression("2÷0.0"))
        assertSimpleDouble(0.0, parseAPLExpression("2.0÷0.0"))
        assertSimpleNumber(2, parseAPLExpression("4÷2"))
        assertSimpleNumber(20, parseAPLExpression("40÷2"))
        assertDoubleWithRange(Pair(3.33332, 3.33334), parseAPLExpression("10÷3"))
    }

    @Test
    fun testDivisionBigint() {
        parseAPLExpression("10000000000000000000000000000000 ÷ 2").let { result ->
            assertBigIntOrLong("5000000000000000000000000000000", result)
        }
        parseAPLExpression("123456789012345678901234567891 ÷ 2").let { result ->
            assertNearDouble(NearDouble(6.172839450617284e+28, -24), result)
        }
    }

    @Test
    fun divisionConvertToDouble() {
        // Test provided by dzaima
        parseAPLExpression("a←×/24⍴2 ⋄ a (a×a×a) ÷ 1000000000000000").let { result ->
            assert1DArray(
                arrayOf(
                    NearDouble(1.6777216e-8),
                    NearDouble(4722366.482869646)),
                result)
        }
    }

    @Test
    fun testMonadicDivisionBigint() {
        parseAPLExpression("÷123456789012345678901234567890").let { result ->
            assertDoubleWithRange(Pair(8.09999E-30, 8.10001E-30), result)
        }
    }

    @Test
    fun testAbs() {
        // Plain integers
        assertSimpleNumber(2, parseAPLExpression("|2"))
        assertSimpleNumber(10, parseAPLExpression("|¯10"))
        // Floating point
        assertDoubleWithRange(Pair(10.79999, 10.80001), parseAPLExpression("|10.8"))
        assertDoubleWithRange(Pair(4.89999, 4.90001), parseAPLExpression("|¯4.9"))
        // Complex numbers
        assertDoubleWithRange(Pair(9.219543, 9.219545), parseAPLExpression("|6J7"))
        assertDoubleWithRange(Pair(4.472134, 4.472136), parseAPLExpression("|¯4J2"))
        assertDoubleWithRange(Pair(4.472134, 4.472136), parseAPLExpression("|4J¯2"))
        assertDoubleWithRange(Pair(342.285, 342.287), parseAPLExpression("|¯194J¯282"))
    }

    @Test
    fun testAbsWithBignum() {
        parseAPLExpression("|10000000000000000000000000000000 ¯10000000000000000000000000000000 (int:asBigint 0)").let { result ->
            assert1DArray(
                arrayOf(
                    InnerBigIntOrLong("10000000000000000000000000000000"),
                    InnerBigIntOrLong("10000000000000000000000000000000"),
                    InnerBigIntOrLong("0")), result)
        }
    }

    @Test
    fun absRational() {
        parseAPLExpression("| (1÷9) (¯3÷2) (100000000000000000000000000000000000÷3) (1000000000000000000000000000000000000÷3)").let { result ->
            assert1DArray(
                arrayOf(
                    InnerRational(1, 9),
                    InnerRational(3, 2),
                    InnerRational(Rational.make("100000000000000000000000000000000000", "3")),
                    InnerRational(Rational.make("1000000000000000000000000000000000000", "3"))),
                result)
        }
    }

    @Test
    fun testMod() {
        assertSimpleNumber(1, parseAPLExpression("2|3"))
        assertDoubleWithRange(Pair(0.66669, 0.700001), parseAPLExpression("1|1.7"))
        assertSimpleNumber(-1, parseAPLExpression("¯2|11"))
        assertSimpleNumber(0, parseAPLExpression("3|3"))
        assertSimpleNumber(2, parseAPLExpression("100|2"))
        assertSimpleNumber(9995, parseAPLExpression("10000|¯20005"))
        assertSimpleNumber(0, parseAPLExpression("5|0"))
        assertSimpleNumber(0, parseAPLExpression("0|0"))
        assertSimpleNumber(3, parseAPLExpression("0|3"))
        assertAPLValue(NearComplex(Complex(1.0, 1.0), 4, 4), parseAPLExpression("2J3 | 10J8"))
        assertAPLValue(NearComplex(Complex(2.7, 3.1), 4, 4), parseAPLExpression("2.6J3.8 | 10.7J31.1"))
        assertAPLValue(NearComplex(Complex(10.2, 9.9), 4, 4), parseAPLExpression("0 | 10.2J9.9"))
    }

    @Test
    fun testModCombinationsLong() {
        parseAPLExpression("(int:ensureLong 2 2 ¯2 ¯2) | (int:ensureLong 123 ¯123 123 ¯123)").let { result ->
            assert1DArray(arrayOf(1, 1, -1, -1), result)
        }
    }

    @Test
    fun testModCombinationsGeneric() {
        parseAPLExpression("(int:ensureGeneric 2 2 ¯2 ¯2) | (int:ensureGeneric 123 ¯123 123 ¯123)").let { result ->
            assert1DArray(arrayOf(1, 1, -1, -1), result)
        }
    }

    @Test
    fun testModCombinationsBigint0() {
        parseAPLExpression("(int:asBigint¨ 2 2 ¯2 ¯2) | int:asBigint¨ 123 ¯123 123 ¯123").let { result ->
            assert1DArray(arrayOf(InnerBigIntOrLong(1), InnerBigIntOrLong(1), InnerBigIntOrLong(-1), InnerBigIntOrLong(-1)), result)
        }
    }

    @Test
    fun testModCombinationsBigint1() {
        parseAPLExpression("(int:asBigint¨ 10000 10000 ¯10000 ¯10000) | int:asBigint¨ 20005 ¯20005 20005 ¯20005").let { result ->
            assert1DArray(arrayOf(InnerBigIntOrLong(5), InnerBigIntOrLong(9995), InnerBigIntOrLong(-9995), InnerBigIntOrLong(-5)), result)
        }
    }

    //10000|¯20005

    @Test
    fun testModOptimisedInt() {
        parseAPLExpression("4 | int:ensureLong 2 5 6 ¯2").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(2, 1, 2, 2), result)
        }
        parseAPLExpression("4 | int:ensureGeneric 2 5 6 ¯2").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(2, 1, 2, 2), result)
        }
    }

    @Test
    fun testModOptimisedDouble() {
        parseAPLExpression("2.0 | int:ensureDouble 2.0 2.1 2.5").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(NearDouble(0.0, 4), NearDouble(0.1, 4), NearDouble(0.5, 4)), result)
        }
        parseAPLExpression("2.0 | int:ensureGeneric 2.0 2.1 2.5").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(NearDouble(0.0, 4), NearDouble(0.1, 4), NearDouble(0.5, 4)), result)
        }
    }

    @Test
    fun testModBigint() {
        parseAPLExpression("4 | (int:asBigint 2) (int:asBigint 5) (int:asBigint 6) (int:asBigint ¯2) 123456789012345678901234567891").let { result ->
            assert1DArray(
                arrayOf(
                    InnerBigIntOrLong("2"),
                    InnerBigIntOrLong("1"),
                    InnerBigIntOrLong("2"),
                    InnerBigIntOrLong("2"),
                    InnerBigIntOrLong("3")),
                result)
        }
    }

    @Test
    fun testModRational() {
        parseAPLExpression("5 | (117÷10) (¯117÷3)").let { result ->
            assert1DArray(arrayOf(InnerRational(17, 10), InnerBigIntOrLong(1)), result)
        }
    }

    @Test
    fun testNegation() {
        assertSimpleNumber(0, parseAPLExpression("-0"))
        assertSimpleNumber(1, parseAPLExpression("-(1-2)"))
        assertSimpleNumber(-3, parseAPLExpression("-3"))
        assertSimpleNumber(-6, parseAPLExpression("-2+4"))
    }

    @Test
    fun testExponential() {
        assertAPLValue(InnerBigIntOrLong(1024), parseAPLExpression("2⋆10"))
        assertDoubleWithRange(Pair(0.0009, 0.0011), parseAPLExpression("10⋆¯3"))
        assertSimpleDouble(0.0, parseAPLExpression("0⋆10.0"))
        assertBigIntOrLong("0", parseAPLExpression("0⋆10"))
        assertSimpleDouble(1.0, parseAPLExpression("10⋆0.0"))
        assertBigIntOrLong("1", parseAPLExpression("10⋆0"))
        assertComplexWithRange(Pair(-0.0000001, 0.0000001), Pair(1.732050807, 1.732050809), parseAPLExpression("¯3⋆0.5"))
        assertComplexWithRange(Pair(0.01342669136, 0.01342669138), Pair(0.04132310697, 0.04132310699), parseAPLExpression("¯7.1*¯1.6"))
        assertBigIntOrLong("-27", parseAPLExpression("¯3⋆3"))
        // This is special cased to be compatible with apl
        assertSimpleNumber(1, parseAPLExpression("0⋆0"))
        assertSimpleDouble(1.0, parseAPLExpression("0⋆0.0"))
    }

    @Test
    fun testExponentialWithComplex() {
        parseAPLExpression("1J1 ¯2J9 1J¯9 ¯2J¯2*3").let { result ->
            assert1DArray(
                arrayOf(
                    NearComplex(Complex(-2.0, 2.0)),
                    NearComplex(Complex(478.0, -621.0)),
                    NearComplex(Complex(-242.0, 702.0)),
                    NearComplex(Complex(16.0, -16.0))),
                result)
        }
    }

    @Test
    fun testMonadicExponential() {
        parseAPLExpression("⋆1 2 (int:asBigint 3) ¯10").let { result ->
            assert1DArray(
                arrayOf(
                    NearDouble(2.718281828),
                    NearDouble(7.389056099),
                    NearDouble(20.08553692),
                    NearDouble(0.00004539992976, 8)),
                result)
        }
    }

    @Test
    fun exponentResultShouldNotExpandToBigint() {
        parseAPLExpression("2⋆3").let { result ->
            assertIs<APLLong>(result)
            assertSimpleNumber(8, result)
        }
    }

    @Test
    fun invalidExpressions() {
        assertFailsWith<ParseException> {
            parseAPLExpression("1+")
        }
        assertFailsWith<ParseException> {
            parseAPLExpression("1++")
        }
        assertFailsWith<ParseException> {
            parseAPLExpression("-")
        }
        assertFailsWith<ParseException> {
            parseAPLExpression("1 2 3 4+")
        }
    }

    @Test
    fun mathOperationsWithCharacters() {
//        testFailedOpWithChar("+")
//        testFailedOpWithChar("-")
        testFailedOpWithChar("×")
        testFailedOpWithChar("÷")
        testFailedOpWithChar("⋆")
        testFailedOpWithChar("|")
    }

    private fun testFailedOpWithChar(name: String) {
        assertFailsWith<APLEvalException> { parseAPLExpression("1${name}@a").collapse() }
        assertFailsWith<APLEvalException> { parseAPLExpression("@a${name}1").collapse() }
        assertFailsWith<APLEvalException> { parseAPLExpression("@a${name}@b").collapse() }
    }

    @Test
    fun functionAliases() {
        val result = parseAPLExpression("2*4")
        assertAPLValue(InnerBigIntOrLong(16), result)
    }

    private fun assertMathsOperation(op: (Long, Long) -> Long, name: String) {
        val args: Array<Long> = arrayOf(0, 1, -1, 2, 3, 10, 100, 123456, -12345)
        args.forEach { left ->
            args.forEach { right ->
                val expr = "${formatLongAsAPL(left)}${name}${formatLongAsAPL(right)}"
                val result = parseAPLExpression(expr)
                val expect = op(left, right)
                assertSimpleNumber(expect, result, expr)
            }
        }
    }

    private fun formatLongAsAPL(value: Long): String {
        return if (value < 0) {
            // This is a hack to deal with Long.MIN_VALUE
            "¯${value.toString().substring(1)}"
        } else {
            value.toString()
        }
    }

    @Test
    fun optimisedAdd() {
        runExprTest("(int:ensureLong 1 2 3 4) + (int:ensureLong 11 12 13 14)") { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(12, 14, 16, 18), result)
        }
    }

    private fun runExprTest(expr: String, withStandardLib: Boolean = false, fn: (APLValue) -> Unit) {
        parseAPLExpression(expr, withStandardLib).let { result ->
            fn(result)
        }
    }

    @Test
    fun positiveIntegerGamma() {
        parseAPLExpression("!1 2 10 11").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(InnerDouble(1.0), InnerDouble(2.0), InnerDouble(3628800.0), InnerDouble(39916800.0)), result)
        }
    }

    @Test
    fun positiveDoubleGamma() {
        parseAPLExpression("!1.2 1.3 1.4 10.1 2.2 4.2").let { result ->
            assertDimension(dimensionsOfSize(6), result)
            assertArrayContent(
                arrayOf(
                    NearDouble(1.101802491, 4),
                    NearDouble(1.166711905, 4),
                    NearDouble(1.242169345, 4),
                    NearDouble(4593083.59, 2),
                    NearDouble(2.42396548, 4),
                    NearDouble(32.57809605, 4)),
                result)
        }
    }

    @Test
    fun negativeDoubleGamma() {
        parseAPLExpression("!¯1.2 ¯3.4 ¯5.1 ¯5.2 ¯2.2 ¯2.3").let { result ->
            assertDimension(dimensionsOfSize(6), result)
            assertArrayContent(
                arrayOf(
                    NearDouble(-5.821148569, 4),
                    NearDouble(-1.108029947, 4),
                    NearDouble(-0.3639731139, 4),
                    NearDouble(-0.1640610505, 4),
                    NearDouble(4.850957141, 4),
                    NearDouble(3.328347007, 4)),
                result)
        }
    }

    @Test
    fun positiveIntegerBinomial() {
        parseAPLExpression("10!32").let { result ->
            assertSimpleNumber(64512240, result)
        }
    }

    @Test
    fun positiveDoubleBinominal() {
        parseAPLExpression("10.2!32.2").let { result ->
            assertNearDouble(NearDouble(80760102.76, 2), result)
        }
    }

    @Test
    fun complexBinomial() {
        parseAPLExpression("0 2 2J2 3J¯3 ¯3J10.1 ¯3J¯4 ∘.! 0 3 8.1J1 ¯3.4J4 10J¯3 ¯2J¯8").let { result ->
            assertDimension(dimensionsOfSize(6, 6), result)
            assertArrayContent(
                arrayOf(
                    NearComplex(Complex(1.0, 0.0)),
                    NearComplex(Complex(1.0, 0.0)),
                    NearComplex(Complex(1.0, 0.0)),
                    NearComplex(Complex(1.0, 0.0)),
                    NearComplex(Complex(1.0, 0.0)),
                    NearComplex(Complex(1.0, 0.0)),
                    NearComplex(Complex(0.0, 0.0)),
                    NearComplex(Complex(3.0, 0.0)),
                    NearComplex(Complex(28.255, 7.6)),
                    NearComplex(Complex(-0.52, -15.6)),
                    NearComplex(Complex(40.5, -28.5)),
                    NearComplex(Complex(-29.0, 20.0)),
                    NearComplex(Complex(21.30646169, 21.30646169)),
                    NearComplex(Complex(12.78387701, -12.78387701)),
                    NearComplex(Complex(-26.45504396, 54.29773874)),
                    NearComplex(Complex(0.1771815219, -0.02202208026)),
                    NearComplex(Complex(-67.13452858, 236.1405125)),
                    NearComplex(Complex(1166.024365, -3346.794191)),
                    NearComplex(Complex(-328.6986648, 328.6986648)),
                    NearComplex(Complex(-80.91044057, 10.11380507)),
                    NearComplex(Complex(374.3589127, -543.1323969)),
                    NearComplex(Complex(40407.45676, -142258.8772)),
                    NearComplex(Complex(-248.375, -29.23214286)),
                    NearComplex(Complex(-0.02446697456, -0.4780505611)),
                    NearComplex(Complex(-8.729323264E11, 2.592868296E11), -7, -7),
                    NearComplex(Complex(3783271107.0, -344239831.5), -5, -5),
                    NearComplex(Complex(-21687822.9, -12528639.47), -3, -3),
                    NearComplex(Complex(-19219016.06, -335408971.5), -3, -3),
                    NearComplex(Complex(77075910.62, 74882120.92), -3, -3),
                    NearComplex(Complex(-3.870615137E12, 2.375705362E12), -8, -8),
                    NearComplex(Complex(-7302.062228, -5476.546671)),
                    NearComplex(Complex(-34.67794562, 206.7833053)),
                    NearComplex(Complex(14.54486452, -14.78546549)),
                    NearComplex(Complex(-48737.12032, 8780.542527)),
                    NearComplex(Complex(3.029684439, -0.7536373119)),
                    NearComplex(Complex(0.06167254732, 0.04814395987))),
                result)
        }
    }

    @Test
    fun squareRootInt() {
        parseAPLExpression("√4 2 10 1 0").let { result ->
            assert1DArray(
                arrayOf(NearDouble(2.0), NearDouble(1.414213562), NearDouble(3.16227766), NearDouble(1.0), NearDouble(0.0)),
                result)
        }
    }

    @Test
    fun squareRootIntNegative() {
        parseAPLExpression("√¯1 ¯2 ¯5 ¯123459").let { result ->
            assert1DArray(
                arrayOf(
                    NearComplex(Complex(0.0, 1.0)),
                    NearComplex(Complex(0.0, 1.414213562)),
                    NearComplex(Complex(0.0, 2.236067977)),
                    NearComplex(Complex(0.0, 351.3673292))),
                result)
        }
    }

    @Test
    fun squareRootIntNegativeBase3() {
        parseAPLExpression("3√¯1 ¯2 ¯5 ¯123459").let { result ->
            assert1DArray(
                arrayOf(
                    NearComplex(Complex(0.5, 0.8660254038)),
                    NearComplex(Complex(0.6299605249, 1.091123636)),
                    NearComplex(Complex(0.8549879733, 1.48088261)),
                    NearComplex(Complex(24.89684159, 43.12259457))),
                result)
        }
    }

    @Test
    fun squareRootDouble() {
        parseAPLExpression("√1.7 0.1 12345.9 9.7 9876.1234 1.987654321").let { result ->
            assert1DArray(
                arrayOf(
                    NearDouble(1.303840481),
                    NearDouble(0.316227766),
                    NearDouble(111.1121056),
                    NearDouble(3.1144823),
                    NearDouble(99.37868685),
                    NearDouble(1.409841949)),
                result)
        }
    }

    @Test
    fun squareRootExplicitBase() {
        parseAPLExpression("2√1.7 0.1 12345.9 9.7 9876.1234 1.987654321").let { result ->
            assert1DArray(
                arrayOf(
                    NearDouble(1.303840481),
                    NearDouble(0.316227766),
                    NearDouble(111.1121056),
                    NearDouble(3.1144823),
                    NearDouble(99.37868685),
                    NearDouble(1.409841949)),
                result)
        }
    }

    @Test
    fun squareRootDoubleNegative() {
        parseAPLExpression("√¯1.7 ¯10000.2 ¯0.001 ¯9.5").let { result ->
            assert1DArray(
                arrayOf(
                    NearComplex(Complex(0.0, 1.303840481)),
                    NearComplex(Complex(0.0, 100.001)),
                    NearComplex(Complex(0.0, 0.0316227766)),
                    NearComplex(Complex(0.0, 3.082207001))),
                result)
        }
    }

    @Test
    fun squareRootDoubleNegativeExplicitBase() {
        parseAPLExpression("2√¯1.7 ¯10000.2 ¯0.001 ¯9.5").let { result ->
            assert1DArray(
                arrayOf(
                    NearComplex(Complex(0.0, 1.303840481)),
                    NearComplex(Complex(0.0, 100.001)),
                    NearComplex(Complex(0.0, 0.0316227766)),
                    NearComplex(Complex(0.0, 3.082207001))),
                result)
        }
    }

    @Test
    fun squareRootNegativeIntegerComplexBase() {
        parseAPLExpression("2J1√¯1 ¯2 ¯5 ¯123459").let { result ->
            assert1DArray(
                arrayOf(
                    NearComplex(Complex(0.5792387863, 1.782713677)),
                    NearComplex(Complex(1.082032803, 2.224120703)),
                    NearComplex(Complex(2.119650817, 2.870532607)),
                    NearComplex(Complex(94.65947366, -180.6312599))),
                result)
        }
    }

    @Test
    fun squareRootDoubleComplexBase() {
        parseAPLExpression("1J1√1.7 0.1 12345.9 9.7 9876.1234 1.987654321").let { result ->
            assert1DArray(
                arrayOf(
                    NearComplex(Complex(1.258219338, -0.3418831619)),
                    NearComplex(Complex(0.1288018808, 0.2888080254)),
                    NearComplex(Complex(-0.2054827018, 111.1119155)),
                    NearComplex(Complex(1.311722151, -2.824780522)),
                    NearComplex(Complex(-11.25047118, 98.73981111)),
                    NearComplex(Complex(1.327491996, -0.4747834466))),
                result)
        }
    }

    @Test
    fun squareRootBigint() {
        parseAPLExpression("√(int:asBigint 15) (int:asBigint ¯15)").let { result ->
            assert1DArray(
                arrayOf(
                    NearDouble(3.872983346207417),
                    NearComplex(Complex(2.3715183290419594e-16, 3.872983346207417))),
                result)
        }
    }

    @Test
    fun nthRootBigint() {
        parseAPLExpression("3√(int:asBigint 15) (int:asBigint ¯15)").let { result ->
            assert1DArray(
                arrayOf(
                    NearDouble(2.46621207433047),
                    NearComplex(Complex(1.2331060371652354, 2.1358023074901036))),
                result)
        }
    }

    @Test
    fun squareRootRational() {
        parseAPLExpression("√ (3÷5) (¯3÷5)").let { result ->
            assert1DArray(
                arrayOf(
                    NearDouble(0.7745966692414834),
                    NearComplex(Complex(4.743036658083919e-17, 0.7745966692414834))),
                result)
        }
    }

    @Test
    fun nthRootRational() {
        parseAPLExpression("(11÷10) √ (3÷5) (¯3÷5)").let { result ->
            assert1DArray(
                arrayOf(
                    NearDouble(0.6285203136088378),
                    NearComplex(Complex(-0.6030608246816601, 0.17707463497979534))),
                result)
        }
    }

    @Test
    fun mathConstants() {
        parseAPLExpression("math:pi", withStandardLib = true).let { result ->
            assertNearDouble(NearDouble(3.14159265358979323846, 15), result)
        }
    }
}
