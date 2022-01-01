package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ComposeTest : APLTest() {
    @Test
    fun compose2Arg0() {
        parseAPLExpression("¯2 3 4 (×∘-) 1000").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(2000, -3000, -4000), result)
        }
    }

    @Test
    fun compose2Arg1() {
        parseAPLExpression("¯2 3 4 ×∘- 1000").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(2000, -3000, -4000), result)
        }
    }

    @Test
    fun compose1Arg0() {
        parseAPLExpression("(×∘÷) ¯1 2 3").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(-1, 1, 1), result)
        }
    }

    @Test
    fun compose1Arg1() {
        parseAPLExpression("×∘÷ ¯1 2 3").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(-1, 1, 1), result)
        }
    }

    @Test
    fun mismatchedArgumentCount0() {
        assertFailsWith<InvalidDimensionsException> {
            parseAPLExpression("¯2 3 4 (×∘-) 1000 11")
        }
    }

    @Test
    fun integerOptimisedArrays() {
        parseAPLExpression("(int:ensureLong 301 ¯302 303 ¯304 305) (+∘×) (int:ensureLong ¯10 ¯11 12 13 14)").let { result ->
            assertDimension(dimensionsOfSize(5), result)
            assertArrayContent(arrayOf(300, -303, 304, -303, 306), result)
        }
    }

    @Test
    fun doubleOptimisedArrays() {
        parseAPLExpression("(int:ensureDouble 301 ¯302 303 ¯304 305) (+∘×) (int:ensureDouble ¯10 ¯11 12 13 14)").let { result ->
            assertDimension(dimensionsOfSize(5), result)
            assertArrayContent(arrayOf(300.0, -303.0, 304.0, -303.0, 306.0), result)
        }
    }

    @Test
    fun composeWithCustomFunction0() {
        val result = parseAPLExpression(
            """
            |∇ a (x) { x+100 }
            |∇ (x) b (y) { y+1000+x }
            |1000 (b∘a) 2000
            """.trimMargin())
        assertSimpleNumber(4100, result)
    }

    @Test
    fun composeWithCustomFunction1() {
        val result = parseAPLExpression(
            """
            |∇ a (x) { x+100 }
            |∇ (x) b (y) { y+1000+x }
            |1000 b∘a 2000
            """.trimMargin())
        assertSimpleNumber(4100, result)
    }

    @Test
    fun composeWithCustomFunction2() {
        val result = parseAPLExpression(
            """
            |c ⇐ {⍵+2}
            |d ⇐ {⍵+3+⍺}
            |5 (d∘c) 6
            """.trimMargin())
        assertSimpleNumber(16, result)
    }

    @Test
    fun composeWithCustomFunction3() {
        val result = parseAPLExpression(
            """
            |c ⇐ {⍵+2}
            |d ⇐ {⍵+3+⍺}
            |5 d∘c 6
            """.trimMargin())
        assertSimpleNumber(16, result)
    }

    @Test
    fun simpleFork0() {
        parseAPLExpression("1 (⊢⊣,) 2").let { result ->
            assertSimpleNumber(2, result)
        }
    }

    @Test
    fun simpleFork1() {
        parseAPLExpression("1 (⊣⊢,) 2").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(1, 2), result)
        }
    }

    @Test
    fun simple2Train0() {
        parseAPLExpression("10 (-,) 20").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(-10, -20), result)
        }
    }

    @Test
    fun simple2Train1() {
        parseAPLExpression("2 (-*) 5").let { result ->
            assertSimpleNumber(-32, result)
        }
    }

    @Test
    fun composeWithLeftArgShouldFail() {
        assertFailsWith<ParseException> {
            parseAPLExpression("2 (3+⊢) 5")
        }
    }

    @Test
    fun forkWithLeftArgShouldFail() {
        assertFailsWith<ParseException> {
            parseAPLExpression("2 (3+⊢⊣) 5")
        }
    }

    @Test
    fun nested2Chain0() {
        parseAPLExpression("7 3 (-(÷⌈)) 4").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(NearDouble(-0.1428571429, 4), NearDouble(-0.25, 4)), result)
        }
    }

    @Test
    fun nested2Chain1() {
        parseAPLExpression("7 3 ((-÷)⌈) 4").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(NearDouble(-0.1428571429, 4), NearDouble(-0.25, 4)), result)
        }
    }

    @Test
    fun nested2ChainNoParen() {
        parseAPLExpression("(-+,×) 2 5").let { result ->
            assertDimension(dimensionsOfSize(4), result)
            assertArrayContent(arrayOf(-2, -5, -1, -1), result)
        }
    }

    @Test
    fun nested2ChainWithFunction0() {
        parseAPLExpression("a ⇐ ÷⌈ ⋄ 7 3 (-a) 4").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(NearDouble(-0.1428571429, 4), NearDouble(-0.25, 4)), result)
        }
    }

    @Test
    fun nested2ChainWithFunction1() {
        parseAPLExpression("a ⇐ -÷ ⋄ 7 3 (a⌈) 4").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(NearDouble(-0.1428571429, 4), NearDouble(-0.25, 4)), result)
        }
    }

    @Test
    fun nested3Chain0() {
        parseAPLExpression("10 11 (-+(÷⌈)) 3").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(NearDouble(7.1, 4), NearDouble(8.090909091, 4)), result)
        }
    }

    @Test
    fun nested3ChainWithFunction() {
        parseAPLExpression("a ⇐ ÷⌈ ⋄ 10 11 (-+a) 3").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(arrayOf(NearDouble(7.1, 4), NearDouble(8.090909091, 4)), result)
        }
    }

    private fun makeFunctions(): String {
        val buf = StringBuilder()
        for (ch in 'A'..'E') {
            buf.append("${ch} ⇐ { io:print if(isLocallyBound '⍺) { \"(\",⍺,\"${ch}\",⍵,\")\" } else { \"(${ch}\",⍵,\")\" } }\n")
        }
        return buf.toString()
    }

    @Test
    fun contribTest0() {
        val src = "${makeFunctions()}\n(A(B C))@y ⋄ 3"
        val (result, out) = parseAPLExpressionWithOutput(src, withStandardLib = true)
        assertEquals("(Cy)(B(Cy))(A(B(Cy)))", out)
        assertSimpleNumber(3, result)
    }

    @Test
    fun contribTest1() {
        val src = "${makeFunctions()}\n@x(A(B C))@y ⋄ 3"
        val (result, out) = parseAPLExpressionWithOutput(src, withStandardLib = true)
        assertEquals("(xCy)(B(xCy))(A(B(xCy)))", out)
        assertSimpleNumber(3, result)
    }

    @Test
    fun contribTest2() {
        val src = "${makeFunctions()}\n(A(B C)D E)@y ⋄ 3"
        val (result, out) = parseAPLExpressionWithOutput(src, withStandardLib = true)
        assertEquals("(Ey)(Cy)(B(Cy))((B(Cy))D(Ey))(A((B(Cy))D(Ey)))", out)
        assertSimpleNumber(3, result)
    }

    @Test
    fun contribTest3() {
        val src = "${makeFunctions()}\n@x(A(B C)D E)@y ⋄ 3"
        val (result, out) = parseAPLExpressionWithOutput(src, withStandardLib = true)
        assertEquals("(xEy)(xCy)(B(xCy))((B(xCy))D(xEy))(A((B(xCy))D(xEy)))", out)
        assertSimpleNumber(3, result)
    }
}
