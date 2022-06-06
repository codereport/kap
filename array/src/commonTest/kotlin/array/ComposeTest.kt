package array

import kotlin.test.Ignore
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
        parseAPLExpression("(int:ensureDouble 301.0 ¯302.0 303.0 ¯304.0 305.0) (+∘×) (int:ensureDouble ¯10.0 ¯11.0 12.0 13.0 14.0)").let { result ->
            assertDimension(dimensionsOfSize(5), result)
            assertArrayContent(
                arrayOf(
                    InnerDouble(300.0),
                    InnerDouble(-303.0),
                    InnerDouble(304.0),
                    InnerDouble(-303.0),
                    InnerDouble(306.0)),
                result)
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
            assertAPLValue(InnerDouble(-32.0), result)
        }
    }

    @Test
    fun composeWithLeftArgShouldFail() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression("2 (3+⊢) 5")
        }
    }

    @Test
    fun forkWithLeftArgShouldFail() {
        assertFailsWith<APLEvalException> {
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

    @Ignore
    @Test
    fun chainWithAxisTest() {
        parseAPLExpression("a ⇐ ({9,⊂⍵}+[1]) ◊ 1 2 a 2 2 ⍴ ⍳4").let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertArrayContent(
                arrayOf(
                    9,
                    InnerArray(dimensionsOfSize(2, 2), arrayOf(1, 3, 3, 5))),
                result)
        }
    }

    @Test
    fun chainWithDfns0() {
        parseAPLExpressionWithOutput("({io:print ⍵+1} {io:print ⍺+⍵+10} {io:print ⍵+3}) 1").let { (result, out) ->
            assertSimpleNumber(16, result)
            assertEquals("4216", out)
        }
    }

    @Test
    fun chainWithDfns1() {
        parseAPLExpressionWithOutput("(+ {io:print ⍺+⍵+10} +) 1").let { (result, out) ->
            assertSimpleNumber(12, result)
            assertEquals("12", out)
        }
    }

    @Test
    fun chainWithDfns2() {
        parseAPLExpressionWithOutput("10 ({io:print ⍺+⍵+1} {io:print ⍺+⍵+10} {io:print ⍺+⍵+3}) 100").let { (result, out) ->
            assertSimpleNumber(234, result)
            assertEquals("113111234", out)
        }
    }

    @Test
    fun chainWithDfns3() {
        parseAPLExpressionWithOutput("10 (+ {io:print ⍺+⍵+10} +) 9").let { (result, out) ->
            assertSimpleNumber(48, result)
            assertEquals("48", out)
        }
    }

    @Test
    fun chainWithDfns4() {
        parseAPLExpressionWithOutput("({io:print ⍵+1} + +) 1").let { (result, out) ->
            assertSimpleNumber(3, result)
            assertEquals("2", out)
        }
    }

    @Test
    fun chainWithDfns5() {
        parseAPLExpressionWithOutput("(+ + {io:print ⍵+1}) 1").let { (result, out) ->
            assertSimpleNumber(3, result)
            assertEquals("2", out)
        }
    }

    @Test
    fun reverseCompose0() {
        parseAPLExpression("10 (-⍛+) 100").let { result ->
            assertSimpleNumber(90, result)
        }
    }

    @Test
    fun reverseComposeFailsWithMonadic() {
        assertFailsWith<Unimplemented1ArgException> {
            parseAPLExpression("(-⍛+) 100")
        }
    }

    @Test
    fun leftBindSimpleFunction() {
        parseAPLExpression("(10+) 1").let { result ->
            assertSimpleNumber(11, result)
        }
    }

    @Test
    fun leftBindToDfn() {
        parseAPLExpression("(5 {⍺+⍵+50}) 4").let { result ->
            assertSimpleNumber(59, result)
        }
    }

    @Test
    fun leftBindToNamedFunction() {
        val src = """
            |a ⇐ 10+
            |a 1
        """.trimMargin()
        parseAPLExpression(src).let { result ->
            assertSimpleNumber(11, result)
        }
    }

    @Test
    fun leftBindDfnToNamed() {
        val src = """
            |a ⇐ 5 {⍺+⍵+50}
            |a 4
        """.trimMargin()
        parseAPLExpression(src).let { result ->
            assertSimpleNumber(59, result)
        }
    }

    @Test
    fun leftBindWithClosure() {
        val src = """
            |i ← 0
            |a ⇐ 5 { ⍺+⍵+i←io:print i+1 }
            |(a 1) + (a 2)
        """.trimMargin()
        parseAPLExpressionWithOutput(src).let { (result, out) ->
            assertSimpleNumber(16, result)
            assertEquals("12", out)
        }
    }

    @Test
    fun leftBindWithLeftVariable() {
        val src = """
            |x ← 10
            |a ⇐ x+
            |y ← a 1
            |x ← 20
            |z ← a 6
            |y z
        """.trimMargin()
        parseAPLExpression(src).let { result ->
            assert1DArray(arrayOf(11, 16), result)
        }
    }

    @Test
    fun leftBindWithTrain() {
        val src = """
            |foo ⇐ 2+⊢
            |foo 4
        """.trimMargin()
        parseAPLExpression(src).let { result ->
            assertSimpleNumber(4, result)
        }
    }

    @Test
    fun leftBindWithOutput0() {
        val src = """
            |foo ⇐ (io:print 2)+
            |(foo 100) (foo 101)
        """.trimMargin()
        parseAPLExpressionWithOutput(src).let { (result, out) ->
            assert1DArray(arrayOf(102, 103), result)
            assertEquals("2", out)
        }
    }

    @Test
    fun leftBindWithOutput1() {
        val src = """
            |foo ⇐ (io:print 2)+
            |1
        """.trimMargin()
        parseAPLExpressionWithOutput(src).let { (result, out) ->
            assertSimpleNumber(1, result)
            assertEquals("2", out)
        }
    }

    @Test
    fun leftBindWithScope() {
        val src = """
            |foo ⇐ {
            |  λ((10+⍵)+)
            |}
            |a ⇐ ⍞(foo 1)
            |a 5
        """.trimMargin()
        parseAPLExpression(src).let { result ->
            assertSimpleNumber(16, result)
        }
    }

    @Test
    fun leftBindWithTrainArgument() {
        parseAPLExpression("(1(-÷)) 4").let { result ->
            assertSimpleDouble(-0.25, result)
        }
    }

    @Test
    fun leftBindArgumentIsFirstArgInTrain() {
        parseAPLExpression("((3-)÷) 8").let { result ->
            assertSimpleDouble(2.875, result)
        }
    }

    @Test
    fun leftBindMultipleFunctions0() {
        parseAPLExpression("((11+)(22+)) 44").let { result ->
            assertSimpleNumber(77, result)
        }
    }

    @Test
    fun leftBindMultipleFunctions1() {
        parseAPLExpressionWithOutput("(((io:print 11)+)((io:print 22)+)) 44").let { (result, out) ->
            assertSimpleNumber(77, result)
            assertEquals("2211", out)
        }
    }

    @Test
    fun leftBindMultipleFunctions2() {
        parseAPLExpressionWithOutput("(((io:print 11)+)×((io:print 22)+)) 100").let { (result, out) ->
            assertSimpleNumber(13542, result)
            assertEquals("2211", out)
        }
    }
}
