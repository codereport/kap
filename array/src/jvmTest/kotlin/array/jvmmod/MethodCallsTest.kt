package array.jvmmod

import array.APLTest
import array.APLValue
import array.dimensionsOfSize
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

    @Test
    fun convertToPrimitiveType() {
        fun verifyJvmValue(expected: Any, v: APLValue) {
            assertTrue(v is JvmInstanceValue)
            val instance = v.instance
            assertEquals(expected, instance)
        }

        val src =
            """
            |(jvm:toJvmFloat 50.0) (jvm:toJvmDouble 60.0) (jvm:toJvmShort 10) (jvm:toJvmInt 20) (jvm:toJvmLong 30) (jvm:toJvmByte 40) (jvm:toJvmChar @a-@\u0)
            """.trimMargin()
        parseAPLExpression(src).let { result ->
            assertDimension(dimensionsOfSize(7), result)
            verifyJvmValue(50.0.toFloat(), result.valueAt(0))
            verifyJvmValue(60.0, result.valueAt(1))
            verifyJvmValue(10.toShort(), result.valueAt(2))
            verifyJvmValue(20, result.valueAt(3))
            verifyJvmValue(30.toLong(), result.valueAt(4))
            verifyJvmValue(40.toByte(), result.valueAt(5))
            verifyJvmValue('a', result.valueAt(6))
        }
    }

}
