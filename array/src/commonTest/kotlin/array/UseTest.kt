package array

import kotlin.test.Test

class UseTest : APLTest() {
    @Test
    fun includeGlobal() {
        val src = """
            |use("test-data/include-test/include-test0.kap")
            |a
        """.trimMargin()
        assertSimpleNumber(1, parseAPLExpression(src))
    }


    @Test
    fun includeGlobalSecondInclude() {
        val src = """
            |use("test-data/include-test/include-test1.kap")
            |a0 a1
        """.trimMargin()
        parseAPLExpression(src).let { result ->
            assert1DArray(arrayOf(100, 101), result)
        }
    }

    @Test
    fun includeSeparateParseCalls0() {
        val engine = Engine()
        engine.parseAndEval(StringSourceLocation("use(\"test-data/include-test/include-test0.kap\")")).let { result ->
            assertSimpleNumber(1, result)
        }
        engine.parseAndEval(StringSourceLocation("a")).let { result ->
            assertSimpleNumber(1, result)
        }
    }

    @Test
    fun includeSeparateParseCalls1() {
        val engine = Engine()
        engine.parseAndEval(StringSourceLocation("use(\"test-data/include-test/include-test3.kap\")")).let { result ->
            assertSimpleNumber(1, result)
        }
        engine.parseAndEval(StringSourceLocation("foo 1")).let { result ->
            assertSimpleNumber(3, result)
        }
    }
}
