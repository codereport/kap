package array

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatNumbersTest {
    @Test
    fun formatSimpleDouble() {
        assertEquals("1.5", 1.5.formatDouble())
        assertEquals("1.0", 1.0.formatDouble())
        assertEquals("0.0", 0.0.formatDouble())
        assertEquals("100000.0", 100000.0.formatDouble())
        assertEquals("-1.0", (-1.0).formatDouble())
        assertEquals("-12345.0", (-12345.0).formatDouble())
    }
}