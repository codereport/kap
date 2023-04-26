package array

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ObjectsTest : APLTest() {
    @Test
    fun simpleObjectDefinition() {
        val result = parseAPLExpression(
            """
            |objects:defclass 'foo
            |'foo objects:make 1 2
            """.trimMargin())
        assertTrue(result is TypedAPLValue)
        assert1DArray(arrayOf(1, 2), result.delegate)
    }

    @Test
    fun extractValue() {
        val result = parseAPLExpression(
            """
            |objects:defclass 'foo
            |a ← 'foo objects:make 1 2
            |objects:extract a
            """.trimMargin())
        assert1DArray(arrayOf(1, 2), result)
    }

    @Test
    fun findClassFromInstance() {
        val (result, engine) = parseAPLExpression2(
            """
            |objects:defclass 'foo
            |a ← 'foo objects:make 1 2
            |objects:classof a
            """.trimMargin())
        assertTrue(result is APLSymbol)
        assertSame(engine.internSymbol("foo"), result.value)
    }

    @Test
    fun createInstanceWithIllegalClass() {
        assertFailsWith<KapClassNotFound> {
            parseAPLExpression("'nonexistentClass objects:make 1 2 3 4 5")
        }
    }

    @Test
    fun functionCallUnderExtraction() {
        val result = parseAPLExpression(
            """
            |objects:defclass 'foo
            |a ← 'foo objects:make 1000×1+⍳8
            |(10+)⍢objects:extract a
            """.trimMargin())
        assertTrue(result is TypedAPLValue)
        assert1DArray(arrayOf(1010, 2010, 3010, 4010, 5010, 6010, 7010, 8010), result.delegate)
    }

//    @Test
//    fun genericFunction() {
//        val result = parseAPLExpression(
//            """
//            |∇ bar (x) {
//            |    objects:dispatch x
//            |}
//            |objects:defclass 'foo
//            |objects:defmethod bar (foo) {
//            |    10+⍵
//            |}
//            |a ← objects:make 20
//            |bar a
//            """.trimMargin())
//        assertSimpleNumber(30, result)
//    }
}
