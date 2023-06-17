package array

import kotlin.test.Test

class OutputFormatterTest : APLTest() {
    @Test
    fun outputFormatter0() {
        val src =
            """
            |isArray ⇐ 'array≡typeof
            |isChar ⇐ 'char≡typeof
            |
            |∇ isAplString (v) {
            |  if (1≢≢⍴v) { →0 }
            |  ~0∊'char=typeof¨v
            |}
            |
            |stringToGraphemes ⇐ (1,≢)⍛⍴ unicode:toGraphemes
            |
            |∇ alignCells (v) {
            |  colWidths ← ⌈⌿ (↑↓⍴)¨v
            |  xAligned ← ((⍴v) ⍴ colWidths) {((≢⍵) (⍺-↑↓⍴⍵) ⍴ @\s),⍵}¨ v
            |
            |  rowHeights ← ⌈/ ≢¨xAligned
            |  yAligned ← (⍉ (⌽ ⍴ xAligned) ⍴ rowHeights) {⍵ ,[0] ((⍺-≢⍵) (↑↓⍴⍵)) ⍴ @\s}¨ xAligned
            |}
            |
            |∇ renderArray (v) {
            |  r ← ¯1 (↑¯1↑⍴v) ⍴ v
            |  alignCells (stringToGraphemes⍕)¨ r
            |}
            |
            |∇ prettyPrintToChars (v) {
            |  when {
            |    (isChar(v))      { renderChar v }
            |    (isAplString(v)) { renderString v }
            |    (isArray(v))     { renderArray v }
            |    (1)              { stringToGraphemes ⍕v }
            |  }
            |}
            |
            |prettyPrintToChars 2 3 3 ⍴ ⍳100
            """.trimMargin()
        parseAPLExpression(src, withStandardLib = true).let { result ->
            assertDimension(dimensionsOfSize(6, 3), result)
            fun assertCell1(v: APLValue, i: Int, a: String) = v.valueAt(i).let { v0 ->
                assertDimension(dimensionsOfSize(1, 2), v0)
                assertChar(' '.code, v0.valueAt(0))
                assertString(a, v0.valueAt(1))
            }

            fun assertCell2(v: APLValue, i: Int, a: String, b: String) = v.valueAt(i).let { v0 ->
                assertDimension(dimensionsOfSize(1, 2), v0)
                assertString(a, v0.valueAt(0))
                assertString(b, v0.valueAt(1))
            }
            assertCell1(result, 0, "0")
            assertCell1(result, 1, "1")
            assertCell1(result, 2, "2")
            assertCell1(result, 3, "3")
            assertCell1(result, 4, "4")
            assertCell1(result, 5, "5")
            assertCell1(result, 6, "6")
            assertCell1(result, 7, "7")
            assertCell1(result, 8, "8")
            assertCell1(result, 9, "9")
            assertCell2(result, 10, "1", "0")
            assertCell2(result, 11, "1", "1")
            assertCell2(result, 12, "1", "2")
            assertCell2(result, 13, "1", "3")
            assertCell2(result, 14, "1", "4")
            assertCell2(result, 15, "1", "5")
            assertCell2(result, 16, "1", "6")
            assertCell2(result, 17, "1", "7")
        }
    }

    @Test
    fun outputFormatter1() {
        val src =
            """
            |isArray ⇐ 'array≡typeof
            |isChar ⇐ 'char≡typeof
            |
            |∇ isAplString (v) {
            |  if (1≢≢⍴v) { →0 }
            |  ~0∊'char=typeof¨v
            |}
            |
            |stringToGraphemes ⇐ (1,≢)⍛⍴ unicode:toGraphemes
            |
            |∇ alignCells (v) {
            |  ⍝ The width of each column is the maximum width of any cell
            |  colWidths ← ⌈⌿ (↑↓⍴)¨v
            |  ⍝ Pad each cell to the correct width
            |  xAligned ← ((⍴v) ⍴ colWidths) {((≢⍵) (⍺-↑↓⍴⍵) ⍴ @\s),⍵}¨ v
            |
            |  ⍝ Likewise for the heights
            |  rowHeights ← ⌈/ ≢¨xAligned
            |  yAligned ← (⍉ (⌽ ⍴ xAligned) ⍴ rowHeights) {⍵ ,[0] ((⍺-≢⍵) (↑↓⍴⍵)) ⍴ @\s}¨ xAligned
            |}
            |
            |∇ renderArray (v) {
            |  r ← ¯1 (↑¯1↑⍴v) ⍴ v
            |  alignCells (stringToGraphemes⍕)¨ r
            |}
            |
            |∇ renderString {
            |  stringToGraphemes @" , ⍵ , @"
            |}
            |
            |∇ prettyPrintToChars (v) {
            |  when {
            |    (isChar(v))      { renderChar v }
            |    (isAplString(v)) { renderString v }
            |    (isArray(v))     { renderArray v }
            |    (1)              { stringToGraphemes ⍕v }
            |  }
            |}
            |
            |prettyPrintToChars "foo"
            """.trimMargin()
        parseAPLExpression(src, withStandardLib = true)
    }

    @Test
    fun singleElementArrayShouldNotFail() {
        parseAPLExpression("o3:format ,1", withStandardLib = true)
    }
}
