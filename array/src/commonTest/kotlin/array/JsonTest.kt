package array

import array.json.backendSupportsJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonTest : APLTest() {
    @Test
    fun readJson() {
        unless(backendSupportsJson) { return }

        parseAPLExpression("json:read \"test-data/json-test.json\"").let { result ->
            assertTrue(result is APLMap)
            assertEquals(7, result.elementCount())
            assertAPLValue(InnerDoubleOrLong(1.0), result.lookupValue(APLString("foo")))
            result.lookupValue(APLString("someArray")).let { inner ->
                assertDimension(dimensionsOfSize(5), inner)
                assertAPLValue(InnerDoubleOrLong(1.0), inner.valueAt(0))
                assertAPLValue(InnerDoubleOrLong(2.0), inner.valueAt(1))
                assertAPLValue(InnerDoubleOrLong(3.0), inner.valueAt(2))
                assertAPLValue(InnerDoubleOrLong(4.0), inner.valueAt(3))
                inner.valueAt(4).let { internalList ->
                    assertDimension(dimensionsOfSize(2), internalList)
                    assertArrayContent(arrayOf(InnerDoubleOrLong(5.0), InnerDoubleOrLong(6.0)), internalList)
                }
            }
            assertString("foo test", result.lookupValue(APLString("someString")))
            assertSimpleNumber(1, result.lookupValue(APLString("booleanValue")))
            assertSimpleNumber(0, result.lookupValue(APLString("booleanValue2")))
            result.lookupValue(APLString("recursiveMap")).let { inner ->
                assertTrue(inner is APLMap)
                assertAPLValue(InnerDoubleOrLong(1.0), inner.lookupValue(APLString("a")))
                assertAPLValue(InnerDoubleOrLong(2.0), inner.lookupValue(APLString("b")))
            }
            assertAPLNull(result.lookupValue(APLString("nullValue0")))
        }
    }

    @Test
    fun readJsonFromString() {
        unless(backendSupportsJson) { return }

        parseAPLExpression("json:readString \"{\\\"a\\\":10,\\\"b\\\":\\\"c\\\"}\"").let { result ->
            assertTrue(result is APLMap)
            assertEquals(2, result.elementCount())
            assertAPLValue(InnerDoubleOrLong(10.0), result.lookupValue(APLString("a")))
            assertString("c", result.lookupValue(APLString("b")))
        }
    }
}
