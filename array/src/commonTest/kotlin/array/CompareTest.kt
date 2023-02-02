package array

import kotlin.test.Test

class CompareTest : APLTest() {
    @Test
    fun testComparators() {
        testFunction(arrayOf(1, 0, 0, 1, 0, 0), "<")
        testFunction(arrayOf(0, 1, 0, 0, 1, 0), ">")
        testFunction(arrayOf(1, 0, 1, 1, 0, 1), "≤")
        testFunction(arrayOf(0, 1, 1, 0, 1, 1), "≥")
        testFunction(arrayOf(0, 0, 1, 0, 0, 1), "=")
    }

    @Test
    fun testIdenticalSimple() {
        assertSimpleNumber(1, parseAPLExpression("10≡10"))
        assertSimpleNumber(0, parseAPLExpression("10≡100"))
        assertSimpleNumber(1, parseAPLExpression("10 11≡10 11"))
        assertSimpleNumber(1, parseAPLExpression("11 12 13 14 15 16 17≡10+1 2 3 4 5 6 7"))
        assertSimpleNumber(0, parseAPLExpression("1 2 3≡1 2"))
        assertSimpleNumber(1, parseAPLExpression("(1 2) (3 4 (5 6)) ≡ (1 2) (3 4 (5 6))"))
        assertSimpleNumber(0, parseAPLExpression("(1 2) (3 4 (5 6)) ≡ (1 2) (3 4 (@a 6))"))
        assertSimpleNumber(1, parseAPLExpression("@a ≡ @a"))
        assertSimpleNumber(1, parseAPLExpression("1.2 ≡ 1.2"))
        assertSimpleNumber(0, parseAPLExpression("2J4 ≡ 2J3"))
    }

    @Test
    fun testIdenticalLazyValueLong() {
        assertSimpleNumber(1, parseAPLExpression("0≡0⌷0 1"))
    }

    @Test
    fun testIdenticalLazyValueDouble() {
        assertSimpleNumber(1, parseAPLExpression("0.0≡0⌷0.0 1.0"))
    }

    @Test
    fun testIdenticalLazyValueComplex() {
        assertSimpleNumber(1, parseAPLExpression("3J1≡0⌷3J1 4J1"))
    }

    @Test
    fun testIdenticalLazyValueChar() {
        assertSimpleNumber(1, parseAPLExpression("@a≡0⌷@a @b"))
    }

    @Test
    fun testNotIdentical() {
        assertSimpleNumber(0, parseAPLExpression("10≢10"))
        assertSimpleNumber(1, parseAPLExpression("10≢100"))
        assertSimpleNumber(0, parseAPLExpression("10 11≢4+6 7"))
        assertSimpleNumber(0, parseAPLExpression("11 12 13 14 15 16 17≢10+1 2 3 4 5 6 7"))
        assertSimpleNumber(1, parseAPLExpression("1 2 3≢1 2"))
        assertSimpleNumber(0, parseAPLExpression("\"foo\"≢\"foo\""))
        assertSimpleNumber(1, parseAPLExpression("(2 4 ⍴ 1 2 3 4 5 6 7 8) ≢ (4 2 ⍴ 1 2 3 4 5 6 7 8)"))
        assertSimpleNumber(1, parseAPLExpression("2 ≢ 1⍴2"))
        assertSimpleNumber(0, parseAPLExpression("'foo ≢ 'foo"))
        assertSimpleNumber(1, parseAPLExpression("@a ≢ @b"))
        assertSimpleNumber(0, parseAPLExpression("(1;2;3) ≢ (1;2;3)"))
    }

    @Test
    fun testNotIdenticalLazyValueLong() {
        assertSimpleNumber(0, parseAPLExpression("0≢0⌷0 1"))
    }

    @Test
    fun testNotIdenticalLazyValueDouble() {
        assertSimpleNumber(0, parseAPLExpression("0.0≢0⌷0.0 1.0"))
    }

    @Test
    fun testNotIdenticalLazyValueComplex() {
        assertSimpleNumber(0, parseAPLExpression("3J1≢0⌷3J1 4J1"))
    }

    @Test
    fun testNotIdenticalLazyValueChar() {
        assertSimpleNumber(0, parseAPLExpression("@a≢0⌷@a @b"))
    }

