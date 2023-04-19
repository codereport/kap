package array.jvmmod

import array.APLTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class Foo {
    @Suppress("unused")
    fun standardTest(): String {
        return "some result"
    }

    companion object {
        @Suppress("unused")
        @JvmStatic
        fun staticTest(arg: String): String {
            val result = "message from method: ${arg}"
            return result
        }
    }
}

class MethodCallsTest : APLTest() {
    @Test
    fun findClass() {
        parseAPLExpression("jvm:findClass \"java.lang.String\"").let { result ->
            assertTrue(result is JvmInstanceValue)
            assertSame(String::class.java, result.instance)
        }
    }

    @Test
    fun findMethod() {
        parseAPLExpression("(jvm:findClass \"array.jvmmod.Foo\") jvm:findMethod (\"staticTest\" ; jvm:findClass \"java.lang.String\")").let { result ->
            assertTrue(result is JvmInstanceValue)
            val expected = Foo::class.java.getMethod("staticTest", String::class.java)
            assertEquals(expected, result.instance)
        }
    }

    @Test
    fun callMethod() {
        val result = parseAPLExpression(
            """                        
            |fooClass ← jvm:findClass "array.jvmmod.Foo"
            |constructor ← fooClass jvm:findConstructor toList ⍬ 
            |method ← fooClass jvm:findMethod "standardTest"
            |a ← constructor jvm:createInstance toList ⍬
            |method jvm:callMethod a
            """.trimMargin())
        assertTrue(result is JvmInstanceValue)
        val instance = result.instance
        assertTrue(instance is String)
        assertEquals("some result", instance)
    }

    @Test
    fun callStatic() {
        val result = parseAPLExpression(
            """
            |fooClass ← jvm:findClass "array.jvmmod.Foo"
            |method ← fooClass jvm:findMethod ("staticTest" ; jvm:findClass "java.lang.String")
            |method jvm:callMethod (:null ; "qwe")
            """.trimMargin())
        assertTrue(result is JvmInstanceValue)
        val instance = result.instance
        assertTrue(instance is String)
        assertEquals("message from method: qwe", instance)
    }
}
