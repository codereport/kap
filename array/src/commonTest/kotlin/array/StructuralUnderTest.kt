package array

import kotlin.test.Test

class StructuralUnderTest : APLTest() {
    @Test
    fun scalarFunctionUnderTake() {
        parseAPLExpression("((1000+)under(1↑)) 3 4 ⍴ ⍳12").let { result ->
            assertDimension(dimensionsOfSize(3, 4), result)
            assertArrayContent(arrayOf(1000, 1001, 1002, 1003, 4, 5, 6, 7, 8, 9, 10, 11), result)
        }
    }
}