    @Test
    fun compareEqualsNonNumeric() {
        assertSimpleNumber(1, parseAPLExpression("@a = @a"))
        assertSimpleNumber(0, parseAPLExpression("@a = @b"))
        assertSimpleNumber(1, parseAPLExpression("('foo) = 'foo"))
        assertSimpleNumber(0, parseAPLExpression("('foo) = 'bar"))
    }

    @Test
    fun compareNotEqualsNonNumeric() {
        assertSimpleNumber(1, parseAPLExpression("'foo ≠ 'foox"))
        assertSimpleNumber(0, parseAPLExpression("'foo ≠ 'foo"))
        assertSimpleNumber(0, parseAPLExpression("@b ≠ @b"))
        assertSimpleNumber(1, parseAPLExpression("@a ≠ @b"))
        assertSimpleNumber(0, parseAPLExpression("(1;2;3) ≠ (1;2;3)"))
        assertSimpleNumber(1, parseAPLExpression("(1;2;3) ≠ (1;2;3;4)"))
    }

    private fun testFunction(expected: Array<Long>, name: String) {
        assertSimpleNumber(expected[0], parseAPLExpression("1${name}2"))
        assertSimpleNumber(expected[1], parseAPLExpression("2${name}1"))
        assertSimpleNumber(expected[2], parseAPLExpression("2${name}2"))
        assertSimpleNumber(expected[3], parseAPLExpression("0${name}1"))
        assertSimpleNumber(expected[4], parseAPLExpression("1${name}0"))
        assertSimpleNumber(expected[5], parseAPLExpression("0${name}0"))
    }

    @Test
    fun oneArgumentNotIdenticalTest() {
        assertSimpleNumber(0, parseAPLExpression("≢0⍴0"))
        assertSimpleNumber(0, parseAPLExpression("≢4"))
        assertSimpleNumber(4, parseAPLExpression("≢1 2 3 4"))
        assertSimpleNumber(1, parseAPLExpression("≢,4"))
        assertSimpleNumber(2, parseAPLExpression("≢2 3 ⍴ ⍳100"))
        assertSimpleNumber(8, parseAPLExpression("≢8 3 4 ⍴ ⍳100"))
        assertSimpleNumber(2, parseAPLExpression("≢(2 2 ⍴ ⍳4) (2 2 ⍴ ⍳4)"))
        assertSimpleNumber(5, parseAPLExpression("≢(1 2) (3 4) (5 6) (7 8) (9 10)"))
    }

    @Test
    fun oneArgumentIdenticalTest0() {
        assertSimpleNumber(1, parseAPLExpression("≡1 2 3"))
        assertSimpleNumber(0, parseAPLExpression("≡1"))
        assertSimpleNumber(2, parseAPLExpression("≡(1 2 3) (4 5 6)"))
        assertSimpleNumber(2, parseAPLExpression("≡(1 2 3) (10 11)"))
        assertSimpleNumber(2, parseAPLExpression("≡(1 2 3) (4 5 6) (100 200) (3000 4000)"))
        assertSimpleNumber(3, parseAPLExpression("≡(1 2 3) (4 5 6 (10 11))"))
        assertSimpleNumber(3, parseAPLExpression("≡((1 2) (3 4)) ((10 20) (30 40))"))
        assertSimpleNumber(1, parseAPLExpression("≡\"foo\""))
        assertSimpleNumber(2, parseAPLExpression("≡(1 2 ⍬)"))
    }

    @Test
    fun oneArgumentIdenticalWithHashMap() {
        val result = parseAPLExpression(
            """
            |a ← map 2 2 ⍴ 1 2 3 4
            |≡a
            """.trimMargin())
        assertSimpleNumber(0, result)
    }

    @Test
    fun oneArgumentIdenticalWithComplex() {
        assertSimpleNumber(0, parseAPLExpression("≡1J3"))
    }

    @Test
    fun oneArgumentIdenticalWithEnclosedArg0() {
        assertSimpleNumber(2, parseAPLExpression("≡⊂1 2 3"))
    }

    @Test
    fun oneArgumentIdenticalWithEnclosedArg1() {
        assertSimpleNumber(3, parseAPLExpression("≡⊂(1 2 3) (4 5 6)"))
    }

    @Test
    fun compareIntegers() {
        assertSimpleNumber(1, parseAPLExpression("3 cmp 1"))
        assertSimpleNumber(-1, parseAPLExpression("10 cmp 110"))
        assertSimpleNumber(0, parseAPLExpression("1 cmp 1"))
        assertSimpleNumber(0, parseAPLExpression("¯3 cmp ¯3"))
    }

