package array

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
}
