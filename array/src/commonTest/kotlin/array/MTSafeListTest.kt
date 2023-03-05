package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MTSafeListTest {
    @Test
    fun simpleInsertion() {
        val list = MTSafeArrayList<String>()
        assertContent(arrayOf(), list)
        list.add("foo")
        assertContent(arrayOf("foo"), list)
        list.add("blah")
        assertContent(arrayOf("foo", "blah"), list)
    }

    @Test
    fun simpleRemove() {
        val list = MTSafeArrayList<String>()
        list.add("foo")
        list.add("bar")
        val res = list.removeAt(0)
        assertEquals(res, "foo")
        assertContent(arrayOf("bar"), list)
    }

    @Test
    fun testRemoveLast() {
        val list = MTSafeArrayList<String>()
        list.addAll(listOf("foo", "bar", "test", "xyz", "qwert"))
        assertContent(arrayOf("foo", "bar", "test", "xyz", "qwert"), list)
        val res = list.removeLast()
        assertEquals("qwert", res)
        assertContent(arrayOf("foo", "bar", "test", "xyz"), list)
    }

    @Test
    fun testAddAllMiddle() {
        val list = MTSafeArrayList<String>()
        list.addAll(listOf("foo", "bar", "test", "xyz", "qwert"))
        list.addAll(2, listOf("a", "b"))
        assertContent(arrayOf("foo", "bar", "a", "b", "test", "xyz", "qwert"), list)
    }

    @Test
    fun testRemoveAll() {
        val list = MTSafeArrayList<String>()
        list.addAll(listOf("foo", "bar", "test", "xyz", "qwert"))
        list.removeAll(listOf("bar", "test", "qwert"))
        assertContent(arrayOf("foo", "xyz"), list)
    }

    @Test
    fun testRemoveAllMultipleInstance() {
        val list = MTSafeArrayList<String>()
        list.addAll(listOf("foo", "bar", "test", "xyz", "qwert", "test"))
        list.removeAll(listOf("bar", "test", "qwert"))
        assertContent(arrayOf("foo", "xyz"), list)
    }

    @Test
    fun iteratorWithConcurrentModification() {
        val list = MTSafeArrayList<String>()
        list.addAll(listOf("foo", "bar", "test", "xyz", "qwert", "test"))
        val iterator = list.iterator()
        assertTrue(iterator.hasNext())
        assertEquals("foo", iterator.next())
        assertTrue(iterator.hasNext())
        assertEquals("bar", iterator.next())
        assertTrue(iterator.hasNext())
        assertEquals("test", iterator.next())
        list.removeAt(4)
        assertContent(arrayOf("foo", "bar", "test", "xyz", "test"), list)
        assertTrue(iterator.hasNext())
        assertEquals("xyz", iterator.next())
        assertTrue(iterator.hasNext())
        assertEquals("qwert", iterator.next())
        assertTrue(iterator.hasNext())
        assertEquals("test", iterator.next())
        assertFalse(iterator.hasNext())
    }

    @Test
    fun removeNonExistentElement() {
        val list = MTSafeArrayList<String>()
        list.addAll(listOf("foo", "bar", "test", "xyz", "qwert"))
        assertContent(arrayOf("foo", "bar", "test", "xyz", "qwert"), list)
        assertFalse(list.remove("zxcv"))
        assertContent(arrayOf("foo", "bar", "test", "xyz", "qwert"), list)
    }

    @Test
    fun removeOneElement() {
        val list = MTSafeArrayList<String>()
        list.addAll(listOf("foo", "bar", "test", "xyz", "qwert", "bar"))
        assertTrue(list.remove("bar"))
        assertContent(arrayOf("foo", "test", "xyz", "qwert", "bar"), list)
    }

    @Test
    fun removeMultiInstances() {
        val list = MTSafeArrayList<String>()
        list.addAll(listOf("foo", "bar", "test", "xyz", "qwert", "bar"))
        assertTrue(list.removeAll(listOf("bar", "qwert", "test")))
        assertContent(arrayOf("foo", "xyz"), list)
    }

    @Test
    fun testClear() {
        val list = MTSafeArrayList<String>()
        list.addAll(listOf("foo", "bar", "test", "xyz", "qwert", "bar"))
        list.clear()
        assertContent(arrayOf(), list)
    }

    private fun <T> assertContent(expected: Array<T>, list: MTSafeArrayList<T>) {
        assertEquals(expected.size, list.size, "expected.size=${expected.size}, list.size=${list.size}")
        repeat(list.size) { i ->
            assertEquals(expected[i], list[i], "element at index ${i} does not match")
        }
    }
}