package array

import kotlin.test.Test

class StandardLibSimpleFunctionsTest : APLTest() {
    @Test
    fun formatterTest() {
        val src =
            """
            |use("standard-lib.kap")
            |use("output3.kap")
            |o3:format 2 2 ⍴ 1 100 10000 1.2
            """.trimMargin()
        parseAPLExpression(src)
    }
}
