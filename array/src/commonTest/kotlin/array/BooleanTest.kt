package array

import kotlin.test.Test

class BooleanTest : APLTest() {
    @Test
    fun simpleBoolean() {
        parseAPLExpression("toBoolean¨ 1 1.0 0 0.0").let { result ->
            assert1DArray(arrayOf(1, 1, 0, 0), result)
        }
    }

    @Test
    fun extendedRangeBooleans() {
        parseAPLExpression("toBoolean¨ 2 4 100 100000000000000000000000000000000000000000 ¯1 ¯10000 1.1 ¯1.1 0.1").let { result ->
            assert1DArray(arrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1), result)
        }
    }

    @Test
    fun bigIntBoolean() {
        parseAPLExpression("toBoolean¨ (int:asBigint 1) (int:asBigint 0) (int:asBigint 1000) (int:asBigint ¯1)").let { result ->
            assert1DArray(arrayOf(1, 0, 1, 1), result)
        }
    }

    @Test
    fun rationalBoolean() {
        parseAPLExpression("toBoolean¨ (1÷2) (1÷100000000000000000000000000000000000000000000000) (10÷9)").let { result ->
            assert1DArray(arrayOf(1, 1, 1), result)
        }
    }

    @Test
    fun arrayBoolean() {
        parseAPLExpression("toBoolean¨ (0 0 0) (,0) (⊂,0) ⍬ (⍬ ⍬) (1 1 ⍴ 0)").let { result ->
            assert1DArray(arrayOf(1, 1, 1, 1, 1, 1), result)
        }
    }

    @Test
    fun arrayChar() {
        parseAPLExpression("toBoolean¨ @\\0 @a @\\s").let { result ->
            assert1DArray(arrayOf(1, 1, 1), result)
        }
    }
}
