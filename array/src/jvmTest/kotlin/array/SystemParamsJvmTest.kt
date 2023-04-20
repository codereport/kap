package array

import org.junit.Test

class SystemParamsJvmTest : APLTest() {
    @Test
    fun platformParam() {
        val (result, engine) = parseAPLExpression2("sysparam 'kap:platform")
        assertSymbolNameCoreNamespace(engine, "jvm", result)
    }
}