    @Test
    fun compareDouble() {
        assertSimpleNumber(1, parseAPLExpression("3.0 cmp 1.0"))
        assertSimpleNumber(-1, parseAPLExpression("10.0 cmp 110.0"))
        assertSimpleNumber(0, parseAPLExpression("1.0 cmp 1.0"))
        assertSimpleNumber(0, parseAPLExpression("¯3.0 cmp ¯3.0"))
    }

    @Test
    fun compareIntWithDouble() {
        assertSimpleNumber(-1, parseAPLExpression("2.0 cmp 4"))
        assertSimpleNumber(-1, parseAPLExpression("2 cmp 4.0"))
        assertSimpleNumber(1, parseAPLExpression("9.0 cmp 1"))
        assertSimpleNumber(1, parseAPLExpression("9 cmp 1.0"))
        assertSimpleNumber(0, parseAPLExpression("9 cmp 9.0"))
        assertSimpleNumber(0, parseAPLExpression("9.0 cmp 9"))
    }

    @Test
    fun compareChars() {
        assertSimpleNumber(-1, parseAPLExpression("@a cmp @b"))
        assertSimpleNumber(1, parseAPLExpression("@b cmp @a"))
        assertSimpleNumber(0, parseAPLExpression("@b cmp @b"))
        assertSimpleNumber(1, parseAPLExpression("@a cmp @A"))
        assertSimpleNumber(0, parseAPLExpression("@a cmp @a"))
    }

    @Test
    fun compareStrings() {
        assertSimpleNumber(-1, parseAPLExpression("\"abc\" cmp \"bcd\""))
        assertSimpleNumber(-1, parseAPLExpression("\"ABC\" cmp \"abc\""))
        assertSimpleNumber(0, parseAPLExpression("⍬ cmp ⍬"))
        assertSimpleNumber(0, parseAPLExpression("\"a\" cmp \"a\""))
        assertSimpleNumber(-1, parseAPLExpression("\"abc\" cmp \"abcc\""))
    }

    @Test
    fun compareNestedStrings() {
        assertSimpleNumber(1, parseAPLExpression("\"abc\" \"def\" cmp \"abc\" \"cde\""))
        assertSimpleNumber(-1, parseAPLExpression("\"abc\" \"def\" cmp \"abc\" \"ghi\""))
    }

    @Test
    fun compareIntAndChar() {
        assertSimpleNumber(-1, parseAPLExpression("1 cmp @c"))
        assertSimpleNumber(1, parseAPLExpression("@c cmp 1"))
    }

    @Test
    fun compareIntAndComplex() {
        assertSimpleNumber(-1, parseAPLExpression("1 cmp 2J1"))
        assertSimpleNumber(1, parseAPLExpression("3 cmp 2J1"))
        assertSimpleNumber(-1, parseAPLExpression("2J1 cmp 3"))
        assertSimpleNumber(1, parseAPLExpression("3J1 cmp 3"))
        assertSimpleNumber(0, parseAPLExpression("1J0 cmp 1"))
        assertSimpleNumber(0, parseAPLExpression("1 cmp 1J0"))
    }

    @Test
    fun compareDoubleAndComplex() {
        assertSimpleNumber(-1, parseAPLExpression("2.1 cmp 2.2J5"))
        assertSimpleNumber(1, parseAPLExpression("3.1 cmp 2.2J5"))
        assertSimpleNumber(1, parseAPLExpression("2.2J1 cmp 2.0"))
        assertSimpleNumber(-1, parseAPLExpression("1J1 cmp 2.0"))
        assertSimpleNumber(0, parseAPLExpression("2J0 cmp 2.0"))
        assertSimpleNumber(0, parseAPLExpression("2.0 cmp 2J0"))
    }

    @Test
    fun compareLists() {
        assertSimpleNumber(-1, parseAPLExpression("(1;2;3) cmp (1;2;4)"))
        assertSimpleNumber(1, parseAPLExpression("(1;2;4) cmp (1;2;3)"))
        assertSimpleNumber(0, parseAPLExpression("(1;2;3) cmp (1;2;3)"))
        assertSimpleNumber(-1, parseAPLExpression("(10;11;12) cmp (1;2;3;4)"))
        assertSimpleNumber(1, parseAPLExpression("(1;2;3;4) cmp (10;11;12)"))
    }
}
