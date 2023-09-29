package array

import kotlin.test.Test
import kotlin.test.assertEquals

class TypesTest : APLTest() {
    @Test
    fun testInteger() {
        testResultType("typeof 10", APLValueType.INTEGER)
        testResultType("typeof ¯10", APLValueType.INTEGER)
        testResultType("typeof 100", APLValueType.INTEGER)
        testResultType("typeof 0", APLValueType.INTEGER)
    }

    @Test
    fun testDouble() {
        testResultType("typeof 1.2", APLValueType.FLOAT)
        testResultType("typeof 10.", APLValueType.FLOAT)
        testResultType("typeof 1.0", APLValueType.FLOAT)
        testResultType("typeof 0.0", APLValueType.FLOAT)
        testResultType("typeof 100.0", APLValueType.FLOAT)
        testResultType("typeof ¯1.0", APLValueType.FLOAT)
        testResultType("typeof ¯1.2", APLValueType.FLOAT)
        testResultType("typeof ¯1.", APLValueType.FLOAT)
        testResultType("typeof 1+1.2", APLValueType.FLOAT)
    }

    @Test
    fun testComplex() {
        testResultType("typeof 1J2", APLValueType.COMPLEX)
        testResultType("typeof 1.2J2.4", APLValueType.COMPLEX)
        testResultType("typeof 100+1J2", APLValueType.COMPLEX)
    }

    @Test
    fun testChar() {
        testResultType("typeof 0 ⌷ \"foo\"", APLValueType.CHAR)
        testResultType("typeof 0 0 ⌷ 2 2 ⍴ \"foox\"", APLValueType.CHAR)
    }

    @Test
    fun testArray() {
        testResultType("typeof 0⍴1", APLValueType.ARRAY)
        testResultType("typeof 1 2 3 4 5 6", APLValueType.ARRAY)
        testResultType("typeof ⍳100", APLValueType.ARRAY)
        testResultType("typeof (⍳100) × ⍳100", APLValueType.ARRAY)
        testResultType("typeof ⍬", APLValueType.ARRAY)
    }

    @Test
    fun testSymbol() {
        testResultType("typeof 'foo", APLValueType.SYMBOL)
    }

    @Test
    fun testLambdaFunction() {
        testResultType("typeof λ { ⍺+⍵+1 }", APLValueType.LAMBDA_FN)
    }

    @Test
    fun testList() {
        testResultType("typeof (1;2;3)", APLValueType.LIST)
    }

    @Test
    fun testMaps() {
        testResultType("typeof map \"a\" 2", APLValueType.MAP)
    }

    @Test
    fun specialisedTypeFromAddLong() {
        parseAPLExpression("1 2 + 3 4", collapse = false).let { result ->
            assertEquals(ArrayMemberType.LONG, result.specialisedType)
        }
    }

    @Test
    fun specialisedTypeFromAddSingleValueLeftLong() {
        parseAPLExpression("1 + 3 4", collapse = false).let { result ->
            assertEquals(ArrayMemberType.LONG, result.specialisedType)
        }
    }

    @Test
    fun specialisedTypeFromSingleValueRightAddLong() {
        parseAPLExpression("1 2 + 3", collapse = false).let { result ->
            assertEquals(ArrayMemberType.LONG, result.specialisedType)
        }
    }

    @Test
    fun specialisedTypeFromAddDouble() {
        parseAPLExpression("1.1 2.0 + 3.9 4.8", collapse = false).let { result ->
            assertEquals(ArrayMemberType.DOUBLE, result.specialisedType)
        }
    }

    @Test
    fun specialisedTypeFromSingleValueLeftAddDouble() {
        parseAPLExpression("1.1 + 3.9 4.8", collapse = false).let { result ->
            assertEquals(ArrayMemberType.DOUBLE, result.specialisedType)
        }
    }

    @Test
    fun specialisedTypeFromSingleValueRightAddDouble() {
        parseAPLExpression("1.1 2.0 + 3.9", collapse = false).let { result ->
            assertEquals(ArrayMemberType.DOUBLE, result.specialisedType)
        }
    }

    @Test
    fun specialisedTypeAddLongAndDoubleArrays() {
        parseAPLExpression("1.1 2.0 + 10 11", collapse = false).let { result ->
            assertEquals(ArrayMemberType.DOUBLE, result.specialisedType)
        }
    }

    @Test
    fun specialisedTypeAddDoubleAndLongArrays() {
        parseAPLExpression("1 2 + 10.0 11.3", collapse = false).let { result ->
            assertEquals(ArrayMemberType.DOUBLE, result.specialisedType)
        }
    }

    @Test
    fun specialisedTypeAddSingleValueDoubleLongArray() {
        parseAPLExpression("1.1 + 10 11", collapse = false).let { result ->
            assertEquals(ArrayMemberType.DOUBLE, result.specialisedType)
        }
    }

    @Test
    fun specialisedTypeAddLongArraysSingleValueDouble() {
        parseAPLExpression("10 11 + 1.1", collapse = false).let { result ->
            assertEquals(ArrayMemberType.DOUBLE, result.specialisedType)
        }
    }

    @Test
    fun specialisedTypeAddSingleValueLongDoubleArray() {
        parseAPLExpression("1 + 10.0 11.3", collapse = false).let { result ->
            assertEquals(ArrayMemberType.DOUBLE, result.specialisedType)
        }
    }

    @Test
    fun specialisedTypeAddDoubleArraySingleValueLong() {
        parseAPLExpression("1.1 2.0 + 1", collapse = false).let { result ->
            assertEquals(ArrayMemberType.DOUBLE, result.specialisedType)
        }
    }

    private fun testResultType(expression: String, expectedResultSym: APLValueType) {
        val engine = Engine()
        val result = engine.parseAndEval(StringSourceLocation(expression))
        assertSymbolNameCoreNamespace(engine, expectedResultSym.typeName, result)
    }
}
