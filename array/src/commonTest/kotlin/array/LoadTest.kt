package array

import kotlin.test.Test

class LoadTest : APLTest() {
    @Test
    fun loadSimpleSource() {
        val engine = Engine()
        engine.withThreadLocalAssigned {
            val res0 = engine.parseAndEval(StringSourceLocation("io:load \"test-data/test-source.kap\""), allocateThreadLocals = false)
            assertSimpleNumber(10, res0)
            val res1 = engine.parseAndEval(StringSourceLocation("foo:bar 1"), allocateThreadLocals = false)
            assertSimpleNumber(101, res1)
        }
    }

    @Test
    fun ensureLoadPreservesOldNamespace() {
        val engine = Engine()
        engine.withThreadLocalAssigned {
            val res0 =
                engine.parseAndEval(
                    StringSourceLocation("namespace(\"a\") x←1 ◊ io:load \"test-data/test-source.kap\""),
                    allocateThreadLocals = false)
            assertSimpleNumber(10, res0)
            val res1 = engine.parseAndEval(StringSourceLocation("foo:bar 1"), allocateThreadLocals = false)
            assertSimpleNumber(101, res1)
            val res2 = engine.parseAndEval(StringSourceLocation("x + 10"), allocateThreadLocals = false)
            assertSimpleNumber(11, res2)
        }
    }

    @Test
    fun ensureLoadPreservesOldNamespaceOnError() {
        val engine = Engine()
        engine.withThreadLocalAssigned {
            try {
                engine.parseAndEval(
                    StringSourceLocation("namespace(\"a\") x←1 ◊ io:load \"test-data/parse-error.kap\""),
                    allocateThreadLocals = false)
            } catch (e: ParseException) {
                // expected
            }
            val res2 = engine.parseAndEval(StringSourceLocation("x + 10"), allocateThreadLocals = false)
            assertSimpleNumber(11, res2)
        }
    }
}
