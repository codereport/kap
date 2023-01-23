package array

import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFailsWith

class EvalInstrTest : APLTest() {
    @Test
    fun plainEvalSameContext() {
        val engine = Engine()
        engine.withThreadLocalAssigned {
            engine.parseAndEval(StringSourceLocation("foo ← 1"), allocateThreadLocals = false).let { result ->
                assertSimpleNumber(1, result)
            }
            engine.parseAndEval(StringSourceLocation("foo + 3"), allocateThreadLocals = false).let { result ->
                assertSimpleNumber(4, result)
            }
        }
    }

    @Test
    fun plainEvalNewContext() {
        val engine = Engine()
        engine.parseAndEval(StringSourceLocation("foo ← 1")).let { result ->
            assertSimpleNumber(1, result)
        }
        assertFailsWith<VariableNotAssigned> {
            engine.parseAndEval(StringSourceLocation("foo + 3"))
        }
    }

    @Test
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
