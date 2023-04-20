package array

import kotlin.test.Test

class SystemParamsLinuxTest : APLTest() {
    @Test
    fun platformParam() {
        val (result, engine) = parseAPLExpression2("sysparam 'kap:platform")
        assertSymbolNameCoreNamespace(engine, "linux", result)
    }
}
