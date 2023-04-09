package array

import array.complex.Complex
import com.dhsdevelopments.mpbignum.BigInt
import com.dhsdevelopments.mpbignum.of
import kotlin.test.Test

class KapBigintTest : APLTest() {
    @Test
    fun addToBigint() {
        parseAPLExpression("9223372036854775807 + 1").let { result ->
            assertAPLValue(InnerBigIntOrLong(BigInt.of("9223372036854775808")), result)
        }
    }

    @Test
    fun addLongArray() {
        parseAPLExpression("1 + 10 9223372036854775807").let { result ->
            assert1DArray(
                arrayOf(
                    InnerBigIntOrLong(BigInt.of(11)),
                    InnerBigIntOrLong(BigInt.of("9223372036854775808"))),
                result)
        }
    }

    @Test
    fun addBigintToComplex() {
        parseAPLExpression("(int:asBigint 2) + 2J6").let { result ->
            assertSimpleComplex(Complex(4.0, 6.0), result)
        }
    }

    @Test
    fun addComplexToBigint() {
        parseAPLExpression("2J6 + (int:asBigint 2)").let { result ->
            assertSimpleComplex(Complex(4.0, 6.0), result)
        }
    }
}
