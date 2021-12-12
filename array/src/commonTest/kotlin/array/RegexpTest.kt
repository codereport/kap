package array

import array.builtins.TagCatch
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class RegexpTest : APLTest() {
    @Test
    fun plainRegexpMatches() {
        val result = parseAPLExpression(
            """
            |"abc" regex:match "qweabcasd"
            """.trimMargin())
        assertSimpleNumber(1, result)
    }

    @Test
    fun plainRegexpNoMatch() {
        val result = parseAPLExpression(
            """
            |"abc" regex:match "xyztestcxz"
            """.trimMargin())
        assertSimpleNumber(0, result)
    }

    @Test
    fun testFullStringMatch() {
        val result = parseAPLExpression(
            """
            |"^xyz*w$" regex:match "xyw"
            """.trimMargin())
        assertSimpleNumber(1, result)
    }

    @Test
    fun plainRegexSyntaxError() {
        assertFailsWith<InvalidRegexp> {
            parseAPLExpression("\"a[z\" regex:match \"foo\"")
        }
        assertFailsWith<InvalidRegexp> {
            parseAPLExpression("\"a(z\" regex:match \"foo\"")
        }
    }

    @Test
    fun plainWithMatcher0() {
        val result = parseAPLExpression(
            """
            |(regex:create "abc") regex:match "qweabcasd"
            """.trimMargin())
        assertSimpleNumber(1, result)
    }

    @Test
    fun plainWithMatcher1() {
        val result = parseAPLExpression(
            """
            |(regex:create "abc") regex:match "xyztestcxz"
            """.trimMargin())
        assertSimpleNumber(0, result)
    }

    @Test
    fun regexpFind0() {
        val result = parseAPLExpression(
            """
            |"^zx:([a-z]+):x${'$'}" regex:find "zx:test:x"
            """.trimMargin())
        assertDimension(dimensionsOfSize(2), result)
        assertString("zx:test:x", result.valueAt(0))
        assertString("test", result.valueAt(1))
    }

    @Test
    fun regexpFind1() {
        val (result, engine) = parseAPLExpression2(
            """
            |"^zx:(foo)?:x${'$'}" regex:find "zx::x"
            """.trimMargin())
        assertDimension(dimensionsOfSize(2), result)
        assertString("zx::x", result.valueAt(0))
        assertSame(engine.keywordNamespace.internSymbol("undefined"), result.valueAt(1).ensureSymbol().value)
    }

    @Test
    fun regexpTestMultiline() {
        val result = parseAPLExpression(
            """
            |(:multiLine regex:create "^foo") regex:match "a
            |fooa
            |a
            |a"
            """.trimMargin())
        assertSimpleNumber(1, result)
    }

    @Test
    fun regexpTestCase() {
        val result0 = parseAPLExpression(
            """
            |(regex:create "^foo$") regex:match "foO"
            """.trimMargin())
        assertSimpleNumber(0, result0)

        val result1 = parseAPLExpression(
            """
            |(:ignoreCase regex:create "^foo$") regex:match "foO"
            """.trimMargin())
        assertSimpleNumber(1, result1)
    }

    @Test
    fun regexpWithIllegalFlags0() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression(
                """
                |:foo regex:create "foo"
                """.trimMargin())
        }
    }

    @Test
    fun regexpWithIllegalFlags1() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression(
                """
                |:foo :bar regex:create "foo"
                """.trimMargin())
        }
    }

    @Test
    fun regexpWithIllegalFlags2() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression(
                """
                |1 regex:create "foo"
                """.trimMargin())
        }
    }

    @Test
    fun regexpWithIllegalFlags3() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression(
                """
                |(2 1 ⍴ :multiLine :ignoreCase) regex:create "foo"
                """.trimMargin())
        }
    }

