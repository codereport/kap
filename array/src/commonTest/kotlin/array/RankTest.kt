package array

import kotlin.test.Test

class RankTest : APLTest() {
    @Test
    fun rank0Test() {
        parseAPLExpression("({100+⍵}⍤0) ⍳10").let { result ->
            assertDimension(dimensionsOfSize(10), result)
            assertArrayContent(arrayOf(100, 101, 102, 103, 104, 105, 106, 107, 108, 109), result)
        }
    }

    @Test
    fun rank0Test2() {
        parseAPLExpression("({100,9+⍵}⍤0) ⍳10").let { result ->
            assertDimension(dimensionsOfSize(10, 2), result)
            assertArrayContent(arrayOf(100, 9, 100, 10, 100, 11, 100, 12, 100, 13, 100, 14, 100, 15, 100, 16, 100, 17, 100, 18), result)
        }
    }

    @Test
    fun rank1Test() {
        parseAPLExpression("({100,9+⍵}⍤1) ⍳10").let { result ->
            assertDimension(dimensionsOfSize(11), result)
            assertArrayContent(arrayOf(100, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18), result)
        }
    }

    @Test
    fun rank1Test2Dimension() {
        parseAPLExpression("({100,9+⍵}⍤1) 2 3 ⍴ ⍳10").let { result ->
            assertDimension(dimensionsOfSize(2, 4), result)
            assertArrayContent(arrayOf(100, 9, 10, 11, 100, 12, 13, 14), result)
        }
    }

