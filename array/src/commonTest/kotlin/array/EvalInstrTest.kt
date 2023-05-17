package array

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertFails

class EvalInstrTest : APLTest() {
    @Test
    fun plainEvalSameContext() {
        val engine = Engine()
        engine.parseAndEval(StringSourceLocation("foo ← 1")).let { result ->
            assertSimpleNumber(1, result)
        }
        engine.parseAndEval(StringSourceLocation("foo + 3")).let { result ->
            assertSimpleNumber(4, result)
        }
    }

    // Support for extraBindings was removed in the rewrite of the stack management
    @Test
    @Ignore
    fun evalWithExtraBindings() {
        val engine = Engine()
        val b = mapOf(engine.internSymbol("a") to APLLong(2))
        engine.parseAndEval(StringSourceLocation("a + 1"), extraBindings = b).let { result ->
            assertSimpleNumber(3, result)
        }
    }

    @Test
    fun evalWithExtraBindingsRequiresNewContext() {
        val engine = Engine()
        val b = mapOf(engine.internSymbol("a") to APLLong(3))
        assertFails {
            engine.parseAndEval(StringSourceLocation("a + 7"), extraBindings = b)
        }
    }
}
