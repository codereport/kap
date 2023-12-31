package array

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListTest : APLTest() {
    @Test
    fun parseList() {
        val result = parseAPLExpression("1;2;3")
        assertListContent(listOf(1, 2, 3), result)
    }

    @Test
    fun parseListWithStatements() {
        val result = parseAPLExpression("1+2;2+3;3 ◊ 5+1+1;6+1+1;7;8")
        assertListContent(listOf(7, 8, 7, 8), result)
    }

    @Test
    fun parseWithExpressions() {
        val result = parseAPLExpression("1+2;3+4")
        assertListContent(listOf(3, 7), result)
    }

    @Test
    fun fromList() {
        parseAPLExpression("fromList (1;2;3)").let { result ->
            assertDimension(dimensionsOfSize(3), result)
            assertArrayContent(arrayOf(1, 2, 3), result)
        }
    }

    @Test
    fun toList() {
        parseAPLExpression("toList 1 2 3").let { result ->
            assertListContent(listOf(1, 2, 3), result)
        }
    }

    private fun assertListContent(expected: List<Any>, list: APLValue) {
        assertTrue(list is APLList, "actual type: ${list::class.simpleName}")
        assertEquals(expected.size, list.listSize())
        expected.forEachIndexed { index, item ->
            assertAPLValue(item, list.listElement(index))
        }
    }
}
