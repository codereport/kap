package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class MPAtomicRefArrayTest {
    @Test
    fun checkOrUpdateTest() {
        var index = 0

        class CachedObj(val a: Int) {
            init {
                index++
            }
        }

        val a = makeAtomicRefArray<CachedObj>(10)
        val b0 = a.checkOrUpdate(0) { CachedObj(0) }
        assertEquals(1, index)
        assertEquals(0, b0.a)
        val b1 = a.checkOrUpdate(1) { CachedObj(1) }
        assertEquals(2, index)
        assertEquals(1, b1.a)

        val b2 = a.checkOrUpdate(0) { CachedObj(2) }
        assertEquals(2, index)
        assertEquals(0, b2.a)
        assertEquals(2, index)
        assertEquals(0, b2.a)
        assertSame(b0, b2)
    }
}
