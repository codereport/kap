package array

import array.complex.Complex
import com.dhsdevelopments.mpbignum.BigInt
import com.dhsdevelopments.mpbignum.of
import kotlin.test.*

class TokenGeneratorTest {
    @Test
    fun testSimpleToken() {
        val gen = makeGenerator("foo")
        val token = gen.nextToken()
        assertTokenIsSymbol(gen, token, "foo", gen.engine.initialNamespace.name)
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun testMultipleTokens() {
        val gen = makeGenerator("foo bar test abc test")
        val expectedTokens = arrayOf("foo", "bar", "test", "abc", "test")
        expectedTokens.forEach { name ->
            val token = gen.nextToken()
            assertTokenIsSymbol(gen, token, name, gen.engine.initialNamespace.name)
        }
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun testMultipleTokensSpecialChars() {
        val gen = makeGenerator("foo bar test abc test ∆ ⍙ a∆ a⍙ ∆b ⍙b x∆y x⍙y")
        val expectedTokens = arrayOf("foo", "bar", "test", "abc", "test", "∆", "⍙", "a∆", "a⍙", "∆b", "⍙b", "x∆y", "x⍙y")
        expectedTokens.forEach { name ->
            val token = gen.nextToken()
            assertTokenIsSymbol(gen, token, name, gen.engine.initialNamespace.name)
        }
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun testArgSymbols() {
        val gen = makeGenerator("⍵ ⍺ ⍺x ⍵x qz⍺x⍵zq")
        assertTokenIsSymbol(gen, gen.nextToken(), "⍵", gen.engine.coreNamespace.name)
        assertTokenIsSymbol(gen, gen.nextToken(), "⍺", gen.engine.coreNamespace.name)
        assertTokenIsSymbol(gen, gen.nextToken(), "⍺", gen.engine.coreNamespace.name)
        assertTokenIsSymbol(gen, gen.nextToken(), "x", gen.engine.initialNamespace.name)
        assertTokenIsSymbol(gen, gen.nextToken(), "⍵", gen.engine.coreNamespace.name)
        assertTokenIsSymbol(gen, gen.nextToken(), "x", gen.engine.initialNamespace.name)
        assertTokenIsSymbol(gen, gen.nextToken(), "qz", gen.engine.initialNamespace.name)
        assertTokenIsSymbol(gen, gen.nextToken(), "⍺", gen.engine.coreNamespace.name)
        assertTokenIsSymbol(gen, gen.nextToken(), "x", gen.engine.initialNamespace.name)
        assertTokenIsSymbol(gen, gen.nextToken(), "⍵", gen.engine.coreNamespace.name)
        assertTokenIsSymbol(gen, gen.nextToken(), "zq", gen.engine.initialNamespace.name)
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun testMultipleSpaces() {
        val gen = makeGenerator("     foo       bar     test        ")
        val expectedTokens = arrayOf("foo", "bar", "test")
        expectedTokens.forEach { name ->
            val token = gen.nextToken()
            assertTokenIsSymbol(gen, token, name, gen.engine.initialNamespace.name)
        }
        assertSame(EndOfFile, gen.nextToken())
    }

//    @Test
//    fun testNewline() {
//        val gen = makeGenerator("foo\nbar test")
//        val expectedTokens = arrayOf("foo", "bar", "test")
//        expectedTokens.forEach { name ->
//            val token = gen.nextToken()
//            assertTokenIsSymbol(gen, token, name)
//        }
//        assertSame(EndOfFile, gen.nextToken())
//    }

    @Test
    fun newlinePara() {
        val gen = makeGenerator("foo\nbar\nabc")
        assertTokenIsSymbol(gen, gen.nextToken(), "foo", gen.engine.initialNamespace.name)
        assertSame(Newline, gen.nextToken())
        assertTokenIsSymbol(gen, gen.nextToken(), "bar", gen.engine.initialNamespace.name)
        assertSame(Newline, gen.nextToken())
        assertTokenIsSymbol(gen, gen.nextToken(), "abc", gen.engine.initialNamespace.name)
    }

    @Test
    fun singleCharFunction() {
        val gen = makeGenerator("+-,,")
        val expectedTokens = arrayOf("+", "-", ",", ",")
        expectedTokens.forEach { name ->
            val token = gen.nextToken()
            assertTokenIsSymbol(gen, token, name, "kap")
        }
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun parseNumbers() {
        val gen = makeGenerator("10 20 1 ¯1 ¯10")
        val expectedTokens = arrayOf(10, 20, 1, -1, -10)
        expectedTokens.forEach { value ->
            val token = gen.nextToken()
            assertTrue(token is ParsedLong)
            assertEquals(value.toLong(), token.value)
        }
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun parseNumberTypes() {
        val gen = makeGenerator("1 2 1.2 2.0 9. ¯2.3 ¯2. 0 0.0")
        assertInteger(1, gen.nextToken())
        assertInteger(2, gen.nextToken())
        assertDouble(Pair(1.11119, 1.20001), gen.nextToken())
        assertDouble(Pair(1.99999, 2.00001), gen.nextToken())
        assertDouble(Pair(8.99999, 9.00001), gen.nextToken())
        assertDouble(Pair(-2.30001, -2.29999), gen.nextToken())
        assertDouble(Pair(-2.00001, -1.99999), gen.nextToken())
        assertInteger(0, gen.nextToken())
        assertDouble(Pair(-0.00001, 0.00001), gen.nextToken())
        assertSame(EndOfFile, gen.nextToken())
    }

    private fun assertDouble(expected: Pair<Double, Double>, token: Token) {
        assertTrue(token is ParsedDouble)
        assertTrue(expected.first <= token.value)
        assertTrue(expected.second >= token.value)
    }

    private fun assertInteger(expected: Long, token: Token) {
        assertTrue(token is ParsedLong)
        assertEquals(expected, token.value)
    }

    @Test
    fun parseInvalidNumbers() {
        assertFailsWith<IllegalNumberFormat> {
            val gen = makeGenerator("2000a")
            gen.nextToken()
        }
    }

    @Test
    fun parseComments() {
        val gen = makeGenerator("foo ⍝ test comment")
        val token = gen.nextToken()
        assertTokenIsSymbol(gen, token, "foo", gen.engine.initialNamespace.name)
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun testStrings() {
        val gen = makeGenerator("\"foo\" \"embedded\\\"quote\"")
        gen.nextToken().let { token ->
            assertTrue(token is StringToken)
            assertEquals("foo", token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is StringToken)
            assertEquals("embedded\"quote", token.value)
        }
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun testUnterminatedString() {
        val gen = makeGenerator("\"bar")
        assertFailsWith<ParseException> {
            gen.nextToken()
        }
    }

    @Test
    fun testSymbolsWithNumbers() {
        val gen = makeGenerator("a1 a2 a3b aa2233")
        gen.nextToken().let { token ->
            assertTokenIsSymbol(gen, token, "a1", gen.engine.initialNamespace.name)
        }
        gen.nextToken().let { token ->
            assertTokenIsSymbol(gen, token, "a2", gen.engine.initialNamespace.name)
        }
        gen.nextToken().let { token ->
            assertTokenIsSymbol(gen, token, "a3b", gen.engine.initialNamespace.name)
        }
        gen.nextToken().let { token ->
            assertTokenIsSymbol(gen, token, "aa2233", gen.engine.initialNamespace.name)
        }
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun testCharacters() {
        val gen = makeGenerator("@a @b @1 @2")
        assertTokenIsCharacter('a'.code, gen.nextToken())
        assertTokenIsCharacter('b'.code, gen.nextToken())
        assertTokenIsCharacter('1'.code, gen.nextToken())
        assertTokenIsCharacter('2'.code, gen.nextToken())
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun specialCharacters() {
        val gen = makeGenerator("@\\n @\\r @\\\\ @\\e @\\u014E @\\0 @\\s @\\t")
        assertTokenIsCharacter('\n'.code, gen.nextToken())
        assertTokenIsCharacter('\r'.code, gen.nextToken())
        assertTokenIsCharacter('\\'.code, gen.nextToken())
        assertTokenIsCharacter(27, gen.nextToken())
        assertTokenIsCharacter(0x014e, gen.nextToken())
        assertTokenIsCharacter(0, gen.nextToken())
        assertTokenIsCharacter(' '.code, gen.nextToken())
        assertTokenIsCharacter('\t'.code, gen.nextToken())
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun specialCharactersNoUnicodeNames() {
        val gen = makeGenerator("@\\n @\\\\ @\\e @\\u014E")
        assertTokenIsCharacter('\n'.code, gen.nextToken())
        assertTokenIsCharacter('\\'.code, gen.nextToken())
        assertTokenIsCharacter(27, gen.nextToken())
        assertTokenIsCharacter(0x014e, gen.nextToken())
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun unicodeNames() {
        if (backendSupportsUnicodeNames) {
            val gen = makeGenerator("@\\SPACE @\\GREATER-THAN_OR_EQUIVALENT_TO @\\MATHEMATICAL_BOLD_SMALL_E")
            assertTokenIsCharacter(' '.code, gen.nextToken())
            assertTokenIsCharacter(0x2273, gen.nextToken())
            assertTokenIsCharacter(0x1D41E, gen.nextToken())
            assertSame(EndOfFile, gen.nextToken())
        }
    }

    @Test
    fun specialCharactersIllegalSyntax0() {
        assertFailsWith<ParseException> {
            makeGenerator("@\\q").nextToken()
        }
    }

    @Test
    fun specialCharactersIllegalSyntax1() {
        assertFailsWith<ParseException> {
            makeGenerator("@\\SOME_TEXT").nextToken()
        }
    }

    @Test
    fun specialCharactersIllegalSyntax2() {
        assertFailsWith<ParseException> {
            val aa = makeGenerator("@\\u12ab17").nextToken()
            println(aa)
        }
    }

    @Test
    fun specialCharactersIllegalSyntax3() {
        assertFailsWith<ParseException> {
            makeGenerator("@\\u").nextToken()
        }
    }

    @Test
    fun specialCharactersIllegalSyntax4() {
        assertFailsWith<ParseException> {
            makeGenerator("@").nextToken()
        }
    }

    @Test
    fun specialCharactersIllegalSyntax5() {
        assertFailsWith<ParseException> {
            makeGenerator("@\\").nextToken()
        }
    }

    @Test
    fun testSymbolsInStrings() {
        val gen = makeGenerator("\"a\"  \"foo@bar\"  ")
        gen.nextToken().let { token ->
            assertTrue(token is StringToken)
            assertEquals("a", token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is StringToken)
            assertEquals("foo@bar", token.value)
        }
        assertSame(EndOfFile, gen.nextToken())
    }

    private fun assertTokenIsCharacter(expected: Int, token: Token) {
        assertTrue(token is ParsedCharacter, "actual type was: ${token}")
        assertEquals(expected, token.value)
    }

    @Test
    fun complexNumbers() {
        val gen = makeGenerator("1j2 0j2 2j0 1J2 0J2 ¯1j2 1j¯2 ¯1j¯2")
        gen.nextToken().let { token ->
            assertTrue(token is ParsedComplex)
            assertEquals(Complex(1.0, 2.0), token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedComplex)
            assertEquals(Complex(0.0, 2.0), token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedComplex)
            assertEquals(Complex(2.0, 0.0), token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedComplex)
            assertEquals(Complex(1.0, 2.0), token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedComplex)
            assertEquals(Complex(0.0, 2.0), token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedComplex)
            assertEquals(Complex(-1.0, 2.0), token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedComplex)
            assertEquals(Complex(1.0, -2.0), token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedComplex)
            assertEquals(Complex(-1.0, -2.0), token.value)
        }
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun testParserPosition() {
        val gen = makeGenerator("foo bar 10 1.2\nx y")
        gen.nextTokenWithPosition().let { (token, pos) ->
            assertTokenIsSymbol(gen, token, "foo", gen.engine.initialNamespace.name)
            assertEquals(0, pos.line)
            assertEquals(0, pos.col)
        }
        gen.nextTokenWithPosition().let { (token, pos) ->
            assertTokenIsSymbol(gen, token, "bar", gen.engine.initialNamespace.name)
            assertEquals(0, pos.line)
            assertEquals(4, pos.col)
        }
        gen.nextTokenWithPosition().let { (token, pos) ->
            assertTrue(token is ParsedLong)
            assertEquals(10, token.value)
            assertEquals(0, pos.line)
            assertEquals(8, pos.col)
        }
        gen.nextTokenWithPosition().let { (token, pos) ->
            assertTrue(token is ParsedDouble)
            val value = token.value
            assertTrue(1.11119 <= value)
            assertTrue(2.00001 >= value)
            assertEquals(0, pos.line)
            assertEquals(11, pos.col)
        }
        gen.nextTokenWithPosition().let { (token, pos) ->
            assertSame(Newline, token)
            assertEquals(0, pos.line)
            assertEquals(14, pos.col)
        }
        gen.nextTokenWithPosition().let { (token, pos) ->
            assertTokenIsSymbol(gen, token, "x", gen.engine.initialNamespace.name)
            assertEquals(1, pos.line)
            assertEquals(0, pos.col)
        }
        gen.nextTokenWithPosition().let { (token, pos) ->
            assertTokenIsSymbol(gen, token, "y", gen.engine.initialNamespace.name)
            assertEquals(1, pos.line)
            assertEquals(2, pos.col)
        }
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun nonBmpStandaloneChars() {
        val gen = makeGenerator("@\uD835\uDC9F @b")
        gen.nextToken().let { token ->
            assertTrue(token is ParsedCharacter)
            assertEquals(0x1d49f, token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedCharacter)
            assertEquals('b'.code, token.value)
        }
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun continuationCharacter() {
        val gen = makeGenerator(
            """
            |1 2 `
            |3 `
            |4
            """.trimMargin()
        )
        gen.nextToken().let { token ->
            assertTrue(token is ParsedLong)
            assertEquals(1, token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedLong)
            assertEquals(2, token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedLong)
            assertEquals(3, token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedLong)
            assertEquals(4, token.value)
        }
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun continuationCharacterIllegalPosition() {
        val gen = makeGenerator(
            """
            |1 `4 5
            |6
            """.trimMargin()
        )
        gen.nextToken().let { token ->
            assertTrue(token is ParsedLong)
            assertEquals(1, token.value)
        }
        assertFailsWith<ParseException> {
            gen.nextToken()
        }
    }

    @Test
    fun parsePlainSymbol() {
        val res = TokenGenerator.parseStringToSymbol("foo:ab")
        assertNotNull(res)
        assertEquals("foo", res.first)
        assertEquals("ab", res.second)
    }

    @Test
    fun parsePlainSymbolNoNamespace() {
        val res = TokenGenerator.parseStringToSymbol("ab")
        assertNotNull(res)
        assertNull(res.first)
        assertEquals("ab", res.second)
    }

    @Test
    fun parseIllegalNames() {
        assertNull(TokenGenerator.parseStringToSymbol("  foo"))
        assertNull(TokenGenerator.parseStringToSymbol("foo  "))
        assertNull(TokenGenerator.parseStringToSymbol("  foo  "))
        assertNull(TokenGenerator.parseStringToSymbol("  foo:ab"))
        assertNull(TokenGenerator.parseStringToSymbol("foo:ab  "))
        assertNull(TokenGenerator.parseStringToSymbol("  foo:ab  "))
        assertNull(TokenGenerator.parseStringToSymbol("foo abcdef"))
        assertNull(TokenGenerator.parseStringToSymbol("12"))
        assertNull(TokenGenerator.parseStringToSymbol("12foo"))
        assertNull(TokenGenerator.parseStringToSymbol("12:foo"))
        assertNull(TokenGenerator.parseStringToSymbol("foo:99"))
        assertNull(TokenGenerator.parseStringToSymbol("99:77"))
        assertNull(TokenGenerator.parseStringToSymbol("foo:99ab"))
        assertNull(TokenGenerator.parseStringToSymbol("aa,ww"))
        assertNull(TokenGenerator.parseStringToSymbol("aa:"))
        assertNull(TokenGenerator.parseStringToSymbol("aa::"))
        assertNull(TokenGenerator.parseStringToSymbol("1a1"))
    }

    @Test
    fun parseKeyword() {
        val res = TokenGenerator.parseStringToSymbol(":foo")
        assertNotNull(res)
        assertEquals("keyword", res.first)
        assertEquals("foo", res.second)
    }

    @Test
    fun pushBackTokenTest() {
        val gen = makeGenerator("aa abc abcd")
        gen.nextToken().let { token ->
            assertTokenIsSymbol(gen, token, "aa", gen.engine.initialNamespace.name)
        }
        val a = gen.nextTokenWithPosition()
        assertTokenIsSymbol(gen, a.token, "abc", gen.engine.initialNamespace.name)
        assertEquals(0, a.pos.line)
        assertEquals(3, a.pos.col)
        assertEquals(6, a.pos.computedEndCol)
        gen.pushBackToken(a)
        val (b, bPos) = gen.nextTokenWithPosition()
        assertTokenIsSymbol(gen, b, "abc", gen.engine.initialNamespace.name)
        assertEquals(0, bPos.line)
        assertEquals(3, bPos.col)
        assertEquals(6, bPos.computedEndCol)
        gen.nextTokenWithPosition().let { (c, cPos) ->
            assertTokenIsSymbol(gen, c, "abcd", gen.engine.initialNamespace.name)
            assertEquals(0, cPos.line)
            assertEquals(7, cPos.col)
            assertEquals(11, cPos.computedEndCol)
        }
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun integerTokenPosition() {
        val gen = makeGenerator("aa abc 1234 abcd")
        gen.nextToken().let { token ->
            assertTokenIsSymbol(gen, token, "aa", gen.engine.initialNamespace.name)
        }
        gen.nextToken().let { token ->
            assertTokenIsSymbol(gen, token, "abc", gen.engine.initialNamespace.name)
        }
        gen.nextTokenWithPosition().let { (token, pos) ->
            assertTrue(token is ParsedLong)
            assertEquals(1234, token.value)
            assertEquals(0, pos.line)
            assertEquals(7, pos.col)
            assertEquals(11, pos.computedEndCol)
        }
        gen.nextToken().let { token ->
            assertTokenIsSymbol(gen, token, "abcd", gen.engine.initialNamespace.name)
        }
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun multilineTokenPosition() {
        val gen = makeGenerator(
            """
            |aa bb
            |cccc dddd
            """.trimMargin()
        )
        gen.nextTokenWithPosition().let { (token, pos) ->
            assertTokenIsSymbol(gen, token, "aa", gen.engine.initialNamespace.name)
            assertPosition(0, 0, 0, 2, pos)
        }
        gen.nextTokenWithPosition().let { (token, pos) ->
            assertTokenIsSymbol(gen, token, "bb", gen.engine.initialNamespace.name)
            assertPosition(0, 3, 0, 5, pos)
        }
        assertSame(Newline, gen.nextToken())
        gen.nextTokenWithPosition().let { (token, pos) ->
            assertTokenIsSymbol(gen, token, "cccc", gen.engine.initialNamespace.name)
            assertPosition(1, 0, 1, 4, pos)
        }
        gen.nextTokenWithPosition().let { (token, pos) ->
            assertTokenIsSymbol(gen, token, "dddd", gen.engine.initialNamespace.name)
            assertPosition(1, 5, 1, 9, pos)
        }
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun multilinePositionWithContinuationChars() {
        val gen = makeGenerator(
            """
            |aa bb `
            |cc dd
            """.trimMargin()
        )
        gen.nextTokenWithPosition().let { (token, pos) ->
            assertTokenIsSymbol(gen, token, "aa", gen.engine.initialNamespace.name)
            assertPosition(0, 0, 0, 2, pos)
        }
        gen.nextTokenWithPosition().let { (token, pos) ->
            assertTokenIsSymbol(gen, token, "bb", gen.engine.initialNamespace.name)
            assertPosition(0, 3, 0, 5, pos)
        }
        gen.nextTokenWithPosition().let { (token, pos) ->
            assertTokenIsSymbol(gen, token, "cc", gen.engine.initialNamespace.name)
            assertPosition(1, 0, 1, 2, pos)
        }
        gen.nextTokenWithPosition().let { (token, pos) ->
            assertTokenIsSymbol(gen, token, "dd", gen.engine.initialNamespace.name)
            assertPosition(1, 3, 1, 5, pos)
        }
        assertSame(EndOfFile, gen.nextToken())
    }

    @Test
    fun parseBigInt() {
        val gen =
            makeGenerator(
                "1234567891234567891234567890 ¯22222234567891234567891234567890 9223372036854775807 " +
                        "9223372036854775808 ¯9223372036854775808 ¯9223372036854775809"
            )
        gen.nextToken().let { token ->
            assertTrue(token is ParsedBigInt)
            assertEquals(BigInt.of("1234567891234567891234567890"), token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedBigInt)
            assertEquals(BigInt.of("-22222234567891234567891234567890"), token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedLong)
            assertEquals(0x7FFFFFFFFFFFFFFF, token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedBigInt)
            assertEquals(BigInt.of("9223372036854775808"), token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedLong)
            assertEquals((-0x7FFFFFFFFFFFFFFF) - 1, token.value)
        }
        gen.nextToken().let { token ->
            assertTrue(token is ParsedBigInt)
            assertEquals(BigInt.of("-9223372036854775809"), token.value)
        }
        assertSame(EndOfFile, gen.nextToken())
    }

    private fun makeGenerator(content: String): TokenGenerator {
        val engine = Engine()
        return TokenGenerator(engine, StringSourceLocation(content))
    }

    private fun assertTokenIsSymbol(gen: TokenGenerator, token: Token, name: String, namespace: String) {
        assertTrue(token is Symbol, "Token was: ${token}")
        assertEquals(gen.engine.internSymbol(name, gen.engine.makeNamespace(namespace)), token)
        assertEquals(name, token.symbolName)
        assertEquals(namespace, token.namespace.name)
    }

    private fun assertPosition(line: Int, col: Int, endLine: Int, endCol: Int, pos: Position) {
        assertEquals(line, pos.line)
        assertEquals(col, pos.col)
        assertEquals(endLine, pos.endLine)
        assertEquals(endCol, pos.endCol)
    }
}
