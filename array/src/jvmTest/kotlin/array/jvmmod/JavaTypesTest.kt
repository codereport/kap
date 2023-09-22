package array.jvmmod

import array.*
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TypesExample() {
    fun createString() = "foostring"
    fun createLong() = 1L
}

class SimpleExample() {
    override fun toString() = "foo"
}

class JavaTypesTest : APLTest() {
    @Test
    fun convertToPrimitiveType() {
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

    @Test
    fun convertToBooleanTest() {
        val src =
            """
            |(jvm:toJvmBoolean 0) (jvm:toJvmBoolean 1) (jvm:toJvmBoolean 10) (jvm:toJvmBoolean 100 200 150)
            """.trimMargin()
        parseAPLExpression(src).let { result ->
            assertDimension(dimensionsOfSize(4), result)
            verifyJvmValue(false, result.valueAt(0))
            verifyJvmValue(true, result.valueAt(1))
            verifyJvmValue(true, result.valueAt(2))
            verifyJvmValue(true, result.valueAt(3))
        }
    }

    @Test
    fun convertToKapTestString() {
        val src =
            """
            |typesExampleClass ← jvm:findClass "array.jvmmod.TypesExample"
            |constructor ← typesExampleClass jvm:findConstructor toList ⍬
            |a ← constructor jvm:createInstance toList ⍬
            |createStringMethod ← typesExampleClass jvm:findMethod "createString"
            |jvm:fromJvm createStringMethod jvm:callMethod a
            """.trimMargin()
        parseAPLExpression(src).let { result ->
            assertString("foostring", result)
        }
    }

    @Test
    fun convertToKapTestLong() {
        val src =
            """
            |typesExampleClass ← jvm:findClass "array.jvmmod.TypesExample"
            |constructor ← typesExampleClass jvm:findConstructor toList ⍬
            |a ← constructor jvm:createInstance toList ⍬
            |createLongMethod ← typesExampleClass jvm:findMethod "createLong"
            |jvm:fromJvm createLongMethod jvm:callMethod a
            """.trimMargin()
        parseAPLExpression(src).let { result ->
            assertSimpleNumber(1, result)
        }
    }

    @Test
    fun missingMethodName() {
        val src =
            """
            |typesExampleClass ← jvm:findClass "array.jvmmod.TypesExample"
            |constructor ← typesExampleClass jvm:findConstructor toList ⍬
            |a ← constructor jvm:createInstance toList ⍬
            |createLongMethod ← typesExampleClass jvm:findMethod "createLong"
            |jvm:fromJvm createLongMethod jvm:callMethod toList ⍬ 
            """.trimMargin()
        assertFailsWith<ListOutOfBounds> {
            parseAPLExpression(src)
        }
    }

    @Test
    fun toStringCalledWhenPrinting() {
        val src =
            """
            |cl ← jvm:findClass "array.jvmmod.SimpleExample"
            |constructor ← cl jvm:findConstructor toList ⍬
            |constructor jvm:createInstance toList ⍬
            """.trimMargin()
        parseAPLExpression(src).let { result ->
            assertEquals("foo", result.formatted(FormatStyle.PLAIN))
        }
    }

    private fun verifyJvmValue(expected: Any, v: APLValue) {
        assertTrue(v is JvmInstanceValue)
        val instance = v.instance
        assertEquals(expected, instance)
    }
}
