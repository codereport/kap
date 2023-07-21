package array

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertFailsWith

class EncodeTest : APLTest() {
    @Test
    fun simpleEncode() {
        assertSimpleNumber(123, parseAPLExpression("10 ⊥ 1 2 3", true))
    }

    @Test
    fun oneDigitEncode() {
        assertSimpleNumber(2, parseAPLExpression("10 ⊥ 2", true))
    }

    @Test
    fun encodeEmptyArray() {
        assertSimpleNumber(0, parseAPLExpression("9 ⊥ ⍬", true))
    }

    @Test
    fun encodeSingleDigitOverflow() {
        assertSimpleNumber(33, parseAPLExpression("10 ⊥ 33", true))
    }

    @Test
    fun encodeOverflowInElement() {
        assertSimpleNumber(433, parseAPLExpression("10 ⊥ 1 33 3", true))
    }

    @Test
    fun differentBases() {
        assertSimpleNumber(26, parseAPLExpression("2 4 5 ⊥ 1 1 1", true))
    }

    @Test
    fun floatingPointArguments() {
        parseAPLExpression("10 ⊥ 2 3.1 3.1", true).let { result ->
            assertDoubleWithRange(Pair(234.099, 234.101), result)
        }
    }

    @Test
    fun mismatchedSizes() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("2 2 2 ⊥ 1 1 0 1 0", true)
        }
    }

    @Test
    fun invalidDimensions0() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(2 2 ⍴ 2) ⊥ 1 1 1 1", true)
        }
    }

    @Test
    fun invalidDimensions1() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("(2 2 ⍴ 2) ⊥ 2 2 ⍴ 1 1 1 1", true)
        }
    }

    @Test
    fun invalidDimensions2() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("2 2 2 2 ⊥ 2 2 ⍴ 1 1 1 1", true)
        }
    }

    @Test
    fun encodeInvalidTypeSingleValue() {
        assertFailsWith<IncompatibleTypeException> {
            parseAPLExpression("10 ⊥ @a", true)
        }
    }

    @Test
    fun encodeInvalidTypeArrayMember() {
        assertFailsWith<IncompatibleTypeException> {
            parseAPLExpression("10 ⊥ 9 @a", true)
        }
    }

    // Decode

    @Test
    fun decodeSimple() {
        parseAPLExpression("(3⍴2) ⊤ 3", true).let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(InnerDoubleOrLong(0.0), InnerDoubleOrLong(1.0), InnerDoubleOrLong(1.0)), result)
        }
    }

    @Test
    fun decodeOversized() {
        parseAPLExpression("(2⍴2) ⊤ 7", true).let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(InnerDoubleOrLong(1.0), InnerDoubleOrLong(1.0)), result)
        }
    }

    @Test
    fun decodeDifferentValues() {
        parseAPLExpression("2 3 6 ⊤ 15", true).let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(InnerDoubleOrLong(0.0), InnerDoubleOrLong(2.0), InnerDoubleOrLong(3.0)), result)
        }
    }

    @Test
    fun decodeSingleValue() {
        parseAPLExpression("3 ⊤ 7", true).let { result ->
            assertSimpleNumber(1, result)
        }
    }

    @Test
    fun decodeZero() {
        parseAPLExpression("2 3 6 ⊤ 0", true).let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(0, 0, 0), result)
        }
    }

    @Test
    fun decodeOutsideRangeOfInteger() {
        parseAPLExpression("(40⍴10) ⊤ 12", true).let { result ->
            assertDimension(dimensionsOfSize(40), result)
            assertArrayContent(
                arrayOf(
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2),
                result)
        }
    }

    @Ignore
    @Test
    fun decodeNegative() {
        parseAPLExpression("(10⍴10) ⊤ ¯10", true).let { result ->
            assert1DArray(arrayOf(9, 9, 9, 9, 9, 9, 9, 9, 0), result)
        }
    }

    @Test
    fun decodeBigInt() {
        parseAPLExpression("(100⍴10) ⊤ 10000000000000000000000000000000000000000", true).let { result ->
            val expected = Array(100) { i -> if (i == 59) 1 else 0 }
            assert1DArray(expected, result)
        }
    }

    @Ignore
    @Test
    fun decodeNegativeBigInt() {
        parseAPLExpression("(40⍴10) ⊤ ¯123456789012345678901234567890", true).let { result ->
            assert1DArray(
                arrayOf(
                    9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 9,
                    8, 7, 6, 5, 4, 3, 2, 1, 0, 9, 8, 7, 6, 5, 4, 3, 2, 1, 1, 0),
                result)
        }
    }

    @Ignore
    @Test
    fun decodeMultiDimensional() {
        parseAPLExpression("(2⍴2) ⊤ 2 3 ⍴ 10+⍳6", true).let { result ->
            assertDimension(dimensionsOfSize(2, 2, 3), result)
            assertArrayContent(arrayOf(1, 1, 0, 0, 1, 1, 0, 1, 0, 1, 0, 1), result)
        }
    }
}
