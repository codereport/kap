package array

import kotlin.test.Test
import kotlin.test.assertFailsWith

class InverseFnTest : APLTest() {
    @Test
    fun negateInverse() {
        parseAPLExpression("(-inverse) 10").let { result ->
            assertSimpleNumber(-10, result)
        }
    }

    @Test
    fun reciprocalInverse() {
        parseAPLExpression("÷inverse 8").let { result ->
            assertSimpleDouble(0.125, result)
        }
    }

    @Test
    fun leftAssignedInverse0() {
        parseAPLExpression("((1+)inverse) 8").let { result ->
            assertSimpleNumber(7, result)
        }
    }

    @Test
    fun leftAssignedInverse1() {
        parseAPLExpression("1 +inverse 8").let { result ->
            assertSimpleNumber(7, result)
        }
    }

    @Test
    fun leftAssignedInverseTwoArgFails() {
        assertFailsWith<Unimplemented2ArgException> {
            parseAPLExpression("7 ((1+)inverse) 8")
        }
    }

    @Test
    fun inverseFunctionFailsWith2Arg() {
        assertFailsWith<Unimplemented2ArgException> {
            parseAPLExpression("11 (-inverse) 10")
        }
    }
}
