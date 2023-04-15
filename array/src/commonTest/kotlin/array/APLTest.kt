package array

import array.complex.Complex
import com.dhsdevelopments.mpbignum.BigInt
import com.dhsdevelopments.mpbignum.Rational
import com.dhsdevelopments.mpbignum.make
import com.dhsdevelopments.mpbignum.of
import kotlin.math.pow
import kotlin.test.*

class NearDouble(val expected: Double, val precision: Int = 4) {
    fun assertNear(v: Double, message: String? = null) {
        val dist = 10.0.pow(-precision)
        val messageWithPrefix = if (message == null) "" else ": ${message}"
        assertTrue(expected > v - dist && expected < v + dist, "Expected=${expected}, result=${v}${messageWithPrefix}")
    }
}

class NearComplex(val expected: Complex, val realPrecision: Int = 4, val imPrecision: Int = 4) : APLTest.InnerTest {
    fun assertNear(v: Complex, message: String? = null) {
        val realDist = 10.0.pow(-realPrecision)
        val imDist = 10.0.pow(-imPrecision)
        val messageWithPrefix = if (message == null) "" else ": ${message}"
        assertTrue(
            expected.real > v.real - realDist
                    && expected.real < v.real + realDist
                    && expected.imaginary > v.imaginary - imDist
                    && expected.imaginary < v.imaginary + imDist, "expected=${expected}, result=${v}${messageWithPrefix}")
    }

    override fun assertContent(result: APLValue, message: String?) {
        assertTrue(result is APLNumber)
        assertNear(result.asComplex())
    }
}

abstract class APLTest {
    fun parseAPLExpression(expr: String, withStandardLib: Boolean = false, collapse: Boolean = true, numTasks: Int? = null): APLValue {
        return parseAPLExpression2(expr, withStandardLib, collapse, numTasks).first
    }

    fun parseAPLExpression2(
        expr: String,
        withStandardLib: Boolean = false,
        collapse: Boolean = true,
        numTasks: Int? = null)
            : Pair<APLValue, Engine> {
        val engine = Engine(numTasks)
        engine.addLibrarySearchPath("standard-lib")
        if (withStandardLib) {
            engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"))
        }
        val result = engine.parseAndEval(StringSourceLocation(expr))
        return Pair(if (collapse) result.collapse() else result, engine)
    }

    fun parseAPLExpressionWithOutput(
        expr: String,
        withStandardLib: Boolean = false,
        collapse: Boolean = true,
        numTasks: Int? = null
    ): Pair<APLValue, String> {
        val engine = Engine(numTasks)
        engine.addLibrarySearchPath("standard-lib")
        if (withStandardLib) {
            engine.parseAndEval(StringSourceLocation("use(\"standard-lib.kap\")"))
        }
        val output = StringBuilderOutput()
        engine.standardOutput = output
        val result = engine.parseAndEval(StringSourceLocation(expr))
        engine.withThreadLocalAssigned {
            return Pair(if (collapse) result.collapse() else result, output.buf.toString())
        }
    }

    fun assertArrayContent(expectedValue: Array<out Any>, value: APLValue, message: String? = null) {
        val prefix = if (message == null) "" else "${message}: "
        assertEquals(expectedValue.size, value.size, "Array dimensions mismatch")
        for (i in expectedValue.indices) {
            assertAPLValue(expectedValue[i], value.valueAt(i), "at index: ${i}: ${prefix}")
        }
    }

    fun assertArrayContentDouble(expectedValue: DoubleArray, value: APLValue, message: String? = null) {
        val prefix = if (message == null) "" else "${message}: "
        assertEquals(expectedValue.size, value.size, "Array dimensions mismatch")
        for (i in expectedValue.indices) {
            assertAPLValue(InnerDouble(expectedValue[i]), value.valueAt(i), prefix)
        }
    }

    fun assertDimension(expectDimensions: Dimensions, result: APLValue, message: String? = null) {
        val dimensions = result.dimensions
        val prefix = if (message == null) "" else "${message}: "
        assertTrue(result.dimensions.compareEquals(expectDimensions), "${prefix}expected dimension: $expectDimensions, actual $dimensions")
    }

    fun assertPairs(v: APLValue, vararg values: Array<Int>) {
        for (i in values.indices) {
            val cell = v.valueAt(i)
            val expectedValue = values[i]
            for (eIndex in expectedValue.indices) {
                assertSimpleNumber(expectedValue[eIndex].toLong(), cell.valueAt(eIndex))
            }
        }
    }

    fun assertSimpleNumber(expected: Long, value: APLValue, expr: String? = null) {
        val v = value.unwrapDeferredValue()
        val prefix = "Expected value: ${expected}, actual: ${value}"
        val exprMessage = if (expr == null) prefix else "${prefix}, expr: ${expr}"
        assertTrue(v is APLLong, exprMessage)
        assertEquals(expected, value.ensureNumber().asLong(), exprMessage)
    }

    fun assertDoubleWithRange(expected: Pair<Double, Double>, value: APLValue) {
        assertTrue(value.isScalar())
        val v = value.unwrapDeferredValue()
        assertTrue(v is APLNumber)
        val num = value.ensureNumber().asDouble()
        assertTrue(expected.first <= num, "Comparison is not true: ${expected.first} <= ${num}")
        assertTrue(expected.second >= num, "Comparison is not true: ${expected.second} >= ${num}")
    }

    fun assertSimpleDouble(expected: Double, value: APLValue, message: String? = null) {
        assertAPLValue(InnerDouble(expected), value, message)
    }

    fun assertNearDouble(nearDouble: NearDouble, result: APLValue, message: String? = null) {
        nearDouble.assertNear(result.ensureNumber().asDouble(), message)
    }

