package array

import kotlin.test.Test

class SystemParamsJsTest : APLTest() {
    @Test
    fun platformParam() {
        val (result, engine) = parseAPLExpression2("sysparam 'kap:platform")
        assertSymbolNameCoreNamespace(engine, "js", result)
    }
}
