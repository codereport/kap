package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WeakRefTestLinux {
    @Test
    fun simpleWeakRef() {
        val v = makeWeakStringArray()
        kotlin.native.runtime.GC.collect()
        val a = v.value
        assertNull(a)
    }

    private fun makeWeakStringArray(): MPWeakReference<Array<String>> {
        val v = Array(100000) { i -> "Value:${i}" }
        val weakRef = MPWeakReference.make(v)
        val a = weakRef.value
        assertNotNull(a)
        assertEquals("Value:0", a[0])
        return weakRef
    }
}
