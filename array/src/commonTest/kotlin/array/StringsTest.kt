package array

import array.complex.Complex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StringsTest : APLTest() {
    @Test
    fun testPrint() {
        parseAPLExpressionWithOutput("io:print 200").let { (result, out) ->
            assertSimpleNumber(200, result)
            assertEquals("200", out)
        }
    }

    @Test
    fun testPrintPretty() {
        parseAPLExpressionWithOutput(":pretty io:print \"a\"").let { (result, out) ->
            assertString("a", result)
            assertEquals("\"a\"", out)
        }
    }

    @Test
    fun testPrintString() {
        parseAPLExpressionWithOutput("io:print \"a\"").let { (result, out) ->
            assertString("a", result)
            assertEquals("a", out)
        }
    }

    @Test
    fun readableNumber() {
        parseAPLExpressionWithOutput(":read io:print 1").let { (result, out) ->
            assertSimpleNumber(1, result)
            assertEquals("1", out)
        }
    }

    @Test
    fun readableString() {
        parseAPLExpressionWithOutput(":read io:print \"foo\"").let { (result, out) ->
            assertString("foo", result)
            assertEquals("\"foo\"", out)
        }
    }

    @Test
    fun readableComplex() {
        parseAPLExpressionWithOutput(":read io:print 1J2").let { (result, out) ->
            assertEquals(Complex(1.0, 2.0), result.ensureNumber().asComplex())
            assertEquals("1.0J2.0", out)
        }
    }

    @Test
    fun readCharsAsString() {
        val result = parseAPLExpression("@a @b")
        assertString("ab", result)
    }

    @Test
    fun nonBmpCharsInString() {
        val result = parseAPLExpression("\"\uD835\uDC9F\"")
        assertDimension(dimensionsOfSize(1), result)
        assertString("\uD835\uDC9F", result)
    }

    @Test
    fun nonBmpExplicitCharacter() {
        val result = parseAPLExpression("@\uD835\uDC9F")
        val v = result.unwrapDeferredValue()
        assertTrue(v is APLChar)
        assertEquals("\uD835\uDC9F", v.asString())
    }

    @Test
    fun formatNumber() {
        assertString("8", parseAPLExpression("⍕8"))
        assertString("100", parseAPLExpression("⍕100"))
        assertString("-100", parseAPLExpression("⍕¯100"))
    }

    @Test
    fun formatCharacter() {
        assertString("a", parseAPLExpression("⍕@a"))
        assertString("⍬", parseAPLExpression("⍕@⍬"))
    }

    @Test
    fun formatNull() {
        assertString("", parseAPLExpression("⍕⍬"))
    }

    @Test
    fun formatSelfString() {
        assertString("foo bar", parseAPLExpression("⍕\"foo bar\""))
    }

    @Test
    fun parseIntegerTest() {
        assertSimpleNumber(123, parseAPLExpression("⍎\"123\""))
        assertSimpleNumber(-10, parseAPLExpression("⍎\"-10\""))
        assertSimpleNumber(30, parseAPLExpression("⍎\"  30\""))
        assertSimpleNumber(30, parseAPLExpression("⍎\"30   \""))
        assertSimpleNumber(30, parseAPLExpression("⍎\"  30   \""))
    }

    @Test
    fun parseBigint() {
        assertBigIntOrLong("123456789012345678901234567890123456", parseAPLExpression("⍎\"123456789012345678901234567890123456\""))
        assertBigIntOrLong("-987654321098765432109876543210123456", parseAPLExpression("⍎\"-987654321098765432109876543210123456\""))
    }

    @Test
    fun parseDoubleTest() {
        assertNearDouble(NearDouble(10.1, 4), parseAPLExpression("⍎\"10.1\""))
        assertNearDouble(NearDouble(-4.5, 4), parseAPLExpression("⍎\"-4.5\""))
        assertNearDouble(NearDouble(-0.3, 4), parseAPLExpression("⍎\"-.3\""))
        assertNearDouble(NearDouble(-3.0, 4), parseAPLExpression("⍎\"-3.\""))
        assertNearDouble(NearDouble(40.0, 4), parseAPLExpression("⍎\"  40.0\""))
        assertNearDouble(NearDouble(40.0, 4), parseAPLExpression("⍎\"40.0\""))
        assertNearDouble(NearDouble(40.0, 4), parseAPLExpression("⍎\"  40.0   \""))
    }

    @Test
    fun parseNumberErrorTest() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍎\"illegal\"")
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍎\"\"")
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍎\".\"")
        }

        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍎\"10 11\"")
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍎\"- 1\"")
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍎\"--1\"")
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍎\"1.2.3\"")
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍎\"1..\"")
        }
    }

    @Test
    fun parseNumberFailsWithArrayInput() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍎1 2 3")
        }
    }

    @Test
    fun parseNumberFailsWithIllegalTypeInt() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍎1")
        }
    }

    @Test
    fun parseNumberFailsWithIllegalTypeCharacter() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍎@a")
        }
    }

    @Test
    fun parseNumberFailsWithIllegalTypeMap() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍎map 1 2")
        }
    }

    @Test
    fun charDifference() {
        parseAPLExpression("\"bBa\" - \"aAb\"").let { result ->
            assert1DArray(arrayOf(1, 1, -1), result)
        }
    }

    @Test
    fun additionOfCharsNotAllowed() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("@a + @A")
        }
    }

    @Test
    fun charPlusInt() {
        parseAPLExpression("\"af\" + 1 ¯1").let { result ->
            assertString("be", result)
        }
    }

    @Test
    fun intPlusChar() {
        parseAPLExpression("8 ¯1 + \"af\"").let { result ->
            assertString("ie", result)
        }
    }

    @Test
    fun charMinusInt() {
        parseAPLExpression("\"abj\" - 0 ¯11 3").let { result ->
            assertString("amg", result)
        }
    }

    @Test
    fun intMinusCharIsNotAllowed() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("98 200 - \"aj\"")
        }
    }

    @Test
    fun charPlusComplexShouldFail() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("@a + 1j1")
        }
    }

    @Test
    fun complexPlusCharShouldFail() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("1j1 + @a")
        }
    }

    @Test
    fun charMinusComplexShouldFail() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("@a - 1j1")
        }
    }

    @Test
    fun charPlusDouble() {
        parseAPLExpression("\"abc\" + 0.1 0.9 6.2").let { result ->
            assertString("abi", result)
        }
    }

    @Test
    fun doublePlusChar() {
        parseAPLExpression("0.2 11.9 1.1 + \"abc\"").let { result ->
            assertString("amd", result)
        }
    }

    @Test
    fun negativeCharactersNotAllowed0() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("@a - 98")
        }
    }
}
