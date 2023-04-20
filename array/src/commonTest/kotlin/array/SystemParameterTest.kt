package array

import array.builtins.SystemParameterNotFound
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SystemParameterTest : APLTest() {
    @Test
    fun nonexistentParameter() {
        assertFailsWith<SystemParameterNotFound> {
            parseAPLExpression("sysparam 'qwe")
        }
    }
}
