package array

import array.complex.Complex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SortTest : APLTest() {
    @Test
    fun numberSort() {
        sortTest("10 30 20 5 11 21 3 1 12 2", arrayOf(7, 9, 6, 3, 0, 4, 8, 2, 5, 1))
    }

    @Test
    fun sortStrings() {
        sortTest(
            "\"foo\" \"bar\" \"test\" \"abc\" \"xyz\" \"some\" \"strings\" \"withlongtext\" \"b\"",
            arrayOf(3, 8, 1, 0, 5, 6, 2, 7, 4))
    }

    @Test
    fun sortMultiDimensional() {
        sortTest(
            "3 4 ⍴ 8 5 1 7 0 11 6 2 4 3 10 9",
            arrayOf(1, 2, 0))
    }

    @Test
    fun sortMixedTypes() {
        sortTest("1.2 2 0.1 ¯9 ¯9.9 4 7.1 8.3", arrayOf(4, 3, 2, 0, 1, 5, 6, 7))
    }

    @Test
    fun sortDouble() {
        sortTest("1.2 0.1 ¯9.9 0.0 0.000001", arrayOf(2, 3, 4, 1, 0))
    }

    @Test
    fun sortIntegerWithOverflow() {
        sortTest("0x6000000000000000 + 0x1000 0x7000000000000000 0 ¯3 9 1000", arrayOf(3, 2, 4, 5, 0, 1))
    }

    @Test
    fun sortSingleElement() {
        sortTest(",1", arrayOf(0))
    }

    @Test
    fun sortingScalarsShouldFail() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍋1")
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍒1")
        }
    }

    @Test
    fun sortingArraysOfDifferentDimensions() {
        sortTest(
            "(⊂4 3 ⍴ 1 2 3 4 5 6) (⊂3 4 ⍴ 1 2 3 4 5 6) (⊂3 2 ⍴ 1 2 3 4) (⊂2 5 ⍴ 1 2) (⊂2 5 3 ⍴ 1 2) (⊂4 3 ⍴ 1 2 2 4 5 6)",
            arrayOf(3, 2, 1, 5, 0, 4))
    }

    @Test
    fun compareNumbersAndChars() {
        assert1DArray(arrayOf(1, 2, 0, 3), parseAPLExpression("⍋ @a 1 2 @b"))
        assert1DArray(arrayOf(3, 0, 2, 1), parseAPLExpression("⍒ @a 1 2 @b"))
    }

    @Test
    fun compareLists() {
        assert1DArray(arrayOf(0, 1), parseAPLExpression("⍋ (1;2) (2;1)"))
        assert1DArray(arrayOf(1, 0), parseAPLExpression("⍒ (1;2) (2;1)"))
    }

    @Test
    fun compareComplexShouldFail() {
        assert1DArray(arrayOf(0, 1), parseAPLExpression("⍋ 1J2 2J3"))
        assert1DArray(arrayOf(1, 0), parseAPLExpression("⍒ 1J2 2J3"))
    }

    @Test
    fun mixStringsAndSymbolsShouldFail() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍋ \"foo\" \"bar\" 'somename")
        }
        assertFailsWith<APLEvalException> {
            parseAPLExpression("⍒ \"foo\" \"bar\" 'somename")
        }
    }

    @Test
    fun symbolsAndNumber() {
        assert1DArray(arrayOf(0, 1, 2, 3), parseAPLExpression("⍋ 1 2 3 'somename"))
        assert1DArray(arrayOf(3, 2, 1, 0), parseAPLExpression("⍒ 1 2 3 'somename"))
    }

    @Test
    fun numbersAndComplex() {
        assert1DArray(arrayOf(0, 3, 1, 2), parseAPLExpression("⍋ 1 2 3 1J2"))
        assert1DArray(arrayOf(2, 1, 3, 0), parseAPLExpression("⍒ 1 2 3 1J2"))
    }

    @Test
    fun sortSymbols() {
        sortTest("'foo 'bar 'a 'test 'abclongerstring", arrayOf(2, 4, 1, 0, 3))
    }

    @Test
    fun tokenSymbolComparison() {
        val engine = Engine()
        val sym = APLSymbol(engine.internSymbol("foo"))
        val str = APLString.make("bar")
        assertFailsWith<APLEvalException> {
            sym.compare(str)
        }
        assertFailsWith<APLEvalException> {
            str.compare(sym)
        }
    }

    @Test
    fun numberSymbolComparison() {
        val engine = Engine()
        val num = 1.makeAPLNumber()
        val sym = APLSymbol(engine.internSymbol("foo"))
        assertEquals(-1, num.compare(sym))
        assertEquals(1, sym.compare(num))
    }

    @Test
    fun numberComplexComparison() {
        val num = 1.makeAPLNumber()
        val complex = Complex(2.0, 3.0).makeAPLNumber()
        assertEquals(-1, num.compare(complex))
        assertEquals(1, complex.compare(num))
    }

    @Test
    fun listComparison() {
        val list1 = APLList(listOf(1.makeAPLNumber(), 2.makeAPLNumber()))
        val list2 = APLList(listOf(2.makeAPLNumber(), 4.makeAPLNumber()))
        assertEquals(-1, list1.compare(list2))
        assertEquals(1, list2.compare(list1))
    }

    @Test
    fun numberCharComparison() {
        val char1 = APLChar('a'.code)
        val num1 = 1.makeAPLNumber()
        assertEquals(1, char1.compare(num1))
        assertEquals(-1, num1.compare(char1))
    }

    @Test
    fun symbolCharComparison() {
        val engine = Engine()
        val sym = APLSymbol(engine.internSymbol("foo"))
        val ch = APLChar('a'.code)
        assertEquals(1, sym.compare(ch))
        assertEquals(-1, ch.compare(sym))
    }

    private fun sortTest(content: String, expected: Array<Int>) {
        fun sortTestInner(s: String, exp: Array<Int>) {
            parseAPLExpression(s).let { result ->
                assertDimension(dimensionsOfSize(exp.size), result)
                assertArrayContent(exp, result)
            }
        }

        sortTestInner("⍋ ${content}", expected)
        sortTestInner("⍒ ${content}", expected.reversedArray())
    }
}
