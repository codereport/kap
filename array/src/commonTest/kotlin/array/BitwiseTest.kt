package array

import kotlin.test.Test
import kotlin.test.assertFailsWith

class BitwiseTest : APLTest() {
    @Test
    fun unsupportedBitwise() {
        assertFailsWith<BitwiseNotSupported> {
            parseAPLExpression("1 ⋆∵ 1")
        }
    }

    @Test
    fun bitwiseAnd() {
        assertSimpleNumber(66, parseAPLExpression("99 ∧∵ 70"))
        assertBigIntOrLong(66, parseAPLExpression("(int:asBigint 99) ∧∵ (int:asBigint 70)"))
        assertBigIntOrLong(
            "154742504910672534362390528",
            parseAPLExpression("309485009821327476538736640 ∧∵ 154742504910672551542259715"))
    }

    @Test
    fun andOnIllegalArgumentsShouldFail() {
        assertFailsWith<APLIncompatibleDomainsException> {
            parseAPLExpression("4.0 ∧∵ 2")
        }
        assertFailsWith<APLIncompatibleDomainsException> {
            parseAPLExpression("4 ∧∵ 2.0")
        }
        assertFailsWith<APLIncompatibleDomainsException> {
            parseAPLExpression("(4÷3) ∧∵ 2")
        }
        assertFailsWith<APLIncompatibleDomainsException> {
            parseAPLExpression("5 ∧∵ (2÷3)")
        }
    }

    @Test
    fun andOnReducedRational() {
        assertBigIntOrLong(2, parseAPLExpression("3 ∧∵ (8÷5)+(2÷5)"))
    }

    @Test
    fun bitwiseAndArray() {
        parseAPLExpression("99 ∧∵ 70 191 100 9").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(66, 35, 96, 1), result)
        }
    }

    @Test
    fun bitwiseAndArrayBothArgs() {
        parseAPLExpression("1 2 4 ∧∵ 10 11 12").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(0, 2, 4), result)
        }
    }

    @Test
    fun bitwiseAndWithMul() {
        assertSimpleNumber(66, parseAPLExpression("99 ×∵ 70"))
    }

    @Test
    fun bitwiseOr() {
        assertSimpleNumber(103, parseAPLExpression("99 ∨∵ 70"))
        assertBigIntOrLong(103, parseAPLExpression("(int:asBigint 99) ∨∵ (int:asBigint 70)"))
    }

    @Test
    fun bitwiseXorWithNotEquals() {
        assertSimpleNumber(37, parseAPLExpression("99 ≠∵ 70"))
        assertBigIntOrLong(37, parseAPLExpression("(int:asBigint 99) ≠∵ (int:asBigint 70)"))
    }

    @Test
    fun bitwiseXorWithAdd() {
        assertSimpleNumber(37, parseAPLExpression("99 +∵ 70"))
    }

    @Test
    fun bitwiseXorWithSub() {
        assertSimpleNumber(37, parseAPLExpression("99 -∵ 70"))
    }

    @Test
    fun bitwiseNot() {
        assertSimpleNumber(-203, parseAPLExpression("~∵ 202"))
        assertSimpleNumber(0, parseAPLExpression("~∵ ¯1"))
        assertSimpleNumber(-1, parseAPLExpression("~∵ 0"))
        assertBigIntOrLong("-10000000000000000000000000000001", parseAPLExpression("~∵ 10000000000000000000000000000000"))
    }

    @Test
    fun bitwiseNand() {
        assertSimpleNumber(-67, parseAPLExpression("99 ⍲∵ 70"))
        assertBigIntOrLong(-67, parseAPLExpression("(int:asBigint 99) ⍲∵ (int:asBigint 70)"))
    }

    @Test
    fun bitwiseNor() {
        assertSimpleNumber(-104, parseAPLExpression("99 ⍱∵ 70"))
        assertBigIntOrLong(-104, parseAPLExpression("(int:asBigint 99) ⍱∵ (int:asBigint 70)"))
    }
}
