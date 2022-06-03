package array

import kotlin.test.*

class ScopeTest : APLTest() {
    @Test
    fun includeGlobal() {
        val src = """
            |use("test-data/include-test/include-test0.kap")
            |a
        """.trimMargin()
        assertSimpleNumber(1, parseAPLExpression(src, newContext = false))
    }


    @Test
    fun includeGlobalSecondInclude() {
        val src = """
            |use("test-data/include-test/include-test1.kap")
            |a0 a1
        """.trimMargin()
        parseAPLExpression(src, newContext = false).let { result ->
            assert1DArray(arrayOf(100, 101), result)
        }
    }

    @Ignore
    @Test
    fun includeSeparateParseCalls() {
        val engine = Engine()
        engine.parseAndEval(StringSourceLocation("use(\"test-data/include-test/include-test0.kap\")"), newContext = false).let { result ->
            assertSimpleNumber(1, result)
        }
        engine.parseAndEval(StringSourceLocation("a")).let { result ->
            assertSimpleNumber(1, result)
        }
    }
}