//    @Test
//    fun regexpTestWithIndex0() {
//        val result = parseAPLExpression(
//            """
//            |"a([a-z]+)9" regex:index "xbzafoo9qwatest921"
//            """.trimMargin())
//        assertDimension(dimensionsOfSize(2), result)
//        assertArrayContent(arrayOf(3, 8), result.valueAt(0))
//        assertArrayContent(arrayOf(4, 7), result.valueAt(1))
//    }
//
//    @Test
//    fun regexpTestWithIndex1() {
//        val result = parseAPLExpression(
//            """
//            |"a([a-z]+)?9" regex:index "xbza9qwatest921"
//            """.trimMargin())
//        assertDimension(dimensionsOfSize(2), result)
//        assertArrayContent(arrayOf(3, 5), result.valueAt(0))
//        assertAPLNull(result.valueAt(1))
//    }
//
//    @Test
//    fun regexpWithIndex2() {
//        val result = parseAPLExpression(
//            """
//            |"a([a-z]+)?9" regex:index "xbza8qwatest821"
//            """.trimMargin())
//        assertAPLNull(result)
//    }

    @Test
    fun regexpSplit0() {
        val result = parseAPLExpression(
            """
            |"," regex:split "foo,bar,,test,cba" 
            """.trimMargin())
        assertDimension(dimensionsOfSize(5), result)
        assertString("foo", result.valueAt(0))
        assertString("bar", result.valueAt(1))
        assertString("", result.valueAt(2))
        assertString("test", result.valueAt(3))
        assertString("cba", result.valueAt(4))
    }

    @Test
    fun regexpSplit1() {
        val result = parseAPLExpression(
            """
            |",+" regex:split "foo,bar,,test,cba,,,," 
            """.trimMargin())
        assertDimension(dimensionsOfSize(5), result)
        assertString("foo", result.valueAt(0))
        assertString("bar", result.valueAt(1))
        assertString("test", result.valueAt(2))
        assertString("cba", result.valueAt(3))
        assertString("", result.valueAt(4))
    }

    @Test
    fun regexpSplit2() {
        val result = parseAPLExpression(
            """
            |",+" regex:split ",,,foo" 
            """.trimMargin())
        assertDimension(dimensionsOfSize(2), result)
        assertString("", result.valueAt(0))
        assertString("foo", result.valueAt(1))
    }

    @Test
    fun regexpSplit3() {
        val result = parseAPLExpression(
            """
            |",+" regex:split "foo" 
            """.trimMargin())
        assertDimension(dimensionsOfSize(1), result)
        assertString("foo", result.valueAt(0))
    }

    @Test
    fun regexpSplit4() {
        val result = parseAPLExpression(
            """
            |",+" regex:split "" 
            """.trimMargin())
        assertDimension(dimensionsOfSize(1), result)
        assertString("", result.valueAt(0))
    }

    @Test
    fun regexpSplit5() {
        val result = parseAPLExpression(
            """
            |"," regex:split ",a" 
            """.trimMargin())
        assertDimension(dimensionsOfSize(2), result)
        assertString("", result.valueAt(0))
        assertString("a", result.valueAt(1))
    }

    @Test
    fun regexpSplit6() {
        val result = parseAPLExpression(
            """
            |"," regex:split "foo,,,app" 
            """.trimMargin())
        assertDimension(dimensionsOfSize(4), result)
        assertString("foo", result.valueAt(0))
        assertString("", result.valueAt(1))
        assertString("", result.valueAt(2))
        assertString("app", result.valueAt(3))
    }

    @Test
    fun regexpSplitErrorWithScalar() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression(
                """
                |" " regex:split 10
                """.trimMargin())
        }
    }

    @Test
    fun regexpSplitErrorWithWrongArrayMemberType() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression(
                """
                |" " regex:split 10 20
                """.trimMargin())
        }
    }

    @Test
    fun regexpSplitErrorWithWrongDimension() {
        assertFailsWith<APLEvalException> {
            parseAPLExpression(
                """
                |" " regex:split 3 4 ⍴ 10 20
                """.trimMargin())
        }
    }

    @Test
    fun finderrorWithMatches() {
        parseAPLExpression("\"x(f[0-9]+)y\" regex:finderror \"fooxf12345ybar\"", withStandardLib = true).let { result ->
            assertDimension(dimensionsOfSize(2), result)
            assertString("xf12345y", result.valueAt(0))
            assertString("f12345", result.valueAt(1))
        }
    }

    @Test
    fun funderrorNotMatch() {
        assertFailsWith<TagCatch> {
            parseAPLExpression("\"x(f[0-9]+)y\" regex:finderror \"fooxf12345qwer\"", withStandardLib = true)
        }
    }
}