    fun assertComplexWithRange(real: Pair<Double, Double>, imaginary: Pair<Double, Double>, result: APLValue) {
        assertTrue(result.isScalar())
        val complex = result.ensureNumber().asComplex()
        val message = "expected: ${real} ${imaginary}, actual: ${complex}"
        assertTrue(real.first <= complex.real && real.second >= complex.real, message)
        assertTrue(imaginary.first <= complex.imaginary && imaginary.second >= complex.imaginary, message)
    }

    fun assertSimpleComplex(expected: Complex, result: APLValue, message: String? = null) {
        assertTrue(result.isScalar(), message)
        val v = result.unwrapDeferredValue()
        assertTrue(v is APLNumber, message)
        assertEquals(expected, v.ensureNumber().asComplex(), message)
    }

    fun assertBigIntOrLong(expected: String, result: APLValue, message: String? = null) {
        assertAPLValue(InnerBigIntOrLong(expected), result, message)
    }

    fun assertBigIntOrLong(expected: Long, result: APLValue, message: String? = null) {
        assertAPLValue(InnerBigIntOrLong(expected), result, message)
    }

    fun assertRational(expected: Rational, result: APLValue, message: String? = null) {
        assertTrue(result is APLRational, "Got ${result}, expected ${expected}")
        assertEquals(expected, result.value, message)
    }

    fun assertString(expected: String, value: APLValue, message: String? = null) {
        val suffix = if (message != null) ": ${message}" else ""
        assertEquals(1, value.dimensions.size, "Expected rank-1, got: ${value.dimensions.size}${suffix}")
        val valueString = value.toStringValue()
        assertEquals(expected, valueString, "Expected '${expected}', got: '${valueString}'${suffix}")
    }

    fun assertAPLNull(value: APLValue) {
        assertDimension(dimensionsOfSize(0), value)
        assertEquals(0, value.dimensions[0])
    }

    fun assertAPLValue(expected: Any, result: APLValue, message: String? = null) {
        when (expected) {
            // Note: The ordering here is important, since the JS target treats all number types as the same type.
            //       Because of this, the check for Long has to be done first, and we throw an explicit error if
            //       the value is a Double since that will fail on JS but work everywhere else.
            is Long -> assertSimpleNumber(expected, result, message)
            is Int -> assertSimpleNumber(expected.toLong(), result, message)
            is Double -> throw IllegalArgumentException("Plain doubles are not supported")
            is Complex -> assertSimpleComplex(expected, result, message)
            is String -> assertString(expected, result, message)
            is NearDouble -> assertNearDouble(expected, result, message)
            is InnerTest -> expected.assertContent(result, message)
            is Rational -> assertRational(expected, result, message)
            else -> throw IllegalArgumentException("No support for comparing values of type: ${result::class.simpleName}")
        }
    }

    fun assertSymbolName(engine: Engine, name: String, value: APLValue) {
        assertSame(engine.internSymbol(name), value.ensureSymbol().value)
    }

    fun assertSymbolNameCoreNamespace(engine: Engine, name: String, value: APLValue) {
        assertSame(engine.internSymbol(name, engine.coreNamespace), value.ensureSymbol().value)
    }

    fun assert1DArray(expected: Array<out Any>, result: APLValue, message: String? = null) {
        assertDimension(dimensionsOfSize(expected.size), result, message)
        assertArrayContent(expected, result, message)
    }

    @BeforeTest
    fun initTest() {
        nativeTestInit()
    }

    interface InnerTest {
        fun assertContent(result: APLValue, message: String? = null)
    }

    inner class InnerArray(val expectedDimensions: Dimensions, val expected: Array<out Any>) : InnerTest {
        override fun assertContent(result: APLValue, message: String?) {
            assertDimension(expectedDimensions, result, message)
            assertArrayContent(expected, result, message)
        }
    }

    inner class Inner1D(val expected: Array<out Any>) : InnerTest {
        override fun assertContent(result: APLValue, message: String?) {
            assert1DArray(expected, result, message)
        }
    }

    inner class InnerAPLNull : InnerTest {
        override fun assertContent(result: APLValue, message: String?) {
            assertDimension(dimensionsOfSize(0), result, message)
        }
    }

    inner class InnerDouble(val expectedDouble: Double) : InnerTest {
        override fun assertContent(result: APLValue, message: String?) {
            assertTrue(result is APLDouble, "Result should be double, was: ${result}")
            assertEquals(expectedDouble, result.ensureNumber().asDouble())
        }
    }

    inner class InnerDoubleOrLong(val expectedDouble: Double) : InnerTest {
        override fun assertContent(result: APLValue, message: String?) {
            assertEquals(expectedDouble, result.ensureNumber().asDouble())
        }
    }

    inner class InnerBigIntOrLong(val expected: BigInt) : InnerTest {
        constructor(expectedString: String) : this(BigInt.of(expectedString))
        constructor(expectedLong: Long) : this(BigInt.of(expectedLong))

        override fun assertContent(result: APLValue, message: String?) {
            val v: String = when (result) {
                is APLLong -> result.value.toString()
                is APLBigInt -> result.value.toString()
                else -> {
                    val msgSuffix = if (message == null) "" else ": ${message}"
                    fail("Unexpected type: ${result}${msgSuffix}")
                }
            }
            assertEquals(expected.toString(), v, "${expected} != ${v}, ${message}")
        }
    }

    inner class InnerRational(val expected: Rational) : InnerTest {
        constructor(num: Long, den: Long) : this(Rational.make(num, den))

        override fun assertContent(result: APLValue, message: String?) {
            assertRational(expected, result, message)
        }
    }
}

expect fun nativeTestInit()
expect fun tryGc()
