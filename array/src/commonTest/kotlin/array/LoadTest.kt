package array

import kotlin.test.Test

class LoadTest : APLTest() {
    @Test
    fun loadSimpleSource() {
        val engine = Engine()
        val context = RuntimeContext(engine)
        val res0 = engine.parseAndEval(StringSourceLocation("io:load \"test-data/test-source.kap\""), context = context)
        assertSimpleNumber(10, res0)
        val res1 = engine.parseAndEval(StringSourceLocation("foo:bar 1"), context = context)
        assertSimpleNumber(101, res1)
    }

    @Test
    fun ensureLoadPreservesOldNamespace() {
        val engine = Engine()
        val context = RuntimeContext(engine)
        val res0 =
            engine.parseAndEval(StringSourceLocation("namespace(\"a\") x←1 ◊ io:load \"test-data/test-source.kap\""), context = context)
        assertSimpleNumber(10, res0)
        val res1 = engine.parseAndEval(StringSourceLocation("foo:bar 1"), context = context)
        assertSimpleNumber(101, res1)
        val res2 = engine.parseAndEval(StringSourceLocation("x + 10"), context = context)
        assertSimpleNumber(11, res2)
    }

    @Test
    fun ensureLoadPreservesOldNamespaceOnError() {
        val engine = Engine()
        val context = RuntimeContext(engine)
        try {
            engine.parseAndEval(StringSourceLocation("namespace(\"a\") x←1 ◊ io:load \"test-data/parse-error.kap\""), context = context)
        } catch (e: ParseException) {
            // expected
        }
        val res2 = engine.parseAndEval(StringSourceLocation("x + 10"), context = context)
        assertSimpleNumber(11, res2)
    }
}
