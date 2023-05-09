package array

import kotlin.test.Ignore
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

    @Ignore
    @Test
    fun includeSeparateParseCalls() {
        val engine = Engine()
        engine.parseAndEval(StringSourceLocation("use(\"test-data/include-test/include-test0.kap\")")).let { result ->
            assertSimpleNumber(1, result)
        }
        engine.parseAndEval(StringSourceLocation("a")).let { result ->
            assertSimpleNumber(1, result)
        }
    }
}
