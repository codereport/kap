package array

import array.builtins.TagCatch
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ExceptionsTest : APLTest() {
    @Test
    fun simpleException() {
        parseAPLExpression("{1→'foo}catch 1 2 ⍴ 'foo λ{2+⍺}").let { result ->
            assertSimpleNumber(3, result)
        }
    }

    @Test
    fun exceptionHandlerTagCheck() {
        parseAPLExpression("{1→'foo}catch 1 2 ⍴ 'foo λ{⍵}").let { result ->
            assertSymbol("foo", result)
        }
    }

    @Test
    fun multipleTagHandlers() {
        parseAPLExpression("{1→'foo}catch 4 2 ⍴ 'xyz λ{2+⍺} 'test123 λ{3+⍺} 'bar λ{4+⍺} 'foo λ{5+⍺}").let { result ->
            assertSimpleNumber(6, result)
        }
    }

    @Test
    fun unmatchedTag() {
        assertFailsWith<TagCatch> {
            parseAPLExpression("{1→'foo}catch 1 2 ⍴ 'bar λ{2+⍺}")
        }
    }

    @Test
    fun throwWithoutTagHandler() {
        assertFailsWith<TagCatch> {
            parseAPLExpression("2 + 1→'foo")
        }
    }
}
