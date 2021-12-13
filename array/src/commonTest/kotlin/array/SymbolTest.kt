package array

import kotlin.test.*

class SymbolTest : APLTest() {
    @Test
    fun testIntern() {
        val engine = Engine()
        val symbol1 = engine.internSymbol("symbol1")
        val symbol2 = engine.internSymbol("symbol2")
        val symbol3 = engine.internSymbol("symbol1")
        assertNotSame(symbol1, symbol2)
        assertNotSame(symbol2, symbol3)
        assertSame(symbol1, symbol3)
    }

    @Test
    fun compareWithNamespace() {
        val engine = Engine()
        val n0 = Namespace("n0")
        val n1 = Namespace("n1")
        val sym0 = engine.internSymbol("abc", n0)
        val sym1 = engine.internSymbol("abc", n1)
        assertNotEquals(sym0, sym1)
    }

    @Test
    fun compareToWithNamespace() {
        val engine = Engine()
        val n0 = Namespace("n0")
        val n1 = Namespace("n1")
        val sym0 = engine.internSymbol("abc", n0)
        val sym1 = engine.internSymbol("abc", n1)
        val d = sym0.compareTo(sym1)
        assertEquals(-1, d)
    }

    @Test
    fun testParseSymbol() {
        val engine = Engine()
        val result = engine.parseAndEval(StringSourceLocation("'foo"), false)
        assertSame(engine.internSymbol("foo"), result.ensureSymbol().value)
    }

    @Test
    fun keywordSymbol() {
        val engine = Engine()
        val result = engine.parseAndEval(StringSourceLocation(":foo"), false)
        assertSame(engine.makeNamespace("keyword").internSymbol("foo"), result.ensureSymbol().value)
    }
}