    @Test
    fun aplContribExample0() {
        assertSimpleNumber(1, parseAPLExpression("(1 2⍴(0 1 2)(3 4 5))≡(⊂⍤1) 1 2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample1() {
        assertSimpleNumber(1, parseAPLExpression("(1 2⍴(0 1 2)(3 4 5))≡(⊂⍤¯2)1 2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample2() {
        assertSimpleNumber(1, parseAPLExpression("(,⊂2 3⍴⍳6)≡(⊂⍤2) 1 2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample3() {
        assertSimpleNumber(1, parseAPLExpression("(,⊂2 3⍴⍳6)≡(⊂⍤¯1) 1 2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample4() {
        assertSimpleNumber(1, parseAPLExpression("(2 2⍴(⍳4)(⍳3)(4 5 6 7)(3 4 5))≡(2 4⍴⍳8) ({⍺ ⍵}⍤¯1) 2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample5() {
        assertSimpleNumber(1, parseAPLExpression("(2 2⍴(⍳4)0(4 5 6 7)1) ≡ (2 4⍴⍳8) ({⍺ ⍵}⍤¯1) ⍳2"))
    }

    @Test
    fun aplContribExample6() {
        assertSimpleNumber(1, parseAPLExpression("(2 2⍴0(⍳4)1(4 5 6 7)) ≡ (2 4⍴⍳8) ({⍺ ⍵}⍤¯1)⍨ ⍳2"))
    }

    @Test
    fun aplContribExample7() {
        assertSimpleNumber(1, parseAPLExpression("(2 2⍴0(⍳4)1(4 5 6 7)) ≡ (2 4⍴⍳8) ({⍺ ⍵}⍤0 1)⍨ ⍳2"))
    }

    @Test
    fun aplContribExample8() {
        assertSimpleNumber(1, parseAPLExpression("(2 2⍴0(⍳4)1(4 5 6 7)) ≡ (2 4⍴⍳8) ({⍺ ⍵}⍤ 9 0 1)⍨ ⍳2"))
    }

    @Test
    fun aplContribExample9() {
        assertSimpleNumber(1, parseAPLExpression("(2 2⍴0(⍳4)1(4 5 6 7)) ≡ (2 4⍴⍳8) ({⍺ ⍵}⍤¯9 0 1)⍨ ⍳2"))
    }

    @Test
    fun aplContribExample10() {
        assertSimpleNumber(1, parseAPLExpression("(,⊂2 3⍴⍳6)≡(⊂⍤ 9 2) 1 2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample11() {
        assertSimpleNumber(1, parseAPLExpression("(,⊂2 3⍴⍳6)≡(⊂⍤¯9 2) 1 2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample12() {
        assertSimpleNumber(1, parseAPLExpression("(,⊂2 3⍴⍳6)≡(⊂⍤2 9 9) 1 2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample13() {
        assertSimpleNumber(1, parseAPLExpression("(,⊂2 3⍴⍳6)≡(⊂⍤2 ¯9 ¯9) 1 2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample14() {
        assertSimpleNumber(1, parseAPLExpression("(2 3⍴0 1 2 7 8 9)≡0 4(+⍤¯1)2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample15() {
        assertSimpleNumber(1, parseAPLExpression("(2 3⍴0 1 2 7 8 9)≡0 4(+⍤0 1)2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample16() {
        assertSimpleNumber(1, parseAPLExpression("(2 3⍴0 1 2 7 8 9)≡0 4(+⍤¯9 0 1)2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample17() {
        assertSimpleNumber(1, parseAPLExpression("(2 3⍴0 1 2 7 8 9)≡0 4(+⍤ 9 0 1)2 3⍴⍳6"))
    }

    @Test
    fun aplContribExample18() {
        assertSimpleNumber(1, parseAPLExpression("(2 3 2⍴(12⍴0 4)+2/⍳6)≡0 4(+⍤1 0)2 3⍴⍳6"))
    }

    /*
(1 2⍴(1 2 3)(4 5 6))≡⊂⍤1⊢1 2 3⍴⍳6
(1 2⍴(1 2 3)(4 5 6))≡⊂⍤¯2⊢1 2 3⍴⍳6
(,⊂2 3⍴⍳6)≡⊂⍤2⊢1 2 3⍴⍳6
(,⊂2 3⍴⍳6)≡⊂⍤¯1⊢1 2 3⍴⍳6

(2 2⍴(⍳4)(⍳3)(4 5 6 7)(3 4 5))≡(2 4⍴⍳8) ({⍺⍵}⍤¯1) 2 3⍴⍳6
(2 2⍴(⍳4)0(4 5 6 7)1) ≡ (2 4⍴⍳8) ({⍺⍵}⍤¯1) ⍳2
(2 2⍴0(⍳4)1(4 5 6 7)) ≡ (2 4⍴⍳8) ({⍺⍵}⍤¯1)⍨ ⍳2
(2 2⍴0(⍳4)1(4 5 6 7)) ≡ (2 4⍴⍳8) ({⍺⍵}⍤   0 1)⍨ ⍳2
(2 2⍴0(⍳4)1(4 5 6 7)) ≡ (2 4⍴⍳8) ({⍺⍵}⍤ 9 0 1)⍨ ⍳2
(2 2⍴0(⍳4)1(4 5 6 7)) ≡ (2 4⍴⍳8) ({⍺⍵}⍤¯9 0 1)⍨ ⍳2

(,⊂2 3⍴⍳6)≡⊂⍤ 9 2⊢1 2 3⍴⍳6
(,⊂2 3⍴⍳6)≡⊂⍤¯9 2⊢1 2 3⍴⍳6
(,⊂2 3⍴⍳6)≡⊂⍤2  9  9⊢1 2 3⍴⍳6
(,⊂2 3⍴⍳6)≡⊂⍤2 ¯9 ¯9⊢1 2 3⍴⍳6

(2 3⍴0 1 2 7 8 9)≡0 4(+⍤¯1)2 3⍴⍳6
(2 3⍴0 1 2 7 8 9)≡0 4(+⍤   0 1)2 3⍴⍳6
(2 3⍴0 1 2 7 8 9)≡0 4(+⍤¯9 0 1)2 3⍴⍳6
(2 3⍴0 1 2 7 8 9)≡0 4(+⍤ 9 0 1)2 3⍴⍳6
(2 3 2⍴(12⍴0 4)+2/⍳6)≡0 4(+⍤1 0)2 3⍴⍳6
     */

    /**
     * This error was found after a change to scalar function evaluation, and was triggered by
     * the KAP implementation of decode.
     */
    @Test
    fun fromDecodeImpl() {
        parseAPLExpression("6 3 2 (|⍤1) 15 2 0").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(3, 2, 0), result)
        }
    }

    @Test
    fun simple2DRank() {
        parseAPLExpression("({+/⍵}⍤1) 2 2⍴⍳4").let { result ->
            assert1DArray(arrayOf(1, 5), result)
        }
    }

    @Test
    fun combinedDimensions() {
        parseAPLExpression("2 (,⍤1) 3 3⍴⍳10").let { result ->
            assertDimension(dimensionsOfSize(3, 4), result)
            assertArrayContent(
                arrayOf(
                    2, 0, 1, 2,
                    2, 3, 4, 5,
                    2, 6, 7, 8),
                result)
        }
    }

    @Test
    fun rankWithLargeValue() {
        parseAPLExpression("(⊂⍤3) 1 2 3 4").let { result ->
            assertDimension(emptyDimensions(), result)
            val v = result.valueAt(0)
            assert1DArray(arrayOf(1, 2, 3, 4), v)
        }
    }
}
