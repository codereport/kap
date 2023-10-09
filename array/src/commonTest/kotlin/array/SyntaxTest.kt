package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SyntaxTest : APLTest() {
    @Test
    fun foo() {
        parseAPLExpression("({+/⍳1000000000}⍣10) 0")
    }

    @Test
    fun simpleCustomSyntax() {
        val result = parseAPLExpression(
            """
            |defsyntax foo (:value x) { x + 10 }
            |200+foo (1)
        """.trimMargin())
        assertSimpleNumber(211, result)
    }

    @Test
    fun constants() {
        val result = parseAPLExpression(
            """
            |defsyntax foo (:constant x) { 10 }
            |foo x
            """.trimMargin())
        assertSimpleNumber(10, result)
    }

    @Test
    fun nonMatchedConstants() {
        assertFailsWith<ParseException> {
            parseAPLExpression(
                """
                |defsyntax foo (:constant x) { 10 }
                |foo xyz
                """.trimMargin())
        }
    }

    @Test
    fun valueArg() {
        val result = parseAPLExpression(
            """
            |defsyntax foo (:value x) { x + 1 }
            |foo (100)
            """.trimMargin())
        assertSimpleNumber(101, result)
    }

    @Test
    fun doubleValueArg() {
        val result = parseAPLExpression(
            """
            |defsyntax foo (:value x :value y) { x + y }
            |foo (200) (10)
            """.trimMargin())
        assertSimpleNumber(210, result)
    }

    @Test
    fun optionalTest0() {
        val result = parseAPLExpression(
            """
            |defsyntax foo (:value a :optional (:value b)) {
            |  a+b
            |}
            |foo (1) (2)
            """.trimMargin())
        assertSimpleNumber(3, result)
    }

    @Test
    fun optionalTest1() {
        val result = parseAPLExpression(
            """
            |defsyntax foo (:value a :optional (:value b)) {
            |  100+a
            |}
            |foo (10) , 200
            """.trimMargin())
        assertDimension(dimensionsOfSize(2), result)
        assertArrayContent(arrayOf(110, 200), result)
    }

    @Test
    fun ifTestWithOptionalElse0() {
        val result = parseAPLExpression(
            """
            |defsyntax xif (:value cond :function thenStatement :optional (:constant xelse :function elseStatement)) {
            |  ⍞((cond ≡ 1) ⌷ (⍞((isLocallyBound 'elseStatement) ⌷ λ{λ{⍬}} λ{elseStatement}) ⍬) thenStatement) cond
            |}
            |(xif (1) { 10 }) (xif (0) { 10 })
            """.trimMargin())
        assertDimension(dimensionsOfSize(2), result)
        assertSimpleNumber(10, result.valueAt(0))
        val secondRes = result.valueAt(1)
        assertDimension(dimensionsOfSize(0), secondRes)
    }

    @Test
    fun ifTestWithOptionalElse1() {
        val result = parseAPLExpression(
            """
            |defsyntax xif (:value cond :function thenStatement :optional (:constant xelse :function elseStatement)) {
            |  ⍞((cond ≡ 1) ⌷ (⍞((isLocallyBound 'elseStatement) ⌷ λ{λ{⍬}} λ{elseStatement}) ⍬) thenStatement) cond
            |}
            |
            |∇ foo {
            |  (xif (1) { 10 }) (xif (0) { 10 })
            |}
            |
            |foo 3
            """.trimMargin())
        assertDimension(dimensionsOfSize(2), result)
        assertSimpleNumber(10, result.valueAt(0))
        val secondRes = result.valueAt(1)
        assertDimension(dimensionsOfSize(0), secondRes)
    }

    @Test
    fun ifTestWithElse0() {
        val result = parseAPLExpression(
            """
            |defsyntax xif (:value cond :function thenStatement :optional (:constant xelse :function elseStatement)) {
            |  ⍞((cond ≡ 1) ⌷ (⍞((isLocallyBound 'elseStatement) ⌷ λ{λ{⍬}} λ{elseStatement}) ⍬) thenStatement) cond
            |}
            |(xif (1) { 10 } xelse { 20 }) (xif (0) { 11 } xelse { 22 })
            """.trimMargin())
        assertDimension(dimensionsOfSize(2), result)
        assertArrayContent(arrayOf(10, 22), result)
    }

    @Test
    fun ifTestWithElse1() {
        val result = parseAPLExpression(
            """
            |defsyntax xif (:value cond :function thenStatement :optional (:constant xelse :function elseStatement)) {
            |  ⍞((cond ≡ 1) ⌷ (⍞((isLocallyBound 'elseStatement) ⌷ λ{λ{⍬}} λ{elseStatement}) ⍬) thenStatement) cond
            |}
            |
            |∇ foo {
            |  (xif (1) { 10 } xelse { 20 }) (xif (0) { 11 } xelse { 22 })
            |}
            |
            |foo 3
            """.trimMargin())
        assertDimension(dimensionsOfSize(2), result)
        assertArrayContent(arrayOf(10, 22), result)
    }

    @Test
    fun ifTest() {
        val result = parseAPLExpression(
            """
            |defsyntax xif (:value cond :function thenStatement :constant xelse :function elseStatement) {
            |  ⍞((cond ≡ 1) ⌷ elseStatement thenStatement) cond
            |}
            |(xif (1) { 10 } xelse { 11 }) (xif (0) { 100 } xelse { 101 })  
            """.trimMargin())
        assertDimension(dimensionsOfSize(2), result)
        assertArrayContent(arrayOf(10, 101), result)
    }

    @Test
    fun xifWithSideEffect() {
        val (result, output) = parseAPLExpressionWithOutput(
            """
            |defsyntax xif (:value cond :function thenStatement :constant xelse :function elseStatement) {
            |  ⍞((cond ≡ 1) ⌷ elseStatement thenStatement) cond
            |}
            |xif (1) { io:print "aa" ◊ 10 } xelse { io:print "bb" ◊ 11 }  
            """.trimMargin())
        assertSimpleNumber(10, result)
        assertEquals("aa", output)
    }

    @Test
    fun nonBindingDefinedFunction() {
        val result = parseAPLExpression(
            """
            |defsyntax foo (:nfunction a) { ⍞a 2 }
            |{ x←1 ◊ foo { x } } 0
            """.trimMargin())
        assertSimpleNumber(1, result)
    }

    @Test
    fun nonBindingDefinedFunctionWithArg() {
        val result = parseAPLExpression(
            """
            |defsyntax foo (:nfunction a) { ⍞a 2 }
            |{ x←1+⍵ ◊ foo { x+⍵ } } 3
            """.trimMargin())
        assertSimpleNumber(7, result)
    }

    @Test
    fun bindingFunctionEnsureLocal() {
        val engine = Engine()
        val out = StringBuilderOutput()
        engine.standardOutput = out
        val sourceLocation = StringSourceLocation(
            """
            |defsyntax foo (:function a) { (⍞a 0) + 30 }
            |x ← foo { b ← 20 ◊ 10 }
            |io:print x
            |b
            """.trimMargin())
        assertFailsWith<VariableNotAssigned> {
            engine.parseAndEval(sourceLocation).collapse()
        }
        assertEquals("40", out.buf.toString())
    }

    @Test
    fun bindingDefinedFunctionWithArg() {
        val result = parseAPLExpression(
            """
            |defsyntax foo (:function a) { ⍞a 2 }
            |{ x←1+⍵ ◊ foo { x+⍵ } } 3
            """.trimMargin())
        assertSimpleNumber(6, result)
    }

    @Test
    fun exprFunction() {
        val result = parseAPLExpression(
            """
            |defsyntax foo (:exprfunction a) { 1+⍞a 0 }
            |foo (2)
            """.trimMargin())
        assertSimpleNumber(3, result)
    }

    @Test
    fun optionalExprFunction() {
        val (result, out) = parseAPLExpressionWithOutput(
            """
            |defsyntax foo (:value x :optional (:exprfunction a)) { x+(⍞a 0)+(⍞a 0) }
            |foo (10) (io:print 9)
            """.trimMargin())
        assertSimpleNumber(28, result)
        assertEquals("99", out)
    }

    @Test
    fun defsyntaxRepeat() {
        val result = parseAPLExpression(
            """
            |defsyntaxsub bar (:constant ab :value x) {
            |  x + 1
            |}
            |
            |defsyntax foo (:repeat (y bar)) {
            |  +/y
            |}
            |
            |foo ab (10) ab (20)
            """.trimMargin())
        assertSimpleNumber(32, result)
    }

    @Test
    fun customSingleCharSymbols() {
        val result = parseAPLExpression(
            """
            |declare(:singleCharExported "a")
            |a←1
            |b←2
            |bb←3
            |aaaa bb
            """.trimMargin())
        assertDimension(dimensionsOfSize(5), result)
        assertArrayContent(arrayOf(1, 1, 1, 1, 3), result)
    }

    @Test
    fun unknownDeclarationShouldFail() {
        assertFailsWith<IllegalDeclaration> {
            parseAPLExpression(
                """
                |declare(:foo 1)
                |'a
                """.trimMargin())
        }
    }

    @Test
    fun nexprfunctionWithVariableLookup() {
        val src =
            """
            |defsyntax foo (:nexprfunction a) { 1 + ⍞a 2 }
            |{ b ← 5+⍵ ◊ foo (b) } 100
            """.trimMargin()
        parseAPLExpression(src).let { result ->
            assertSimpleNumber(106, result)
        }
    }

    @Test
    fun exprfunctionWithVariableLookup() {
        val src =
            """
            |defsyntax foo (:exprfunction a) { 1 + ⍞a 2 }
            |{ b ← 5+⍵ ◊ foo (b) } 100
            """.trimMargin()
        parseAPLExpression(src).let { result ->
            assertSimpleNumber(106, result)
        }
    }

    @Test
    fun stringArg() {
        val result = parseAPLExpression(
            """
            |defsyntax foo (:string a) { "bar",a }
            |foo "xcx"
            """.trimMargin())
        assertString("barxcx", result)
    }
}
