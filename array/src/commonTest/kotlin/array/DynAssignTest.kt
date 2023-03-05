package array

import kotlin.test.Test
import kotlin.test.assertEquals

class DynAssignTest : APLTest() {
    @Test
    fun simpleDynamicAssign() {
        val src =
            """
            |a ← 1
            |b dynamicequal a+1
            |io:print b
            """.trimMargin()
        parseAPLExpressionWithOutput(src).let { (result, out) ->
            assertSimpleNumber(2, result)
            assertEquals("2", out)
        }
    }

    @Test
    fun simpleDynamicAssignWithUpdate() {
        val src =
            """
            |a ← 1
            |b dynamicequal a+1
            |io:print b
            |a ← 3
            |io:print b
            """.trimMargin()
        parseAPLExpressionWithOutput(src).let { (result, out) ->
            assertSimpleNumber(4, result)
            assertEquals("24", out)
        }
    }

    @Test
    fun simpleDynamicAssignWithMultipleUpdate() {
        val src =
            """
            |a ← 1
            |b dynamicequal a+1
            |io:print b
            |a ← 3
            |io:print b
            |a ← 5
            |io:print b
            """.trimMargin()
        parseAPLExpressionWithOutput(src).let { (result, out) ->
            assertSimpleNumber(6, result)
            assertEquals("246", out)
        }
    }

    @Test
    fun simpleDynamicAssignWithInternalVar() {
        val src =
            """
            |a ← 1
            |b dynamicequal a + (c × c←2)
            |io:print b
            """.trimMargin()
        parseAPLExpressionWithOutput(src).let { (result, out) ->
            assertSimpleNumber(5, result)
            assertEquals("5", out)
        }
    }

    @Test
    fun simpleDynamicAssignMultipleVariables() {
        val src =
            """
            |a ← 1
            |b ← 2
            |c dynamicequal a+b+1
            |io:print c
            """.trimMargin()
        parseAPLExpressionWithOutput(src).let { (result, out) ->
            assertSimpleNumber(4, result)
            assertEquals("4", out)
        }
    }

    @Test
    fun simpleDynamicAssignMultipleVariablesWithUpdates() {
        val src =
            """
            |a ← 1
            |b ← 2
            |c dynamicequal a+b+1
            |a ← 5
            |b ← 6
            |io:print c
            """.trimMargin()
        parseAPLExpressionWithOutput(src).let { (result, out) ->
            assertSimpleNumber(12, result)
            assertEquals("12", out)
        }
    }

    @Test
    fun dynamicAssignChange() {
        val src =
            """
            |a ← 1
            |b ← 2
            |c dynamicequal a+b+1
            |io:print c
            |a ← 3
            |b ← 6
            |io:print c
            |c dynamicequal a+b+100
            |io:print c
            |a ← 10
            |b ← 9
            |io:print c
            """.trimMargin()
        parseAPLExpressionWithOutput(src).let { (result, out) ->
            assertSimpleNumber(119, result)
            assertEquals("410109119", out)
        }
    }

    @Test
    fun dynamicAssignWithSideEffects() {
        val src =
            """
            |a ← 1
            |b dynamicequal 20+io:print a
            |io:print b
            |a ← 3
            |io:print b
            |io:print b
            """.trimMargin()
        parseAPLExpressionWithOutput(src).let { (result, out) ->
            assertSimpleNumber(23, result)
            assertEquals("12132323", out)
        }
    }

    @Test
    fun dynamicAssignMultiAssignWithSideEffects() {
        val src =
            """
            |a ← 1
            |b dynamicequal 20+io:print a
            |io:print b
            |io:print b
            |a ← 2
            |io:print b
            |io:print b
            |b dynamicequal 30+io:print 100+a
            |io:print b
            |a ← 3
            |io:print b
            """.trimMargin()
        parseAPLExpressionWithOutput(src).let { (result, out) ->
            assertSimpleNumber(133, result)
            assertEquals("1212122222102132103133", out)
        }
    }

    @Test
    fun readResultFromFn() {
        val src =
            """
            |∇ foo {
            |  q ← 10+⍵
            |  1+q
            |}
            |a ← 1
            |b dynamicequal a+20
            |io:print foo b
            |a ← 30
            |io:print foo b
            """.trimMargin()
        parseAPLExpressionWithOutput(src).let { (result, out) ->
            assertSimpleNumber(51, result)
            assertEquals("3251", out)
        }
    }
}
