package array.optimiser

import array.*
import com.dhsdevelopments.mpbignum.BigInt
import com.dhsdevelopments.mpbignum.compareTo
import com.dhsdevelopments.mpbignum.of
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FloorDivOptimiserTest : APLTest() {
    @Test
    fun testStandardCallChainOptimisationWorks() {
        val engine = Engine()
        val instr = engine.parse(StringSourceLocation("⌊10÷4"))
        assertIs<FunctionCall2Arg>(instr)
        assertIs<MergedFloorDivFunction>(instr.fn)
    }

    @Test
    fun testChainedCallOptimisationWorks() {
        val engine = Engine()
        val instr = engine.parse(StringSourceLocation("10(⌊÷)4"))
        assertIs<FunctionCall2Arg>(instr)
        assertIs<MergedFloorDivFunction>(instr.fn)
    }

    @Test
    fun optimiseFloorDiv() {
        parseAPLExpression("⌊5÷2").let { result ->
            assertSimpleNumber(2, result)
        }
    }

    @Test
    fun optimiseFloorDivAs2Chain() {
        parseAPLExpression("5(⌊÷)2").let { result ->
            assertSimpleNumber(2, result)
        }
    }

    @Test
    fun optimiseFloorDivNeg() {
        parseAPLExpression("⌊¯5÷2").let { result ->
            assertSimpleNumber(-3, result)
        }
    }

    @Test
    fun optimiseFloorDivNegAs2Chain() {
        parseAPLExpression("¯5(⌊÷)2").let { result ->
            assertSimpleNumber(-3, result)
        }
    }

    @Test
    fun optimiseFloorDivWithNestedArg() {
        parseAPLExpression("⌊(1 2 3) (4 5) (6 7 8) ÷ 2").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            result.valueAt(0).let { v ->
                assert1DArray(arrayOf(0, 1, 1), v)
            }
            result.valueAt(1).let { v ->
                assert1DArray(arrayOf(2, 2), v)
            }
            result.valueAt(2).let { v ->
                assert1DArray(arrayOf(3, 3, 4), v)
            }
        }
    }

    @Test
    fun optimiseFloorDivWithNestedArgAs2Chain() {
        parseAPLExpression("(1 2 3) (4 5) (6 7 8) (⌊÷) 2").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            result.valueAt(0).let { v ->
                assert1DArray(arrayOf(0, 1, 1), v)
            }
            result.valueAt(1).let { v ->
                assert1DArray(arrayOf(2, 2), v)
            }
            result.valueAt(2).let { v ->
                assert1DArray(arrayOf(3, 3, 4), v)
            }
        }
    }


    @Test
    fun optimiseFloorDivWithMultiRightArg() {
        parseAPLExpression("⌊10 ÷ 1 2 3 4 5 6 7 8 9").let { result ->
            assert1DArray(arrayOf(10, 5, 3, 2, 2, 1, 1, 1, 1), result)
        }
    }

    @Test
    fun optimiseFloorDivWithMultiRightArgAs2Chain() {
        parseAPLExpression("10 (⌊÷) 1 2 3 4 5 6 7 8 9").let { result ->
            assert1DArray(arrayOf(10, 5, 3, 2, 2, 1, 1, 1, 1), result)
        }
    }

    @Test
    fun optimiseWithArrayArgs() {
        val src =
            """
            |⌊1 2 3 4 100 101 200 2001 (2⋆200) `
            |(1+2⋆200) 0 1.0 1.2 2.0 3.0 `
            |¯1 ¯2 ¯3 ¯4 ¯100 ¯101 (-2⋆200) (-1+2⋆200) `
            |¯1.0 ¯1.2 ¯2.0 ¯3.0 ¯100000000.0 ¯100000000.1 `
            |¯100000001 (201÷7) (¯101÷3) 9223372036854775807 9223372036854775806 `
            |¯9223372036854775808 ¯9223372036854775807 ÷ 2
            """.trimMargin()
        parseAPLExpression(src).let { result ->
            assert1DArray(
                arrayOf(
                    0, 1, 1, 2, 50, 50, 100, 1000,
                    InnerBigIntOrLong(BigInt.of("803469022129495137770981046170581301261101496891396417650688")),
                    InnerBigIntOrLong(BigInt.of("803469022129495137770981046170581301261101496891396417650688")),
                    0, 0, 0, 1, 1,
                    -1, -1, -2, -2, -50, -51,
                    InnerBigIntOrLong(BigInt.of("-803469022129495137770981046170581301261101496891396417650688")),
                    InnerBigIntOrLong(BigInt.of("-803469022129495137770981046170581301261101496891396417650689")),
                    -1, -1, -1, -2, -50000000, -50000001, -50000001, 14, -17,
                    4611686018427387903L, 4611686018427387903L, -4611686018427387904L, -4611686018427387904L),
                result)
        }
    }

    @Test
    fun optimiseWithArrayArgsAs2Chain() {
        val src =
            """
            |1 2 3 4 100 101 200 2001 (2⋆200) `
            |(1+2⋆200) 0 1.0 1.2 2.0 3.0 `
            |¯1 ¯2 ¯3 ¯4 ¯100 ¯101 (-2⋆200) (-1+2⋆200) `
            |¯1.0 ¯1.2 ¯2.0 ¯3.0 ¯100000000.0 ¯100000000.1 `
            |¯100000001 (201÷7) (¯101÷3) 9223372036854775807 9223372036854775806 `
            |¯9223372036854775808 ¯9223372036854775807 (⌊÷) 2
            """.trimMargin()
        parseAPLExpression(src).let { result ->
            assert1DArray(
                arrayOf(
                    0, 1, 1, 2, 50, 50, 100, 1000,
                    InnerBigIntOrLong(BigInt.of("803469022129495137770981046170581301261101496891396417650688")),
                    InnerBigIntOrLong(BigInt.of("803469022129495137770981046170581301261101496891396417650688")),
                    0, 0, 0, 1, 1,
                    -1, -1, -2, -2, -50, -51,
                    InnerBigIntOrLong(BigInt.of("-803469022129495137770981046170581301261101496891396417650688")),
                    InnerBigIntOrLong(BigInt.of("-803469022129495137770981046170581301261101496891396417650689")),
                    -1, -1, -1, -2, -50000000, -50000001, -50000001, 14, -17,
                    4611686018427387903L, 4611686018427387903L, -4611686018427387904L, -4611686018427387904L),
                result)
        }
    }

    @Test
    fun optimiseLargeDouble() {
        parseAPLExpression("⌊1e90 ÷ 2").let { result ->
            assertIs<APLBigInt>(result)
            val v = result.value
            assertTrue(
                v >= BigInt.of("499999999999999983242056357731950024912593046310062751489837298654589877718689615343255550") &&
                        v <= BigInt.of("500000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"))
        }
    }

    @Test
    fun optimiseWithSpecialisedArrayLong() {
        parseAPLExpression("⌊1 2 3 4 100 101 200 2001 ¯1 ¯2 ¯3 ¯4 ¯100 ¯101 ÷ 2").let { result ->
            assert1DArray(arrayOf(0, 1, 1, 2, 50, 50, 100, 1000, -1, -1, -2, -2, -50, -51), result)
        }
    }

    @Test
    fun optimiseWithSpecialisedArrayDouble() {
        parseAPLExpression("⌊ 0.0 1.0 1.2 2.0 3.0 ¯1.0 ¯5.0 ¯10.5 10000000.1 ¯123456.12 ¯0.0001 1234567.0 ÷ 2.0").let { result ->
            assert1DArray(arrayOf(0, 0, 0, 1, 1, -1, -3, -6, 5000000, -61729, -1, 617283), result)
        }
    }

    @Test
    fun optimiseWithLongSpecialisedTypeShouldBeInt() {
        parseAPLExpression("⌊ 1 2 3 4 5 ÷ 2", collapse = false).let { result ->
            assertSame(ArrayMemberType.LONG, result.specialisedType)
        }
    }

    @Test
    fun optimiseWithSmallestLong() {
        parseAPLExpression("⌊¯9223372036854775808÷¯1").let { result ->
            assertBigIntOrLong("9223372036854775808", result)
        }
    }
}
